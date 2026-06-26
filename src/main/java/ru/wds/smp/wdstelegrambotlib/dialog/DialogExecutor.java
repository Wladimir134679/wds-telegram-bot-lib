package ru.wds.smp.wdstelegrambotlib.dialog;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.wds.smp.wdstelegrambotlib.command.CommandArgumentException;
import ru.wds.smp.wdstelegrambotlib.command.CommandInvocation;
import ru.wds.smp.wdstelegrambotlib.command.CommandReturnValueHandler;
import ru.wds.smp.wdstelegrambotlib.command.ParsedCommand;
import ru.wds.smp.wdstelegrambotlib.command.callback.CallbackPayloadStore;
import ru.wds.smp.wdstelegrambotlib.core.TelegramBotSender;

import java.util.Arrays;
import java.util.List;

/**
 * Общее ядро исполнения диалогов: запуск {@code @DialogStart} и шага
 * {@code @DialogStep}, применение исхода ({@code next}/{@code finish}/{@code cancel})
 * к состоянию.
 *
 * <p>Вынесено отдельно, чтобы одной и той же логикой пользовались и
 * {@link DialogUpdateHandler} (диалог, запущенный текстом-триггером), и
 * {@link DialogManager} (диалог, запущенный программно из обычной команды или
 * callback). Все изменения {@link DialogState} сосредоточены здесь — единая точка
 * согласованности.</p>
 */
@Slf4j
public class DialogExecutor {

    private final DialogRegistry registry;
    private final DialogStateStore store;
    private final CommandReturnValueHandler returnValueHandler;
    private final CallbackPayloadStore payloadStore;

    public DialogExecutor(DialogRegistry registry, DialogStateStore store,
                          CommandReturnValueHandler returnValueHandler, CallbackPayloadStore payloadStore) {
        this.registry = registry;
        this.store = store;
        this.returnValueHandler = returnValueHandler;
        this.payloadStore = payloadStore;
    }

    /**
     * Запускает диалог: создаёт состояние (заменяя активное) и выполняет
     * {@code @DialogStart}.
     *
     * @param sender    отправитель
     * @param update    исходный апдейт (сообщение или callback)
     * @param key       ключ сессии
     * @param dialog    определение диалога
     * @param startText текст для {@code @Text} в старте (обычно «хвост» после триггера)
     */
    public void start(TelegramBotSender sender, Update update, DialogKey key,
                      DialogDefinition dialog, String startText) {
        DialogState state = store.start(key, dialog.getName());
        DialogContextImpl ctx = new DialogContextImpl(key, state);
        ParsedCommand parsed = command(dialog.getName(), startText);
        invokeAndApply(sender, update, key, state, ctx, dialog.getStart(), parsed, "<start>");
    }

    /**
     * Передаёт сообщение текущему шагу активного диалога.
     *
     * @param sender отправитель
     * @param update исходный апдейт-сообщение
     * @param key    ключ сессии
     * @param state  активное состояние диалога
     * @return {@code true}, если шаг найден и выполнен; {@code false}, если шаг не
     *         найден (состояние при этом сбрасывается)
     */
    public boolean step(TelegramBotSender sender, Update update, DialogKey key, DialogState state) {
        DialogDefinition dialog = registry.find(state.dialogName());
        DialogStepDefinition step = dialog != null ? dialog.step(state.currentStep()) : null;
        if (step == null) {
            log.warn("Активный диалог '{}' указывает на несуществующий шаг '{}' — сбрасываю",
                    state.dialogName(), state.currentStep());
            store.remove(key);
            return false;
        }
        DialogContextImpl ctx = new DialogContextImpl(key, state);
        String input = update.hasMessage() && update.getMessage().getText() != null
                ? update.getMessage().getText() : "";
        ParsedCommand parsed = command(state.dialogName(), input.strip());
        invokeAndApply(sender, update, key, state, ctx, step, parsed, state.currentStep());
        return true;
    }

    private void invokeAndApply(TelegramBotSender sender, Update update, DialogKey key, DialogState state,
                                DialogContextImpl ctx, DialogStepDefinition definition, ParsedCommand parsed,
                                String stepLabel) {
        CommandInvocation invocation = buildInvocation(update, sender, parsed, ctx, key);
        try {
            Object result = definition.invoke(invocation);
            returnValueHandler.handle(result, invocation);
        } catch (CommandArgumentException e) {
            log.debug("Диалог '{}', шаг '{}': ошибка ввода: {}", state.dialogName(), stepLabel, e.getMessage());
            if (invocation.getChatId() != null) {
                sender.executeQuietly(SendMessage.builder()
                        .chatId(invocation.getChatId())
                        .text("⚠️ " + e.getMessage())
                        .build());
            }
            store.save(key, state); // остаёмся на шаге, продлеваем TTL
            return;
        } catch (Exception e) {
            log.error("Диалог '{}', шаг '{}': ошибка выполнения", state.dialogName(), stepLabel, e);
            store.save(key, state);
            return;
        }
        applyOutcome(key, state, ctx);
    }

    private void applyOutcome(DialogKey key, DialogState state, DialogContextImpl ctx) {
        switch (ctx.outcome()) {
            case NEXT -> {
                state.setCurrentStep(ctx.nextStep());
                state.pushHistory(ctx.nextStep());
                store.save(key, state);
            }
            case FINISH, CANCEL -> store.remove(key);
            case STAY -> store.save(key, state); // продлеваем TTL, шаг прежний
        }
    }

    /**
     * Собирает контекст вызова для шага диалога, устойчиво к источнику (сообщение
     * или callback): {@code chatId} берётся из ключа, пользователь — из сообщения
     * либо из callback. {@link Message} равен {@code null}, если апдейт не
     * сообщение (например, программный запуск из callback).
     */
    private CommandInvocation buildInvocation(Update update, TelegramBotSender sender,
                                              ParsedCommand parsed, DialogContext ctx, DialogKey key) {
        Message message = update.hasMessage() ? update.getMessage() : null;
        User user = null;
        Integer messageId = null;
        if (message != null) {
            user = message.getFrom();
            messageId = message.getMessageId();
        } else if (update.hasCallbackQuery()) {
            CallbackQuery cq = update.getCallbackQuery();
            user = cq.getFrom();
        }
        return CommandInvocation.builder()
                .update(update)
                .message(message)
                .chat(message != null ? message.getChat() : null)
                .user(user)
                .sender(sender)
                .command(parsed)
                .chatId(key.chatId())
                .messageId(messageId)
                .payloadStore(payloadStore)
                .dialogContext(ctx)
                .build();
    }

    private ParsedCommand command(String dialogName, String text) {
        if (text == null || text.isEmpty()) {
            return new ParsedCommand(dialogName, "", List.of());
        }
        return new ParsedCommand(dialogName, text, Arrays.asList(text.split("\\s+")));
    }
}
