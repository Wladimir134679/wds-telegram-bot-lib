package ru.wds.smp.wdstelegrambotlib.dialog;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.wds.smp.wdstelegrambotlib.core.TelegramBotSender;
import ru.wds.smp.wdstelegrambotlib.handler.HandlerPriority;
import ru.wds.smp.wdstelegrambotlib.handler.UpdateHandler;

/**
 * Финальная заглушка цепочки ({@link HandlerPriority#FALLBACK}): отвечает на
 * текстовые сообщения, которые не подхватили ни обычная команда, ни активный диалог
 * («команда неизвестна, нет активного диалога»).
 *
 * <p>Доходит до этого звена только апдейт, который никто раньше не «поглотил»
 * (предыдущие звенья при обработке возвращают {@code false} и прерывают цепочку).
 * Так реализуется требование: если контекст пуст и пришло необработанное сообщение —
 * подсказать пользователю.</p>
 *
 * <p><b>Опциональность и осторожность в группах.</b> Включается только заданием
 * непустого {@code telegram.bot.fallback.message}. В групповых чатах бот может
 * получать все сообщения — чтобы не спамить, по умолчанию отвечает лишь в личных
 * чатах (см. {@code telegram.bot.fallback.private-only}, по умолчанию {@code true}).</p>
 */
@Slf4j
public class FallbackUpdateHandler implements UpdateHandler {

    private final String message;
    private final boolean privateOnly;

    /**
     * @param message     текст ответа на необработанное сообщение (непустой)
     * @param privateOnly отвечать только в личных чатах
     */
    public FallbackUpdateHandler(String message, boolean privateOnly) {
        this.message = message;
        this.privateOnly = privateOnly;
    }

    @Override
    public boolean handle(TelegramBotSender sender, Update update) {
        if (!update.hasMessage()) {
            return true;
        }
        Message msg = update.getMessage();
        String text = msg.getText();
        if (text == null || text.isBlank()) {
            return true;
        }
        if (privateOnly && (msg.getChat() == null || !Boolean.TRUE.equals(msg.getChat().isUserChat()))) {
            return true;
        }
        Long chatId = msg.getChat() != null ? msg.getChat().getId() : null;
        if (chatId == null) {
            return true;
        }
        sender.executeQuietly(org.telegram.telegrambots.meta.api.methods.send.SendMessage.builder()
                .chatId(chatId)
                .text(message)
                .build());
        return false;
    }

    @Override
    public int getOrder() {
        return HandlerPriority.FALLBACK;
    }
}
