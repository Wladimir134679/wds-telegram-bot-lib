package ru.wds.smp.wdstelegrambotlib.command.resolver;

import org.springframework.core.Ordered;
import ru.wds.smp.wdstelegrambotlib.command.CommandInvocation;

import java.lang.reflect.Parameter;

/**
 * Резолвер одного параметра метода-команды — точка расширения инъекции аргументов
 * в духе Spring MVC.
 *
 * <p>Каждый параметр метода-команды независимо резолвится подходящим резолвером, что
 * даёт читаемые сигнатуры: метод объявляет только нужные ему параметры в любом
 * порядке.</p>
 *
 * <p><b>Кэширование.</b> Подбор резолвера ({@link #supports(Parameter)}) выполняется
 * один раз при регистрации команды, а не на каждый вызов.</p>
 *
 * <p>Потребитель может добавить свой резолвер, зарегистрировав бин этого типа;
 * порядок задаётся через {@link Ordered}/{@code @Order} (меньше — раньше).</p>
 */
public interface CommandArgumentResolver extends Ordered {

    /**
     * Может ли резолвер обработать данный параметр.
     *
     * @param parameter параметр метода-команды
     * @return {@code true}, если резолвер умеет вычислить значение параметра
     */
    boolean supports(Parameter parameter);

    /**
     * Вычисляет значение параметра для конкретного вызова команды.
     *
     * @param parameter  параметр метода-команды
     * @param invocation контекст текущего вызова
     * @return значение для подстановки в аргумент метода
     */
    Object resolve(Parameter parameter, CommandInvocation invocation);

    @Override
    default int getOrder() {
        return 0;
    }
}
