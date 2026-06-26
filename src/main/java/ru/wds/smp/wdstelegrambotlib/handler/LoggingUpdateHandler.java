package ru.wds.smp.wdstelegrambotlib.handler;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.wds.smp.wdstelegrambotlib.core.TelegramBotSender;

/**
 * Встроенный обработчик-логгер входящих апдейтов (пример звена цепочки).
 *
 * <p>Логирует факт получения апдейта на уровне DEBUG, не раскрывая PII: пишутся
 * только идентификаторы ({@code updateId}, {@code chatId}, тип апдейта), но не
 * текст сообщений. Это сознательно отличается от старого
 * {@code SystemHandlerTelegramServiceImpl}, который писал весь {@link Update} на
 * INFO.</p>
 *
 * <p>Никогда не прерывает цепочку — всегда возвращает {@code true}. Приоритет —
 * {@link HandlerPriority#LOGGING} (раньше маршрутизации команд, позже проверок
 * безопасности).</p>
 *
 * <p>Подключается авто-конфигурацией при {@code telegram.bot.log-updates=true}
 * (значение по умолчанию).</p>
 */
@Slf4j
public class LoggingUpdateHandler implements UpdateHandler {

    @Override
    public boolean handle(TelegramBotSender sender, Update update) {
        if (log.isInfoEnabled()) {
            log.info("Входящий апдейт: updateId={}, type={}, chatId={}",
                    update.getUpdateId(), resolveType(update), resolveChatId(update));
        }
        return true;
    }

    @Override
    public int getOrder() {
        return HandlerPriority.LOGGING;
    }

    private String resolveType(Update update) {
        if (update.hasMessage()) {
            return "message";
        }
        if (update.hasCallbackQuery()) {
            return "callback_query";
        }
        if (update.hasEditedMessage()) {
            return "edited_message";
        }
        if (update.hasInlineQuery()) {
            return "inline_query";
        }
        return "other";
    }

    private Long resolveChatId(Update update) {
        if (update.hasMessage() && update.getMessage().getChat() != null) {
            return update.getMessage().getChat().getId();
        }
        if (update.hasCallbackQuery()
                && update.getCallbackQuery().getMessage() != null
                && update.getCallbackQuery().getMessage().getChat() != null) {
            return update.getCallbackQuery().getMessage().getChat().getId();
        }
        return null;
    }
}
