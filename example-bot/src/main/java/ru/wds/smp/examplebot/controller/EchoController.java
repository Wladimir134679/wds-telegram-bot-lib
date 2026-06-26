package ru.wds.smp.examplebot.controller;

import ru.wds.smp.wdstelegrambotlib.command.annotation.BotController;
import ru.wds.smp.wdstelegrambotlib.command.annotation.CommandMapping;
import ru.wds.smp.wdstelegrambotlib.command.annotation.Text;

/**
 * Простейшие текстовые команды, демонстрирующие параметр {@link Text} — «хвост»
 * сообщения после имени команды.
 *
 * <p>Для команды {@code /say привет всем} в метод придёт строка {@code "привет всем"}
 * целиком. Возврат {@link String} библиотека трактует как текстовый ответ в
 * чат-источник.</p>
 */
@BotController
public class EchoController {

    /**
     * {@code /say <текст>} — возвращает переданный текст без изменений.
     *
     * @param text «хвост» сообщения после {@code /say}; может быть пустым
     * @return эхо-ответ либо подсказка, если текст не указан
     */
    @CommandMapping("say")
    public String say(@Text String text) {
        if (text == null || text.isBlank()) {
            return "Напиши текст после команды, например: /say привет";
        }
        return text;
    }

    /**
     * {@code /upper <текст>} — возвращает текст в верхнем регистре (демонстрация
     * обработки «хвоста» команды).
     *
     * @param text «хвост» сообщения после {@code /upper}
     * @return текст в верхнем регистре либо подсказка
     */
    @CommandMapping("upper")
    public String upper(@Text String text) {
        if (text == null || text.isBlank()) {
            return "Напиши текст после команды, например: /upper привет";
        }
        return text.toUpperCase();
    }
}
