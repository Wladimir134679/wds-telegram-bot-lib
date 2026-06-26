package ru.wds.smp.wdstelegrambotlib.test;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.wds.smp.wdstelegrambotlib.core.TelegramBotSender;
import ru.wds.smp.wdstelegrambotlib.handler.UpdateHandler;

@Component
public class TestHandler implements UpdateHandler {

    @Override
    public boolean handle(TelegramBotSender sender, Update update) {
        Long id = update.getMessage().getChat().getId();
        try {
            sender.sendText(id, "И тебе привет " + id);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}
