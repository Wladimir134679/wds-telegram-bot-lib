package ru.wds.smp.wdstelegrambotlib.command;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

import java.util.Optional;

/**
 * Интерпретирует значение, которое вернул метод-команда, как ответ пользователю —
 * аналог обработки возвращаемого значения контроллера в Spring MVC.
 *
 * <p>Это и есть «динамические сообщения и клавиатуры»: метод формирует и возвращает
 * объект, а фреймворк его отправляет. Поддерживаемые типы:</p>
 * <ul>
 *   <li>{@code null} / void — ничего не отправляется (метод сам всё сделал через
 *       {@link ru.wds.smp.wdstelegrambotlib.core.TelegramBotSender});</li>
 *   <li>{@link CharSequence} — отправляется как текст в чат-источник;</li>
 *   <li>{@link BotApiMethod} (например {@code SendMessage} с инлайн-клавиатурой) —
 *       выполняется как есть;</li>
 *   <li>{@link Optional} — разворачивается;</li>
 *   <li>{@link Iterable} / массив — каждый элемент обрабатывается рекурсивно
 *       (несколько ответов за один вызов).</li>
 * </ul>
 *
 * <p>Ошибки отправки логируются и не пробрасываются (используется «тихая»
 * отправка), чтобы сбой ответа не ломал обработку апдейта.</p>
 */
@Slf4j
public class CommandReturnValueHandler {

    /**
     * Обрабатывает результат вызова команды.
     *
     * @param result     значение, возвращённое методом-командой
     * @param invocation контекст вызова (для адресата ответа и отправителя)
     */
    public void handle(Object result, CommandInvocation invocation) {
        if (result == null) {
            return;
        }
        if (result instanceof Optional<?> optional) {
            optional.ifPresent(value -> handle(value, invocation));
            return;
        }
        if (result instanceof CharSequence text) {
            sendText(text.toString(), invocation);
            return;
        }
        if (result instanceof BotApiMethod<?> method) {
            invocation.getSender().executeQuietly(method);
            return;
        }
        if (result instanceof Iterable<?> iterable) {
            for (Object element : iterable) {
                handle(element, invocation);
            }
            return;
        }
        if (result instanceof Object[] array) {
            for (Object element : array) {
                handle(element, invocation);
            }
            return;
        }
        log.warn("Неподдерживаемый тип результата команды '{}': {}",
                invocation.getCommand() != null ? invocation.getCommand().name() : "?",
                result.getClass().getName());
    }

    private void sendText(String text, CommandInvocation invocation) {
        Long chatId = invocation.getChatId();
        if (chatId == null) {
            log.warn("Невозможно отправить текстовый ответ: неизвестен chatId (команда '{}')",
                    invocation.getCommand() != null ? invocation.getCommand().name() : "?");
            return;
        }
        try {
            invocation.getSender().sendText(chatId, text);
        } catch (Exception e) {
            log.warn("Не удалось отправить текстовый ответ в чат {}", chatId, e);
        }
    }
}
