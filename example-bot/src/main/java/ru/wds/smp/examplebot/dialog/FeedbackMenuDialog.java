package ru.wds.smp.examplebot.dialog;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.wds.smp.wdstelegrambotlib.command.annotation.Text;
import ru.wds.smp.wdstelegrambotlib.command.callback.Callback;
import ru.wds.smp.wdstelegrambotlib.command.keyboard.Keyboards;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogContext;
import ru.wds.smp.wdstelegrambotlib.dialog.annotation.Dialog;
import ru.wds.smp.wdstelegrambotlib.dialog.annotation.DialogCallback;
import ru.wds.smp.wdstelegrambotlib.dialog.annotation.DialogStart;
import ru.wds.smp.wdstelegrambotlib.dialog.annotation.DialogStep;

/**
 * Главный демонстрационный сценарий: меню в одном сообщении, где callback-кнопки и
 * текстовый ввод работают как единый диалог.
 *
 * <p>Сценарий из обсуждения: бот показывает меню «что отредактировать» (имя/телефон/
 * адрес) в одном сообщении, которое <b>редактируется на месте</b>. Нажатие кнопки
 * ({@code @DialogCallback}) переводит диалог в ожидание текста ({@code ctx.next}),
 * текстовый шаг ({@code @DialogStep}) сохраняет значение и перерисовывает то же меню
 * ({@code ctx.edit}). Кнопка «Закончить» завершает диалог и показывает сводку.</p>
 *
 * <p>Состояние (введённые значения) живёт в {@link DialogContext}, само сообщение —
 * «якорь», его id библиотека запоминает автоматически, поэтому и callback, и
 * текстовые шаги редактируют одно и то же сообщение.</p>
 */
@Dialog({"feedback", "📝 Анкета"})
public class FeedbackMenuDialog {

    @DialogStart
    public SendMessage start(DialogContext ctx) {
        return SendMessage.builder()
                .chatId(ctx.chatId())
                .text(menuText(ctx))
                .replyMarkup(menuKeyboard())
                .build();
    }

    // --- выбор поля кнопкой: переходим к ожиданию текста, меню превращаем в приглашение ---

    @DialogCallback("edit-name")
    public EditMessageText editName(DialogContext ctx) {
        ctx.next("await-name");
        return ctx.edit("Введите имя:", null);
    }

    @DialogCallback("edit-phone")
    public EditMessageText editPhone(DialogContext ctx) {
        ctx.next("await-phone");
        return ctx.edit("Введите номер телефона:", null);
    }

    @DialogCallback("edit-address")
    public EditMessageText editAddress(DialogContext ctx) {
        ctx.next("await-address");
        return ctx.edit("Введите адрес:", null);
    }

    // --- текстовый ввод: сохраняем и возвращаем то же меню (редактируем якорь) ---

    @DialogStep("await-name")
    public EditMessageText onName(@Text String name, DialogContext ctx) {
        ctx.set("name", name.strip());
        return ctx.edit(menuText(ctx), menuKeyboard());
    }

    @DialogStep("await-phone")
    public EditMessageText onPhone(@Text String phone, DialogContext ctx) {
        ctx.set("phone", phone.strip());
        return ctx.edit(menuText(ctx), menuKeyboard());
    }

    @DialogStep("await-address")
    public EditMessageText onAddress(@Text String address, DialogContext ctx) {
        ctx.set("address", address.strip());
        return ctx.edit(menuText(ctx), menuKeyboard());
    }

    // --- завершение: убираем клавиатуру, показываем сводку ---

    @DialogCallback("finish")
    public EditMessageText finish(DialogContext ctx) {
        ctx.finish();
        // Пустая inline-разметка убирает клавиатуру (null — оставил бы кнопки).
        return ctx.edit("""
                ✅ Спасибо за обратную связь! Собраны данные:
                • Имя: %s
                • Телефон: %s
                • Адрес: %s""".formatted(value(ctx, "name"), value(ctx, "phone"), value(ctx, "address")),
                Keyboards.inline().build());
    }

    private String menuText(DialogContext ctx) {
        return """
                📝 Анкета. Что отредактировать?
                • Имя: %s
                • Телефон: %s
                • Адрес: %s

                Нажмите кнопку, затем введите значение.""".formatted(
                value(ctx, "name"), value(ctx, "phone"), value(ctx, "address"));
    }

    private InlineKeyboardMarkup menuKeyboard() {
        return Keyboards.inline()
                .row().button("Имя", Callback.to("edit-name")).button("Телефон", Callback.to("edit-phone"))
                .row().button("Адрес", Callback.to("edit-address"))
                .row().button("✅ Закончить", Callback.to("finish"))
                .build();
    }

    private String value(DialogContext ctx, String key) {
        String v = ctx.get(key);
        return v != null ? v : "—";
    }
}
