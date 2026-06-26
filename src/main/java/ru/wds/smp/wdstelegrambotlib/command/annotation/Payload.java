package ru.wds.smp.wdstelegrambotlib.command.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Привязывает параметр callback-команды к «большим» данным из
 * {@link ru.wds.smp.wdstelegrambotlib.command.callback.CallbackPayloadStore}.
 *
 * <p>Работает как {@link Param}, но интерпретирует значение именованного параметра
 * кнопки как <b>ссылку</b> ({@code payloadId}): библиотека достаёт по ней объект из
 * хранилища и приводит к типу параметра. Ссылку разработчик получает заранее через
 * {@code ctx.save(данные)} и кладёт в кнопку как обычный параметр.</p>
 *
 * <pre>{@code
 * // в команде, строящей клавиатуру:
 * String ref = ctx.save(new ChatAccessRules(...));   // большие данные → ссылка
 * Callback.to("connect").arg("rules", ref)
 *
 * // обработчик callback:
 * @CommandMapping(value = "connect", type = CommandType.CALLBACK)
 * public SendMessage connect(@Payload("rules") ChatAccessRules rules) { ... }
 * }</pre>
 *
 * <p>Если данные истекли по TTL или не найдены: при {@code required = true}
 * выбрасывается
 * {@link ru.wds.smp.wdstelegrambotlib.command.callback.CallbackPayloadExpiredException}
 * (пользователю мягко сообщается, что данные устарели), иначе подставляется {@code null}.</p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Payload {

    /**
     * Имя параметра кнопки, в котором лежит ссылка на данные.
     *
     * @return имя параметра (как в {@code .arg("имя", ref)})
     */
    String value();

    /**
     * Обязательны ли данные. Если {@code true} и данные истекли/не найдены —
     * бросается {@link ru.wds.smp.wdstelegrambotlib.command.callback.CallbackPayloadExpiredException}.
     *
     * @return {@code true}, если данные обязательны
     */
    boolean required() default false;
}
