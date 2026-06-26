package ru.wds.smp.wdstelegrambotlib.command.callback;

/**
 * Ошибка кодирования {@link Callback} в {@code callback_data}: превышение лимита
 * Telegram (64 байта) или недопустимый символ-разделитель в имени/значении параметра.
 */
public class CallbackException extends RuntimeException {

    public CallbackException(String message) {
        super(message);
    }
}
