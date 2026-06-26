package ru.wds.smp.examplebot.controller;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import ru.wds.smp.wdstelegrambotlib.command.CommandType;
import ru.wds.smp.wdstelegrambotlib.command.annotation.BotController;
import ru.wds.smp.wdstelegrambotlib.command.annotation.CommandMapping;
import ru.wds.smp.wdstelegrambotlib.command.callback.Callback;
import ru.wds.smp.wdstelegrambotlib.command.keyboard.Keyboards;

/**
 * Точка входа в бота: приветствие, справка и нижнее (reply) меню.
 *
 * <p>Демонстрирует:</p>
 * <ul>
 *   <li>инъекцию контекстных параметров по типу ({@link User}, {@link Chat});</li>
 *   <li>возврат {@link SendMessage} с reply-клавиатурой
 *       ({@link Keyboards#reply()});</li>
 *   <li>маршрутизацию reply-кнопок: подпись кнопки приходит обычным сообщением и
 *       матчится по точному совпадению с алиасом команды;</li>
 *   <li>несколько алиасов одной команды.</li>
 * </ul>
 */
@BotController
public class StartController {

    /**
     * Команда {@code /start}: приветствие по имени и показ нижнего меню.
     *
     * @param user инициатор команды (инъекция по типу)
     * @param chat чат-источник (для адреса ответа)
     * @return сообщение с reply-клавиатурой
     */
    @CommandMapping("start")
    public SendMessage start(User user, Chat chat) {
        String name = user != null && user.getFirstName() != null ? user.getFirstName() : "друг";
        return SendMessage.builder()
                .chatId(chat.getId())
                .text("""
                        Привет, %s! 👋

                        Это демонстрационный бот библиотеки wds-telegram-bot-lib.
                        Нажми кнопку ниже или набери команду:
                        • /menu — инлайн-кнопки и callback
                        • /profile — демонстрация «больших» данных (payload)
                        • /say <текст> — эхо (демонстрация @Text)
                        • /upper <текст> — текст в ВЕРХНЕМ регистре
                        • /help — справка
                        """.formatted(name))
                .replyMarkup(Keyboards.reply()
                        .row().text("📋 Меню").text("🔢 Профиль")
                        .row().text("❓ Помощь")
                        .resize()
                        .build())
                .build();
    }

    /**
     * Команда {@code /help} (и reply-кнопка «❓ Помощь»): список возможностей.
     *
     * @return текст справки (отправляется как сообщение в чат-источник)
     */
    @CommandMapping({"help", "❓ Помощь"})
    public String help() {
        return """
                📖 Справка

                Текстовые команды:
                • /start — приветствие и меню
                • /say <текст> — вернёт тот же текст (@Text)
                • /upper <текст> — текст в верхнем регистре
                • /menu — сообщение с inline-кнопками (callback)
                • /profile — сохранит данные на сервере и пришлёт кнопку,
                  раскрывающую их (демонстрация @Payload и TTL)
                • /close_demo — кнопка, закрывающая inline-клавиатуру

                Inline-кнопки в /menu: счётчик (редактирование сообщения),
                открытие элемента по id, закрытие клавиатуры.
                """;
    }

    /**
     * Команда {@code /close_demo}: показывает {@link Callback#close()} —
     * «системный» callback, который библиотека обрабатывает сама (убирает
     * inline-клавиатуру с сообщения), без отдельного метода-обработчика.
     *
     * @param chat чат-источник (для адреса ответа)
     * @return сообщение с единственной кнопкой закрытия
     */
    @CommandMapping(value = "close_demo", type = CommandType.MESSAGE)
    public SendMessage closeDemo(Chat chat) {
        return SendMessage.builder()
                .chatId(chat.getId())
                .text("Нажми кнопку, чтобы библиотека сама убрала inline-клавиатуру:")
                .replyMarkup(Keyboards.inline()
                        .row().button("✖ Закрыть", Callback.close())
                        .build())
                .build();
    }
}
