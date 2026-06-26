package ru.wds.smp.wdstelegrambotlib.command.annotation;

import ru.wds.smp.wdstelegrambotlib.command.CommandType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Связывает метод контроллера с одной или несколькими командами — аналог
 * {@code @RequestMapping} из Spring MVC.
 *
 * <p>Метод вызывается, когда от пользователя приходит команда с одним из указанных
 * имён. Аргументы метода резолвятся по типу и аннотациям (см.
 * {@link ru.wds.smp.wdstelegrambotlib.command.resolver.CommandArgumentResolver}),
 * а возвращаемое значение интерпретируется как ответ пользователю
 * (см. {@link ru.wds.smp.wdstelegrambotlib.command.CommandReturnValueHandler}).</p>
 *
 * <p><b>Нормализация имён.</b> Ведущий {@code /} необязателен и отрезается:
 * {@code @CommandMapping("/start")} и {@code @CommandMapping("start")} эквивалентны.
 * Сопоставление имён <b>регистронезависимое</b>. Суффикс {@code @имя_бота}
 * у команды (как в группах) отрезается при разборе.</p>
 *
 * <p><b>Конфликты.</b> Если две команды одного типа претендуют на одно имя,
 * библиотека падает при старте с понятной ошибкой — детерминированное поведение
 * вместо «молчаливой перезаписи» из старого проекта.</p>
 *
 * @see BotController
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CommandMapping {

    /**
     * Имена команд (алиасы), на которые откликается метод. Минимум одно.
     * Ведущий {@code /} необязателен.
     *
     * @return имена команд
     */
    String[] value();

    /**
     * Тип команды: текстовое сообщение или callback-кнопка.
     *
     * @return тип команды; по умолчанию {@link CommandType#MESSAGE}
     */
    CommandType type() default CommandType.MESSAGE;
}
