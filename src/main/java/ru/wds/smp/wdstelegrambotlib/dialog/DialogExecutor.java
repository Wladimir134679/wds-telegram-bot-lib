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
import ru.wds.smp.wdstelegrambotlib.command.callback.Callback;
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
        ParsedCommand parsed = command(dialog.getName(), startText);
        execute(sender, update, key, state, dialog.getStart(), parsed, null, state.anchorMessageId(), "<start>");
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
        String input = update.hasMessage() && update.getMessage().getText() != null
                ? update.getMessage().getText() : "";
        ParsedCommand parsed = command(state.dialogName(), input.strip());
        // Текстовый шаг редактирует «якорное» сообщение (последнее меню), если оно есть.
        execute(sender, update, key, state, step, parsed, null, state.anchorMessageId(), state.currentStep());
        return true;
    }

    /**
     * Передаёт нажатие кнопки обработчику {@code @DialogCallback} активного диалога.
     *
     * @param sender   отправитель
     * @param update   апдейт с callback_query
     * @param key      ключ сессии
     * @param state    активное состояние диалога
     * @param callback разобранные данные нажатой кнопки
     * @param action   имя действия (для метки/диагностики)
     * @return {@code true}, если обработчик найден и выполнен; {@code false}, если
     *         действие не относится к диалогу (нужно отдать глобальным callback-командам)
     */
    public boolean callback(TelegramBotSender sender, Update update, DialogKey key, DialogState state,
                            Callback callback, String action) {
        DialogDefinition dialog = registry.find(state.dialogName());
        DialogStepDefinition handler = dialog != null ? dialog.callback(action) : null;
        if (handler == null) {
            return false; // не диалоговое действие — пусть обработают глобальные callback-команды
        }
        // Сообщение с кнопкой становится «якорным» — его и будет редактировать ctx.edit(...).
        Integer messageId = callbackMessageId(update);
        if (messageId != null) {
            state.setAnchorMessageId(messageId);
        }
        ParsedCommand parsed = command(state.dialogName(), "");
        execute(sender, update, key, state, handler, parsed, callback, messageId, action);
        return true;
    }

    private void execute(TelegramBotSender sender, Update update, DialogKey key, DialogState state,
                         DialogStepDefinition definition, ParsedCommand parsed, Callback callback,
                         Integer effectiveMessageId, String stepLabel) {
        DialogContextImpl ctx = new DialogContextImpl(key, state, effectiveMessageId);
        CommandInvocation invocation = buildInvocation(update, sender, parsed, callback, ctx, key, effectiveMessageId);
        try {
            Object result = definition.invoke(invocation);
            Integer sentId = returnValueHandler.handle(result, invocation);
            if (sentId != null) {
                // Отправлено новое сообщение (меню) — оно становится «якорем» для будущих правок.
                state.setAnchorMessageId(sentId);
            }
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
     * Собирает контекст вызова для шага/callback диалога, устойчиво к источнику
     * (сообщение или callback). {@code chatId} берётся из ключа; для callback в
     * {@link Message} попадает сообщение с кнопкой (его и редактирует {@code ctx.edit}),
     * а {@code callback}/{@code callbackQuery} проставляются, чтобы работали
     * {@code @Param}/{@code @Payload}.
     */
    private CommandInvocation buildInvocation(Update update, TelegramBotSender sender, ParsedCommand parsed,
                                              Callback callback, DialogContext ctx, DialogKey key,
                                              Integer effectiveMessageId) {
        Message message = update.hasMessage() ? update.getMessage() : null;
        CallbackQuery cq = update.hasCallbackQuery() ? update.getCallbackQuery() : null;
        User user = null;
        if (message != null) {
            user = message.getFrom();
        } else if (cq != null) {
            user = cq.getFrom();
            if (cq.getMessage() instanceof Message m) {
                message = m; // сообщение с кнопкой — для инъекции Message и адреса редактирования
            }
        }
        return CommandInvocation.builder()
                .update(update)
                .message(message)
                .callbackQuery(cq)
                .callback(callback)
                .chat(message != null ? message.getChat() : null)
                .user(user)
                .sender(sender)
                .command(parsed)
                .chatId(key.chatId())
                .messageId(effectiveMessageId)
                .payloadStore(payloadStore)
                .dialogContext(ctx)
                .build();
    }

    private Integer callbackMessageId(Update update) {
        if (!update.hasCallbackQuery()) {
            return null;
        }
        return update.getCallbackQuery().getMessage() != null
                ? update.getCallbackQuery().getMessage().getMessageId() : null;
    }

    private ParsedCommand command(String dialogName, String text) {
        if (text == null || text.isEmpty()) {
            return new ParsedCommand(dialogName, "", List.of());
        }
        return new ParsedCommand(dialogName, text, Arrays.asList(text.split("\\s+")));
    }
}
