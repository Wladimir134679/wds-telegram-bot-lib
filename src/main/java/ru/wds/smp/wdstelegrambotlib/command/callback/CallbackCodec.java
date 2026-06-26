package ru.wds.smp.wdstelegrambotlib.command.callback;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Внутренний кодек: превращает {@link Callback} в компактную строку
 * {@code callback_data} и обратно, гарантируя лимит Telegram в 64 байта.
 *
 * <p>Разработчик этот класс <b>не вызывает напрямую</b> — кодирование выполняется
 * автоматически при сборке клавиатуры
 * ({@link ru.wds.smp.wdstelegrambotlib.command.keyboard.Keyboards}), а декодирование —
 * библиотечным хендлером callback. Формат позиционно-именованный:</p>
 * <pre>
 *   command
 *   command|page=2
 *   command|page=2|rules=aB3kZ7
 * </pre>
 *
 * <p>Имена и значения параметров не должны содержать разделители {@code '|'} и
 * {@code '='}. Для произвольных/крупных данных — {@link CallbackPayloadStore} и
 * короткая ссылка в параметре.</p>
 *
 * <p>Класс потокобезопасен и неизменяем.</p>
 */
public class CallbackCodec {

    /** Жёсткий лимит Telegram на {@code callback_data} в байтах (UTF-8). */
    public static final int MAX_CALLBACK_DATA_BYTES = 64;

    private static final char FIELD_DELIMITER = '|';
    private static final char KV_DELIMITER = '=';

    /**
     * Кодирует {@link Callback} в строку {@code callback_data}.
     *
     * @param callback структура callback
     * @return строка для поля {@code callback_data} кнопки
     * @throws CallbackException если результат превышает 64 байта или часть содержит
     *                           символ-разделитель
     */
    public String encode(Callback callback) {
        check("команды", callback.command());
        StringBuilder sb = new StringBuilder(callback.command());
        for (Map.Entry<String, String> param : callback.params().entrySet()) {
            check("имени параметра", param.getKey());
            check("значения параметра", param.getValue());
            sb.append(FIELD_DELIMITER).append(param.getKey())
                    .append(KV_DELIMITER).append(param.getValue());
        }

        String encoded = sb.toString();
        int bytes = byteLength(encoded);
        if (bytes > MAX_CALLBACK_DATA_BYTES) {
            throw new CallbackException("callback_data превышает лимит " + MAX_CALLBACK_DATA_BYTES
                    + " байт (получено " + bytes + "). Перенесите крупные данные в "
                    + "CallbackPayloadStore (ctx.save(...)) и положите в кнопку короткую ссылку.");
        }
        return encoded;
    }

    /**
     * Декодирует строку {@code callback_data} в {@link Callback}.
     *
     * @param raw содержимое поля {@code callback_data} (может быть {@code null})
     * @return разобранный callback, либо {@link Optional#empty()} для пустого ввода
     */
    public Optional<Callback> decode(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Optional.empty();
        }
        String[] parts = raw.split("\\" + FIELD_DELIMITER, -1);
        String command = parts[0];
        if (command.isBlank()) {
            return Optional.empty();
        }
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            int eq = part.indexOf(KV_DELIMITER);
            if (eq <= 0) {
                continue; // пропускаем некорректные пары без ключа
            }
            params.put(part.substring(0, eq), part.substring(eq + 1));
        }
        return Optional.of(Callback.raw(command, params));
    }

    /**
     * Длина строки в байтах UTF-8 — именно так Telegram считает лимит.
     */
    public int byteLength(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private void check(String what, String value) {
        if (value != null && (value.indexOf(FIELD_DELIMITER) >= 0 || value.indexOf(KV_DELIMITER) >= 0)) {
            throw new CallbackException("Значение " + what + " содержит запрещённый символ-разделитель ('"
                    + FIELD_DELIMITER + "' или '" + KV_DELIMITER + "'): " + value);
        }
    }
}
