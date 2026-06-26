package ru.wds.smp.examplebot.controller;

import ru.wds.smp.wdstelegrambotlib.command.CommandInvocation;
import ru.wds.smp.wdstelegrambotlib.command.annotation.BotController;
import ru.wds.smp.wdstelegrambotlib.command.annotation.ChatId;
import ru.wds.smp.wdstelegrambotlib.command.annotation.CommandMapping;
import ru.wds.smp.wdstelegrambotlib.command.annotation.UserId;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogManager;

/**
 * Демонстрация «команда, которая продолжилась»: обычная {@code @CommandMapping}
 * программно запускает диалог через {@link DialogManager}, а также показ
 * {@code @ChatId}/{@code @UserId}.
 *
 * <p>{@code DialogManager} — бин библиотеки, доступный при включённых диалогах
 * ({@code telegram.bot.dialog.enabled=true}).</p>
 */
@BotController
public class DialogLauncherController {

    private final DialogManager dialogManager;

    public DialogLauncherController(DialogManager dialogManager) {
        this.dialogManager = dialogManager;
    }

    /**
     * {@code /calc2}: не отвечает сам, а запускает диалог-калькулятор программно —
     * как будто пользователь набрал команду-триггер. Первое сообщение пришлёт сам
     * {@code @DialogStart}.
     *
     * @param ctx контекст вызова (передаём в менеджер — из него берутся чат и пользователь)
     * @return {@code null} — ответ отправит старт диалога
     */
    @CommandMapping("calc2")
    public Object calcViaCommand(CommandInvocation ctx) {
        dialogManager.start(ctx, "calc");
        return null;
    }

    /**
     * {@code /whoami}: показывает {@code @ChatId}/{@code @UserId} — id извлекаются
     * общими резолверами (работают и в командах, и в шагах диалога).
     *
     * @param chatId идентификатор чата ({@code @ChatId})
     * @param userId идентификатор пользователя ({@code @UserId})
     * @return текст с идентификаторами
     */
    @CommandMapping("whoami")
    public String whoami(@ChatId long chatId, @UserId long userId) {
        return "chatId=%d, userId=%d".formatted(chatId, userId);
    }
}
