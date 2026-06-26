package ru.wds.smp.wdstelegrambotlib.command.callback;

/**
 * Бросается, когда обязательные данные по ссылке {@code payloadId} не найдены —
 * как правило, потому что истёк их TTL (пользователь нажал старую кнопку).
 *
 * <p>Обрабатывается мягко: пользователю отвечают, что данные устарели, без падения
 * бота. Это типовой пользовательский сценарий, а не сбой инфраструктуры.</p>
 */
public class CallbackPayloadExpiredException extends RuntimeException {

    public CallbackPayloadExpiredException(String message) {
        super(message);
    }
}
