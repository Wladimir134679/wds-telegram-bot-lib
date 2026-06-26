package ru.wds.smp.examplebot.controller;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import ru.wds.smp.examplebot.model.DemoProfile;
import ru.wds.smp.wdstelegrambotlib.command.CommandInvocation;
import ru.wds.smp.wdstelegrambotlib.command.CommandType;
import ru.wds.smp.wdstelegrambotlib.command.annotation.BotController;
import ru.wds.smp.wdstelegrambotlib.command.annotation.CommandMapping;
import ru.wds.smp.wdstelegrambotlib.command.annotation.Payload;
import ru.wds.smp.wdstelegrambotlib.command.callback.Callback;
import ru.wds.smp.wdstelegrambotlib.command.keyboard.Keyboards;

/**
 * Демонстрация «больших» данных в callback через {@link Payload}.
 *
 * <p>Сценарий: {@code callback_data} ограничен 64 байтами, поэтому объект целиком
 * туда не положить. Команда {@code /profile} сохраняет {@link DemoProfile} на
 * сервере ({@code ctx.save(...)}) и получает короткую ссылку, которую кладёт в
 * кнопку обычным параметром. Обработчик callback читает данные обратно через
 * {@code @Payload}. Если данные истекли по TTL, библиотека сообщит об этом
 * пользователю alert'ом (см. {@code required = true}).</p>
 */
@BotController
public class ProfileController {

    /**
     * {@code /profile}: сохраняет демонстрационный профиль и присылает кнопку,
     * раскрывающую его.
     *
     * @param user инициатор (для содержимого профиля)
     * @param chat чат-источник (адрес ответа)
     * @param ctx  контекст вызова — через него сохраняем данные ({@code ctx.save})
     * @return сообщение с inline-кнопкой, в которой лежит ссылка на данные
     */
    @CommandMapping({"profile", "🔢 Профиль"})
    public SendMessage profile(User user, Chat chat, CommandInvocation ctx) {
        DemoProfile demo = new DemoProfile(
                user.getId(),
                user.getFirstName(),
                "Это произвольные «большие» данные, которые не влезают в callback_data, "
                        + "поэтому хранятся на сервере и доступны по короткой ссылке.");

        // Сохраняем объект и получаем короткую ссылку (payloadId) для кнопки.
        String ref = ctx.save(demo);

        return SendMessage.builder()
                .chatId(chat.getId())
                .text("Профиль сохранён на сервере. Нажми кнопку, чтобы раскрыть данные:")
                .replyMarkup(Keyboards.inline()
                        .row().button("👤 Показать профиль", Callback.to("profile_show").arg("ref", ref))
                        .row().button("✖ Закрыть", Callback.close())
                        .build())
                .build();
    }

    /**
     * Callback {@code profile_show}: достаёт сохранённый профиль по ссылке и
     * показывает его.
     *
     * @param profile данные, восстановленные из хранилища по ссылке ({@link Payload},
     *                обязательны — при истёкшем TTL библиотека покажет alert)
     * @return текстовое представление профиля
     */
    @CommandMapping(value = "profile_show", type = CommandType.CALLBACK)
    public String show(@Payload(value = "ref", required = true) DemoProfile profile) {
        return """
                👤 Профиль
                id: %d
                имя: %s
                заметка: %s
                """.formatted(profile.userId(), profile.displayName(), profile.note());
    }
}
