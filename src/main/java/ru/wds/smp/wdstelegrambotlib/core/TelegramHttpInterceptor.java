package ru.wds.smp.wdstelegrambotlib.core;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Перехватчик HTTP-запросов (OkHttp Interceptor) для обеспечения отказоустойчивости.
 *
 * <p>Выполняет две основные задачи:</p>
 * <ul>
 *   <li><b>Rate Limiting (ограничение скорости)</b>: с помощью алгоритма Token Bucket
 *       ограничивает количество исходящих запросов к API Telegram (по умолчанию 30 запросов в секунду),
 *       чтобы минимизировать вероятность получения ошибок 429.</li>
 *   <li><b>Автоматический повтор при ошибке 429 (Too Many Requests)</b>: если Telegram всё же вернул
 *       код 429, перехватчик считывает параметр {@code retry_after} из тела ответа, усыпляет текущий
 *       поток на указанное время (плюс буфер безопасности) и повторяет запрос до достижения лимита попыток.</li>
 *   </ul>
 */
@Slf4j
public class TelegramHttpInterceptor implements Interceptor {

    private final int maxRetries;
    private final long bufferMs;
    private final TokenBucketRateLimiter rateLimiter;
    private static final Pattern RETRY_AFTER_PATTERN = Pattern.compile("\"retry_after\"\\s*:\\s*(\\d+)");

    /**
     * @param maxRetries максимальное число попыток повтора при ошибке 429
     * @param bufferMs   дополнительное время ожидания (буфер) к retry_after в миллисекундах
     * @param rateLimit  лимит запросов в секунду (если <= 0, лимитирование отключено)
     */
    public TelegramHttpInterceptor(int maxRetries, long bufferMs, int rateLimit) {
        this.maxRetries = maxRetries;
        this.bufferMs = bufferMs;
        this.rateLimiter = rateLimit > 0 ? new TokenBucketRateLimiter(rateLimit) : null;
        log.info("Telegram HTTP Interceptor инициализирован: rateLimit={} rps, maxRetries={}, bufferMs={} ms",
                rateLimit > 0 ? rateLimit : "DISABLED", maxRetries, bufferMs);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        // 1. Применяем ограничение скорости (Rate Limiting) перед отправкой запроса
        if (rateLimiter != null) {
            rateLimiter.acquire();
        }

        Response response = chain.proceed(request);
        int attempt = 1;

        // 2. Обрабатываем ошибки 429 (Too Many Requests) и выполняем повторные попытки
        while (response.code() == 429 && attempt <= maxRetries) {
            log.warn("Telegram API вернул 429 Too Many Requests. Попытка {} из {}", attempt, maxRetries);

            Integer retryAfter = null;
            ResponseBody body = response.body();
            if (body != null) {
                // Читаем тело ответа полностью в строку (оно невелико)
                String bodyString = body.string();

                // Пересоздаем тело ответа, так как чтение body.string() закрывает исходный поток
                response = response.newBuilder()
                        .body(ResponseBody.create(body.contentType(), bodyString))
                        .build();

                retryAfter = parseRetryAfter(bodyString);
            }

            // Вычисляем время задержки: используем retry_after из ответа (в секундах) или 1 секунду по умолчанию
            long sleepMs = (retryAfter != null ? retryAfter * 1000L : 1000L) + bufferMs;
            log.warn("Ожидание {} мс перед повторной попыткой {}...", sleepMs, attempt);

            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Запрос прерван во время ожидания повторной отправки (retry_after)", e);
            }

            // Закрываем предыдущий ответ для освобождения ресурсов перед повторным запросом
            response.close();

            // Перед повтором запроса снова запрашиваем разрешение у Rate Limiter
            if (rateLimiter != null) {
                rateLimiter.acquire();
            }

            response = chain.proceed(request);
            attempt++;
        }

        return response;
    }

    /**
     * Извлекает значение retry_after из JSON-строки ответа с помощью регулярного выражения.
     * Это избавляет от необходимости жесткой привязки к конкретным библиотекам парсинга JSON.
     */
    private Integer parseRetryAfter(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        Matcher matcher = RETRY_AFTER_PATTERN.matcher(json);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                log.debug("Не удалось распарсить retry_after из '{}'", matcher.group(1), e);
            }
        }
        return null;
    }

    /**
     * Потокобезопасная реализация алгоритма Token Bucket для ограничения скорости.
     */
    private static class TokenBucketRateLimiter {
        private final double maxPermits;
        private double storedPermits;
        private long lastRefillTimestamp;

        public TokenBucketRateLimiter(double permitsPerSecond) {
            this.maxPermits = permitsPerSecond;
            this.storedPermits = permitsPerSecond;
            this.lastRefillTimestamp = System.nanoTime();
        }

        public synchronized void acquire() {
            refill();
            while (storedPermits < 1.0) {
                // Рассчитываем время ожидания до появления следующего разрешения в наносекундах
                long sleepTimeNanos = (long) ((1.0 - storedPermits) * (1_000_000_000.0 / maxPermits));
                if (sleepTimeNanos > 0) {
                    try {
                        TimeUnit.NANOSECONDS.sleep(sleepTimeNanos);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                refill();
            }
            storedPermits -= 1.0;
        }

        private void refill() {
            long now = System.nanoTime();
            long durationNanos = now - lastRefillTimestamp;
            if (durationNanos > 0) {
                double newPermits = durationNanos * (maxPermits / 1_000_000_000.0);
                storedPermits = Math.min(maxPermits, storedPermits + newPermits);
                lastRefillTimestamp = now;
            }
        }
    }
}
