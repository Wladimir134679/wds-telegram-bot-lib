package ru.wds.smp.wdstelegrambotlib.command.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Привязывает параметр <b>текстовой</b> команды к «хвосту» — всему тексту после
 * имени команды.
 *
 * <p>Применяется к параметру типа {@link String}. Для команды {@code /say привет
 * всем} в метод придёт {@code "привет всем"} целиком. Это намеренно простая модель:
 * у текстовой команды есть только её имя и остаток строки. Разбор остатка на
 * отдельные числовые/перечислимые аргументы — отдельная задача и здесь не делается.</p>
 *
 * <pre>{@code
 * @CommandMapping("/say")
 * public String say(@Text String text) {
 *     return text;
 * }
 * }</pre>
 *
 * <p>В callback-командах {@code @Text} не используется — там параметры именованные
 * ({@link Param}/{@link Payload}).</p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Text {
}
