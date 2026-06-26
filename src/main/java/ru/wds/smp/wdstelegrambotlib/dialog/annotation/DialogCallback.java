package ru.wds.smp.wdstelegrambotlib.dialog.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Обработчик нажатия inline-кнопки <b>внутри активного диалога</b> — стейтфул-аналог
 * {@code @CommandMapping(type = CALLBACK)}.
 *
 * <p>Если у пользователя активен диалог и пришёл callback, данные которого
 * (имя действия из {@code Callback.to("...")}) совпали с {@link #value()} этого
 * диалога, вызывается данный метод. В отличие от текстового {@code @DialogStep}
 * (он матчится по «текущему шагу»), callback матчится по <b>имени действия</b> —
 * потому что клавиатура предлагает несколько кнопок одновременно, и каждая кнопка
 * это своя ветка.</p>
 *
 * <p>Внутри обработчика типично: перерисовать «якорное» сообщение
 * ({@code return ctx.edit(text, keyboard)} — навигация по меню), либо перейти к
 * ожиданию текста ({@code ctx.next("await-...")}), либо завершить
 * ({@code ctx.finish()}). Аргументы резолвятся общей машинерией: доступны
 * {@code DialogContext}, {@code CallbackQuery}, {@code Message}, {@code User},
 * {@code @Param}/{@code @Payload} (значения из кнопки), {@code @ChatId}/{@code @UserId},
 * {@code TelegramBotSender} и т.д. Возвращаемое значение трактуется как ответ
 * (обычно {@code EditMessageText}).</p>
 *
 * <p><b>Приоритет.</b> Диалоговые callback'и проверяются, пока диалог активен,
 * раньше глобальных {@code @CommandMapping(CALLBACK)}; если действие не совпало —
 * управление уходит глобальным callback-командам. Системный
 * {@code Callback.close()} обрабатывается ещё раньше.</p>
 *
 * @see DialogStep
 * @see ru.wds.smp.wdstelegrambotlib.dialog.DialogContext#edit(String,
 *      org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DialogCallback {

    /**
     * Имена действий (как в {@code Callback.to("имя")}), которые обслуживает метод.
     * Минимум одно. Уникальны в пределах диалога.
     *
     * @return имена действий callback
     */
    String[] value();
}
