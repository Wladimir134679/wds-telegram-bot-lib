package ru.wds.smp.wdstelegrambotlib.command.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Инъектирует идентификатор пользователя-инициатора текущего апдейта в параметр
 * типа {@code long}/{@link Long}.
 *
 * <p>«Сахар» поверх {@code User#getId()}. Работает одинаково в командах, callback и
 * шагах диалога — резолвер общий. Если пользователь недоступен: для {@link Long}
 * подставляется {@code null}, для примитива {@code long} — {@code 0}.</p>
 *
 * <pre>{@code
 * @DialogStep("ask-a")
 * public String a(@Text String in, @UserId long userId, DialogContext ctx) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UserId {
}
