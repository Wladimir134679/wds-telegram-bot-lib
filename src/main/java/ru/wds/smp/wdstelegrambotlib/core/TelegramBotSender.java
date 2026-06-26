package ru.wds.smp.wdstelegrambotlib.core;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * Абстракция отправки сообщений и выполнения методов Telegram Bot API.
 *
 * <p>Тонкая обёртка над {@link TelegramClient}, единая точка для исходящих
 * вызовов: отправка сообщений, ответы пользователю, любые методы API. В отличие
 * от старого {@code TelegramLongPollingEngine}, не создаёт клиента сама и не
 * смешивает роли «отправитель» и «consumer» — клиент инжектируется, что делает
 * класс тестируемым (можно подставить мок {@link TelegramClient}).</p>
 *
 * <p>Предоставляет два стиля вызова:</p>
 * <ul>
 *   <li>{@link #execute(BotApiMethod)} — пробрасывает {@link TelegramApiException}
 *       вызывающему (когда ошибку нужно обработать явно);</li>
 *   <li>{@link #executeQuietly(BotApiMethod)} — «тихий» вариант, логирует ошибку и
 *       возвращает {@link Optional}, не бросая исключение (удобно в обработчиках,
 *       где падение нежелательно).</li>
 * </ul>
 *
 * <p><b>Именование.</b> «Тихий» метод честно называется {@code executeQuietly} и
 * действительно глотает исключение — в отличие от старого {@code executeNotException},
 * который, вопреки имени, бросал {@link RuntimeException}.</p>
 *
 * <p><b>Потокобезопасность.</b> Класс не хранит изменяемого состояния; безопасность
 * определяется реализацией {@link TelegramClient} (стандартный OkHttp-клиент
 * потокобезопасен).</p>
 */
@Slf4j
public class TelegramBotSender {

    private final TelegramClient telegramClient;

    /**
     * @param telegramClient клиент Telegram API для исходящих вызовов; не {@code null}
     */
    public TelegramBotSender(TelegramClient telegramClient) {
        this.telegramClient = Objects.requireNonNull(telegramClient, "telegramClient не должен быть null");
    }

    /**
     * Выполняет произвольный метод Bot API, пробрасывая ошибку вызывающему.
     *
     * @param method метод API (например, {@link SendMessage})
     * @param <T>    тип результата метода
     * @param <M>    конкретный тип метода API
     * @return результат выполнения метода
     * @throws TelegramApiException если вызов к Telegram API завершился ошибкой
     */
    public <T extends Serializable, M extends BotApiMethod<T>> T execute(M method) throws TelegramApiException {
        Objects.requireNonNull(method, "method не должен быть null");
        return telegramClient.execute(method);
    }

    /**
     * «Тихо» выполняет метод Bot API: при ошибке логирует её и возвращает пустой
     * {@link Optional} вместо проброса исключения. Подходит для обработчиков, где
     * сбой отправки не должен ронять обработку апдейта.
     *
     * @param method метод API
     * @param <T>    тип результата метода
     * @param <M>    конкретный тип метода API
     * @return результат в {@link Optional}, либо {@link Optional#empty()} при ошибке
     */
    public <T extends Serializable, M extends BotApiMethod<T>> Optional<T> executeQuietly(M method) {
        try {
            return Optional.ofNullable(execute(method));
        } catch (TelegramApiException e) {
            log.warn("Не удалось выполнить метод Telegram API: {}", method.getClass().getSimpleName(), e);
            return Optional.empty();
        }
    }

    /**
     * Отправляет текстовое сообщение в указанный чат.
     *
     * @param chatId идентификатор чата получателя
     * @param text   текст сообщения
     * @return отправленное {@link Message}
     * @throws TelegramApiException если отправка не удалась
     */
    public Message sendText(Long chatId, String text) throws TelegramApiException {
        Objects.requireNonNull(chatId, "chatId не должен быть null");
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        return execute(message);
    }

    /**
     * Предоставляет доступ к нижележащему клиенту для вызовов, не покрытых
     * удобными методами (отправка файлов, медиа-групп, загрузка файлов и т.п.).
     *
     * @return нижележащий {@link TelegramClient}
     */
    public TelegramClient getClient() {
        return telegramClient;
    }
}
