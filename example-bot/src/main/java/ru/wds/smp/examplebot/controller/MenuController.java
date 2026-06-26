package ru.wds.smp.examplebot.controller;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.wds.smp.wdstelegrambotlib.command.CommandInvocation;
import ru.wds.smp.wdstelegrambotlib.command.CommandType;
import ru.wds.smp.wdstelegrambotlib.command.annotation.BotController;
import ru.wds.smp.wdstelegrambotlib.command.annotation.CommandMapping;
import ru.wds.smp.wdstelegrambotlib.command.annotation.Param;
import ru.wds.smp.wdstelegrambotlib.command.callback.Callback;
import ru.wds.smp.wdstelegrambotlib.command.keyboard.Keyboards;

/**
 * Демонстрация inline-кнопок и callback-команд.
 *
 * <p>Показывает связку «команда строит клавиатуру → нажатие приходит callback'ом →
 * метод-обработчик callback редактирует или дополняет сообщение»:</p>
 * <ul>
 *   <li>{@code /menu} — отправляет сообщение с inline-клавиатурой;</li>
 *   <li>callback {@code counter} — счётчик: {@link EditMessageText} перерисовывает
 *       то же сообщение с новым значением (без спама новыми сообщениями), читает
 *       параметр кнопки через {@link Param};</li>
 *   <li>callback {@code open} — открывает «элемент» по числовому id;</li>
 *   <li>кнопка {@link Callback#close()} — закрытие клавиатуры обрабатывает сама
 *       библиотека.</li>
 * </ul>
 *
 * <p>Все значения в кнопках — короткие примитивы, влезающие в лимит
 * {@code callback_data} 64 байта. Кодирование/проверку длины делает библиотека.</p>
 */
@BotController
public class MenuController {

    /**
     * {@code /menu}: сообщение с inline-клавиатурой.
     *
     * @param chat чат-источник (адрес ответа)
     * @return сообщение с кнопками
     */
    @CommandMapping({"menu", "📋 Меню"})
    public SendMessage menu(Chat chat) {
        return SendMessage.builder()
                .chatId(chat.getId())
                .text("Меню демонстрации callback-кнопок. Текущее значение счётчика: 0")
                .replyMarkup(counterKeyboard(0))
                .build();
    }

    /**
     * Callback {@code counter}: изменяет значение счётчика и перерисовывает то же
     * сообщение через {@link EditMessageText}.
     *
     * @param value текущее значение из нажатой кнопки ({@link Param})
     * @param ctx   контекст вызова (нужен chatId и messageId редактируемого сообщения)
     * @return метод редактирования сообщения
     */
    @CommandMapping(value = "counter", type = CommandType.CALLBACK)
    public EditMessageText counter(@Param("value") int value, CommandInvocation ctx) {
        return EditMessageText.builder()
                .chatId(String.valueOf(ctx.getChatId()))
                .messageId(ctx.getMessageId())
                .text("Меню демонстрации callback-кнопок. Текущее значение счётчика: " + value)
                .replyMarkup(counterKeyboard(value))
                .build();
    }

    /**
     * Callback {@code open}: «открывает» элемент по id. Возврат строки библиотека
     * отправит новым сообщением в чат-источник.
     *
     * @param id идентификатор элемента из кнопки ({@link Param}, обязательный)
     * @return текстовое подтверждение
     */
    @CommandMapping(value = "open", type = CommandType.CALLBACK)
    public String open(@Param(value = "id", required = true) long id) {
        return "Открыт элемент #" + id;
    }

    /**
     * Собирает клавиатуру счётчика: уменьшить / текущее значение / увеличить,
     * строкой ниже — пример кнопки-действия и закрытие.
     *
     * @param value текущее значение счётчика
     * @return разметка inline-клавиатуры
     */
    private InlineKeyboardMarkup counterKeyboard(int value) {
        return Keyboards.inline()
                .row()
                    .button("➖", Callback.to("counter").arg("value", value - 1))
                    .button(String.valueOf(value), Callback.to("counter").arg("value", value))
                    .button("➕", Callback.to("counter").arg("value", value + 1))
                .row()
                    .button("Открыть элемент #42", Callback.to("open").arg("id", 42))
                    .url("Документация", "https://core.telegram.org/bots/api")
                .row()
                    .button("✖ Закрыть", Callback.close())
                .build();
    }
}
