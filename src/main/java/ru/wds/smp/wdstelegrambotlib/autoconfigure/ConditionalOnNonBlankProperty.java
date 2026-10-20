package ru.wds.smp.wdstelegrambotlib.autoconfigure;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Условие: свойство окружения задано и содержит непробельный текст.
 *
 * <p>Нужно вместо {@code @ConditionalOnProperty(name = "...")} без {@code havingValue}:
 * стандартное условие считает выполненным любое значение, кроме {@code "false"},
 * в том числе пустую строку. Типичный случай — {@code token: ${TELEGRAM_BOT_TOKEN:}}
 * при незаданной переменной окружения: свойство есть, но пустое, и бот стартовал бы
 * с пустым токеном. Это условие в такой ситуации не срабатывает.</p>
 *
 * @see OnNonBlankPropertyCondition
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@Conditional(OnNonBlankPropertyCondition.class)
@interface ConditionalOnNonBlankProperty {

    /**
     * Полное имя свойства, например {@code telegram.bot.token}.
     */
    String value();
}
