package ru.wds.smp.wdstelegrambotlib.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;

/**
 * Свойства конфигурации Telegram-бота (префикс {@code telegram.bot}).
 *
 * <p>Это типобезопасная замена ручного чтения YAML из старого проекта. Значения
 * берутся из стандартного {@code Environment} Spring (application.yaml,
 * переменные окружения и т.д.).</p>
 *
 * <p><b>Безопасность:</b> токен бота — секрет. Он намеренно исключён из
 * {@link #toString()} (маскируется), чтобы случайно не утечь в логи. Никогда не
 * логируй объект свойств целиком на уровне INFO без маскирования.</p>
 *
 * <p><b>Активация:</b> авто-конфигурация подключается только при заданном
 * {@code telegram.bot.token}. Без токена библиотека не создаёт ни одного бина.</p>
 *
 * @see TelegramBotAutoConfiguration
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "telegram.bot")
public class TelegramBotProperties {

    /**
     * Токен бота, выданный {@code @BotFather}. Обязателен для работы библиотеки.
     * Является секретом — не логируется (см. {@link #toString()}).
     */
    private String token;

    /**
     * Username бота без ведущего {@code @} (например, {@code my_cool_bot}).
     * Необязателен; используется, в частности, для отрезания суффикса
     * {@code @username} у команд в группах.
     */
    private String username;

    /**
     * Включать ли встроенный обработчик-логгер входящих апдейтов
     * ({@link ru.wds.smp.wdstelegrambotlib.handler.LoggingUpdateHandler}).
     * Логирует на уровне DEBUG, без текста сообщений и PII. По умолчанию — включён.
     */
    private boolean logUpdates = true;

    /**
     * Настройки callback-кнопок и хранилища «больших» данных.
     */
    @NestedConfigurationProperty
    private Callback callback = new Callback();

    /**
     * Настройки работы с callback (inline-кнопки и серверное хранилище данных).
     */
    @Getter
    @Setter
    public static class Callback {

        /**
         * Время жизни записей в {@link ru.wds.smp.wdstelegrambotlib.command.callback.CallbackPayloadStore}.
         * По истечении данные вычищаются фоново. Должно быть положительным.
         * По умолчанию — 10 минут.
         */
        private Duration payloadTtl = Duration.ofMinutes(10);
    }

    /**
     * Возвращает безопасную для логов маску токена: показывает несколько первых
     * символов и длину, не раскрывая сам секрет. {@code null}/пустой токен —
     * возвращает {@code "<none>"}.
     *
     * @return маскированное представление токена, пригодное для логов
     */
    public String maskedToken() {
        if (token == null || token.isBlank()) {
            return "<none>";
        }
        int visible = Math.min(4, token.length());
        return token.substring(0, visible) + "***(len=" + token.length() + ")";
    }

    @Override
    public String toString() {
        return "TelegramBotProperties{token=" + maskedToken()
                + ", username=" + username
                + ", logUpdates=" + logUpdates + '}';
    }
}
