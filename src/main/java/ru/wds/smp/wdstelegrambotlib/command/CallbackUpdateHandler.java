package ru.wds.smp.wdstelegrambotlib.command;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.wds.smp.wdstelegrambotlib.command.callback.Callback;
import ru.wds.smp.wdstelegrambotlib.command.callback.CallbackCodec;
import ru.wds.smp.wdstelegrambotlib.command.callback.CallbackPayloadExpiredException;
import ru.wds.smp.wdstelegrambotlib.command.callback.CallbackPayloadStore;
import ru.wds.smp.wdstelegrambotlib.core.TelegramBotSender;
import ru.wds.smp.wdstelegrambotlib.handler.HandlerPriority;
import ru.wds.smp.wdstelegrambotlib.handler.UpdateHandler;

import java.util.Optional;

/**
 * Звено цепочки, маршрутизирующее нажатия inline-кнопок (callback-команды).
 *
 * <p>Декодирует {@code callback_data} через {@link CallbackCodec} в {@link Callback},
 * ищет команду в {@link CommandRegistry} по типу {@link CommandType#CALLBACK},
 * вызывает метод-контроллер и передаёт результат в {@link CommandReturnValueHandler}.</p>
 *
 * <p>Системные callback ({@link Callback#close()}) сюда не доходят — их раньше
 * перехватывает {@link SystemCallbackHandler}.</p>
 *
 * <p>Всегда отправляет {@link AnswerCallbackQuery}, чтобы снять «часики» на кнопке;
 * при истёкших данных/некорректном аргументе — с alert-текстом.</p>
 */
@Slf4j
public class CallbackUpdateHandler implements UpdateHandler {

    private final CommandRegistry registry;
    private final CallbackCodec codec;
    private final CommandReturnValueHandler returnValueHandler;
    private final CallbackPayloadStore payloadStore;

    public CallbackUpdateHandler(CommandRegistry registry, CallbackCodec codec,
                                 CommandReturnValueHandler returnValueHandler,
                                 CallbackPayloadStore payloadStore) {
        this.registry = registry;
        this.codec = codec;
        this.returnValueHandler = returnValueHandler;
        this.payloadStore = payloadStore;
    }

    @Override
    public boolean handle(TelegramBotSender sender, Update update) {
        if (!update.hasCallbackQuery()) {
            return true;
        }
        CallbackQuery callbackQuery = update.getCallbackQuery();

        Optional<Callback> decoded = codec.decode(callbackQuery.getData());
        if (decoded.isEmpty() || decoded.get().isSystemClose()) {
            return true;
        }
        Callback callback = decoded.get();

        CommandDefinition definition = registry.find(callback.command(), CommandType.CALLBACK);
        if (definition == null) {
            answer(sender, callbackQuery, null, false);
            return true;
        }

        CommandInvocation invocation = CommandInvocation.fromCallback(update, sender, callback, payloadStore);
        try {
            Object result = definition.invoke(invocation);
            returnValueHandler.handle(result, invocation);
            answer(sender, callbackQuery, null, false);
        } catch (CallbackPayloadExpiredException e) {
            log.debug("Истёкшие данные callback '{}': {}", callback.command(), e.getMessage());
            answer(sender, callbackQuery, e.getMessage(), true);
        } catch (CommandArgumentException e) {
            log.debug("Ошибка аргумента callback '{}': {}", callback.command(), e.getMessage());
            answer(sender, callbackQuery, e.getMessage(), true);
        }
        return true;
    }

    @Override
    public int getOrder() {
        return HandlerPriority.COMMAND_PROCESSING;
    }

    private void answer(TelegramBotSender sender, CallbackQuery callbackQuery, String text, boolean alert) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackQuery.getId());
        if (text != null) {
            answer.setText(text);
            answer.setShowAlert(alert);
        }
        sender.executeQuietly(answer);
    }
}
