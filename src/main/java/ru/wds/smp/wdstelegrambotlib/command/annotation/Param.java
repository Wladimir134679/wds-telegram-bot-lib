package ru.wds.smp.wdstelegrambotlib.command.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Привязывает параметр <b>callback</b>-команды к именованному параметру кнопки.
 *
 * <p>Имя должно совпадать с тем, что задано при сборке кнопки через
 * {@code Callback.to("...").arg("имя", значение)}. Это устраняет «магические»
 * позиционные индексы: видно, откуда берётся значение.</p>
 *
 * <p>Поддерживаемые типы: {@link String}, {@code long}/{@link Long},
 * {@code int}/{@link Integer}, {@code double}/{@link Double},
 * {@code boolean}/{@link Boolean}, а также {@code enum} (по имени константы).</p>
 *
 * <pre>{@code
 * // сборка кнопки:
 * Callback.to("nav").arg("page", 3)
 *
 * // обработчик:
 * @CommandMapping(value = "nav", type = CommandType.CALLBACK)
 * public EditMessageText nav(@Param("page") int page) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {

    /**
     * Имя параметра кнопки.
     *
     * @return имя (как в {@code .arg("имя", ...)})
     */
    String value();

    /**
     * Обязателен ли параметр. Если {@code true} и параметр отсутствует —
     * выбрасывается {@link ru.wds.smp.wdstelegrambotlib.command.CommandArgumentException}.
     * Если {@code false} — подставляется {@code null} (или ноль для примитивов).
     *
     * @return {@code true}, если параметр обязателен
     */
    boolean required() default false;
}
