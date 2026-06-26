package ru.wds.smp.wdstelegrambotlib.command.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Инъектирует идентификатор чата текущего апдейта в параметр типа
 * {@code long}/{@link Long}.
 *
 * <p>Удобный «сахар», чтобы не доставать его через {@code Chat}/{@code Message}.
 * Работает одинаково в обычных командах, callback и шагах диалога — резолвер общий.
 * Если чат недоступен: для {@link Long} подставляется {@code null}, для примитива
 * {@code long} — {@code 0}.</p>
 *
 * <pre>{@code
 * @CommandMapping("start")
 * public void start(@ChatId long chatId, TelegramBotSender sender) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ChatId {
}
