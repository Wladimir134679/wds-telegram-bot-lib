package ru.wds.smp.wdstelegrambotlib.command.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Помечает бин как «контроллер команд» Telegram-бота — аналог {@code @Controller}
 * из Spring MVC.
 *
 * <p>Класс, помеченный этой аннотацией, содержит методы-обработчики команд,
 * помеченные {@link CommandMapping}. Библиотека находит такие бины в контексте
 * потребителя и при старте строит карту «команда → метод».</p>
 *
 * <p>Аннотация мета-аннотирована {@link Component}, поэтому контроллер
 * подхватывается обычным сканированием компонентов приложения-потребителя.
 * Библиотека <b>не</b> навязывает собственный {@code @ComponentScan} — она лишь
 * читает уже зарегистрированные бины.</p>
 *
 * <p>Пример:</p>
 * <pre>{@code
 * @BotController
 * public class StartController {
 *
 *     @CommandMapping("/start")
 *     public String start(User user) {
 *         return "Привет, " + user.getFirstName() + "!";
 *     }
 * }
 * }</pre>
 *
 * @see CommandMapping
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface BotController {

    /**
     * Имя бина (необязательно), пробрасывается в мета-аннотацию {@link Component}.
     *
     * @return имя бина или пустая строка для имени по умолчанию
     */
    @AliasFor(annotation = Component.class)
    String value() default "";
}
