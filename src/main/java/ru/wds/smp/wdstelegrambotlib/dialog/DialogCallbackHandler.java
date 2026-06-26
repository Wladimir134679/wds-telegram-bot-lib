package ru.wds.smp.wdstelegrambotlib.dialog;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import ru.wds.smp.wdstelegrambotlib.command.callback.Callback;
import ru.wds.smp.wdstelegrambotlib.command.callback.CallbackCodec;
import ru.wds.smp.wdstelegrambotlib.core.TelegramBotSender;
import ru.wds.smp.wdstelegrambotlib.handler.HandlerPriority;
import ru.wds.smp.wdstelegrambotlib.handler.UpdateHandler;

import java.util.Optional;

/**
 * Звено цепочки, направляющее нажатия inline-кнопок в <b>активный диалог</b>
 * пользователя ({@link HandlerPriority#DIALOG_CALLBACK}).
 *
 * <p>Выполняется после системного callback ({@code Callback.close()}) и раньше
 * глобальных callback-команд ({@link ru.wds.smp.wdstelegrambotlib.command.CallbackUpdateHandler}).
 * Логика:</p>
 * <ol>
 *   <li>декодирует {@code callback_data} в {@link Callback};</li>
 *   <li>если у пользователя нет активного диалога — пропускает дальше (глобальные
 *       callback-команды);</li>
 *   <li>если действие совпало с {@code @DialogCallback} активного диалога — выполняет
 *       его, снимает «часики» кнопки и прерывает цепочку;</li>
 *   <li>если действие не диалоговое — пропускает дальше (диалог не сбрасывается).</li>
 * </ol>
 */
@Slf4j
public class DialogCallbackHandler implements UpdateHandler {

    private final CallbackCodec codec;
    private final DialogStateStore store;
    private final DialogExecutor executor;

    public DialogCallbackHandler(CallbackCodec codec, DialogStateStore store, DialogExecutor executor) {
        this.codec = codec;
        this.store = store;
        this.executor = executor;
    }

    @Override
    public boolean handle(TelegramBotSender sender, Update update) {
        if (!update.hasCallbackQuery()) {
            return true;
        }
        CallbackQuery cq = update.getCallbackQuery();

        Optional<Callback> decoded = codec.decode(cq.getData());
        if (decoded.isEmpty() || decoded.get().isSystemClose()) {
            return true;
        }
        Callback callback = decoded.get();

        DialogKey key = keyOf(cq);
        if (key == null) {
            return true;
        }
        Optional<DialogState> active = store.find(key);
        if (active.isEmpty()) {
            return true; // нет диалога — отдаём глобальным callback-командам
        }

        boolean handled = executor.callback(sender, update, key, active.get(), callback, callback.command());
        if (handled) {
            answer(sender, cq);
            return false;
        }
        return true; // действие не диалоговое — пусть обработают глобальные callback-команды
    }

    @Override
    public int getOrder() {
        return HandlerPriority.DIALOG_CALLBACK;
    }

    private DialogKey keyOf(CallbackQuery cq) {
        User from = cq.getFrom();
        MaybeInaccessibleMessage source = cq.getMessage();
        if (from == null || from.getId() == null || source == null || source.getChatId() == null) {
            return null;
        }
        return new DialogKey(source.getChatId(), from.getId());
    }

    private void answer(TelegramBotSender sender, CallbackQuery cq) {
        sender.executeQuietly(new AnswerCallbackQuery(cq.getId()));
    }
}
