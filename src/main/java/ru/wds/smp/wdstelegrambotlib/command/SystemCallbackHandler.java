package ru.wds.smp.wdstelegrambotlib.command;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import ru.wds.smp.wdstelegrambotlib.command.callback.Callback;
import ru.wds.smp.wdstelegrambotlib.command.callback.CallbackCodec;
import ru.wds.smp.wdstelegrambotlib.core.TelegramBotSender;
import ru.wds.smp.wdstelegrambotlib.handler.HandlerPriority;
import ru.wds.smp.wdstelegrambotlib.handler.UpdateHandler;

import java.util.Optional;

/**
 * Предобрабатывающее звено, ловящее <b>системные</b> callback библиотеки.
 *
 * <p>Сейчас обрабатывает {@link Callback#close()} — «закрыть клавиатуру»: убирает
 * inline-клавиатуру с сообщения, на котором нажата кнопка, и прерывает цепочку, не
 * доводя апдейт до обычной маршрутизации. Разработчику для типового «закрыть меню»
 * не нужно писать свой обработчик.</p>
 *
 * <p>Приоритет {@link HandlerPriority#SYSTEM_CALLBACK} — раньше
 * {@link CallbackUpdateHandler}.</p>
 */
@Slf4j
public class SystemCallbackHandler implements UpdateHandler {

    private final CallbackCodec codec;

    public SystemCallbackHandler(CallbackCodec codec) {
        this.codec = codec;
    }

    @Override
    public boolean handle(TelegramBotSender sender, Update update) {
        if (!update.hasCallbackQuery()) {
            return true;
        }
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Optional<Callback> decoded = codec.decode(callbackQuery.getData());
        if (decoded.isEmpty() || !decoded.get().isSystemClose()) {
            return true;
        }

        removeKeyboard(sender, callbackQuery);
        sender.executeQuietly(new AnswerCallbackQuery(callbackQuery.getId()));
        // Системный callback обработан — дальше по цепочке не пускаем.
        return false;
    }

    @Override
    public int getOrder() {
        return HandlerPriority.SYSTEM_CALLBACK;
    }

    private void removeKeyboard(TelegramBotSender sender, CallbackQuery callbackQuery) {
        MaybeInaccessibleMessage source = callbackQuery.getMessage();
        if (source == null || source.getChatId() == null || source.getMessageId() == null) {
            return;
        }
        sender.executeQuietly(EditMessageReplyMarkup.builder()
                .chatId(source.getChatId())
                .messageId(source.getMessageId())
                .replyMarkup(null)
                .build());
    }
}
