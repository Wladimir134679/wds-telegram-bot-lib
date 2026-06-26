package ru.wds.smp.wdstelegrambotlib.command;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.message.Message;

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
     * <p>Возвращает id последнего <b>нового</b> отправленного сообщения (если отправка
     * порождала {@link Message}, например {@code SendMessage}). Это используется слоем
     * диалогов для запоминания «якорного» сообщения; обычным командам/callback
     * возвращаемое значение не нужно — они его игнорируют.</p>
     *
     * @param result     значение, возвращённое методом
     * @param invocation контекст вызова (для адресата ответа и отправителя)
     * @return id отправленного сообщения либо {@code null}
     */
    public Integer handle(Object result, CommandInvocation invocation) {
        if (result == null) {
            return null;
        }
        if (result instanceof Optional<?> optional) {
            return optional.map(value -> handle(value, invocation)).orElse(null);
        }
        if (result instanceof CharSequence text) {
            return sendText(text.toString(), invocation);
        }
        if (result instanceof BotApiMethod<?> method) {
            return executeAndCaptureId(method, invocation);
        }
        if (result instanceof Iterable<?> iterable) {
            Integer last = null;
            for (Object element : iterable) {
                Integer id = handle(element, invocation);
                if (id != null) {
                    last = id;
                }
            }
            return last;
        }
        if (result instanceof Object[] array) {
            Integer last = null;
            for (Object element : array) {
                Integer id = handle(element, invocation);
                if (id != null) {
                    last = id;
                }
            }
            return last;
        }
        log.warn("Неподдерживаемый тип результата команды '{}': {}",
                invocation.getCommand() != null ? invocation.getCommand().name() : "?",
                result.getClass().getName());
        return null;
    }

    private Integer executeAndCaptureId(BotApiMethod<?> method, CommandInvocation invocation) {
        Optional<?> sent = invocation.getSender().executeQuietly(method);
        return sent.filter(Message.class::isInstance).map(m -> ((Message) m).getMessageId()).orElse(null);
    }

    private Integer sendText(String text, CommandInvocation invocation) {
        Long chatId = invocation.getChatId();
        if (chatId == null) {
            log.warn("Невозможно отправить текстовый ответ: неизвестен chatId (команда '{}')",
                    invocation.getCommand() != null ? invocation.getCommand().name() : "?");
            return null;
        }
        try {
            Message sent = invocation.getSender().sendText(chatId, text);
            return sent != null ? sent.getMessageId() : null;
        } catch (Exception e) {
            log.warn("Не удалось отправить текстовый ответ в чат {}", chatId, e);
            return null;
        }
    }
}
