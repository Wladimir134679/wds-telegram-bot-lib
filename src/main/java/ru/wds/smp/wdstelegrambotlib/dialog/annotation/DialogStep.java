package ru.wds.smp.wdstelegrambotlib.dialog.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Шаг диалога: метод вызывается, когда диалог пользователя находится на шаге с
 * именем {@link #value()} и приходит очередное сообщение.
 *
 * <p>Внутри шага типичный сценарий: разобрать ввод пользователя
 * ({@code @Text String input}), сохранить его в контекст ({@code ctx.set("a", ...)})
 * и решить, что дальше — перейти на следующий шаг ({@code ctx.next("...")}),
 * завершить диалог ({@code ctx.finish()}) или, при невалидном вводе, остаться на
 * текущем шаге (не вызывать переход) и попросить повторить.</p>
 *
 * <p><b>Ветвление</b> выражается обычным {@code if/else} с разными
 * {@code ctx.next(...)} — отдельные методы-шаги вместо ветвей одного {@code switch}.
 * Например: после выбора валюты для биткоина запросить второе число
 * ({@code ctx.next("ask-b")}), а для доллара — сразу посчитать
 * ({@code ctx.finish()}).</p>
 *
 * <p><b>Ввод пользователя.</b> В шаге {@code @Text String} содержит весь текст
 * сообщения целиком (а не «хвост после имени команды», как у обычных команд) —
 * это и есть ответ пользователя на заданный вопрос.</p>
 *
 * <p>Имена шагов уникальны в пределах одного {@link Dialog}. Один метод может
 * обслуживать несколько шагов, если перечислить их в {@link #value()} (например,
 * единый обработчик со {@code switch} по {@code ctx.step()}).</p>
 *
 * @see Dialog
 * @see DialogStart
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DialogStep {

    /**
     * Имена шагов, которые обслуживает метод (минимум одно). Уникальны в пределах
     * диалога.
     *
     * @return имена шагов
     */
    String[] value();
}
