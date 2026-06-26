package ru.wds.smp.wdstelegrambotlib.dialog.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Точка входа в диалог: метод вызывается, когда пользователь набирает одну из
 * команд-триггеров {@link Dialog}.
 *
 * <p>Метод обычно приветствует пользователя и задаёт первый вопрос, после чего
 * переводит автомат на первый шаг через {@code ctx.next("...")}. Если метод не
 * вызвал {@code next}/{@code finish}, диалог считается запущенным, но без активного
 * шага — следующее сообщение пользователя будет проигнорировано слоем диалогов;
 * как правило, в {@code @DialogStart} всегда указывают переход.</p>
 *
 * <p><b>Сигнатура.</b> Аргументы резолвятся той же машинерией, что и у обычных
 * команд: можно объявлять {@link ru.wds.smp.wdstelegrambotlib.dialog.DialogContext},
 * {@code @Text String}, {@code Message}, {@code Chat}, {@code User},
 * {@code TelegramBotSender} и т.д. Возвращаемое значение интерпретируется как ответ
 * пользователю (текст, {@code SendMessage} и пр.), как и у команд.</p>
 *
 * <p>В одном {@link Dialog} должен быть ровно один метод {@code @DialogStart}.</p>
 *
 * @see Dialog
 * @see DialogStep
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DialogStart {
}
