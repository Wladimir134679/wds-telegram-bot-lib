package ru.wds.smp.wdstelegrambotlib.dialog.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Объявляет бин диалог-машиной (пошаговый сценарий, «мастер»).
 *
 * <p>Диалог — это конечный автомат: одна команда-триггер запускает его (метод
 * {@link DialogStart}), а последующие сообщения пользователя попадают в текущий
 * шаг ({@link DialogStep}). Между шагами переходят явно — через
 * {@code ctx.next("имя-шага")}; накопленные данные хранятся в
 * {@link ru.wds.smp.wdstelegrambotlib.dialog.DialogContext}. Это библиотечный
 * аналог «формы-визарда»: запросить число A, затем B, при необходимости ветвиться
 * и в конце показать результат.</p>
 *
 * <p>Аннотация — мета-{@link Component}: помеченный класс автоматически становится
 * Spring-бином, дополнительная регистрация не нужна. Один и тот же класс может быть
 * и {@code @Dialog}, и обычным контроллером команд, но обычно диалог выделяют
 * отдельно для читаемости.</p>
 *
 * <p><b>Триггеры.</b> {@link #value()} — имена команд, запускающих диалог (как
 * {@link ru.wds.smp.wdstelegrambotlib.command.annotation.CommandMapping}: ведущий
 * {@code /} необязателен, сопоставление регистронезависимое, поддерживаются подписи
 * reply-кнопок). Первое имя дополнительно служит ключом диалога. Если триггер
 * совпадает с именем обычной {@code @CommandMapping}-команды, выигрывает обычная
 * команда (она маршрутизируется раньше) — не дублируйте имена.</p>
 *
 * @see DialogStart
 * @see DialogStep
 * @see ru.wds.smp.wdstelegrambotlib.dialog.DialogContext
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Dialog {

    /**
     * Имена команд-триггеров, запускающих диалог (минимум одно). Первое имя
     * используется как уникальный ключ диалога в реестре. Ведущий {@code /}
     * необязателен.
     *
     * @return имена-триггеры
     */
    String[] value();
}
