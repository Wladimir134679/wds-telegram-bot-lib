package ru.wds.smp.wdstelegrambotlib.command;

import ru.wds.smp.wdstelegrambotlib.command.resolver.CommandArgumentResolver;

import java.lang.reflect.Parameter;

/**
 * Предрассчитанная привязка одного параметра метода-команды к резолверу.
 *
 * <p>Вычисляется один раз при регистрации команды и переиспользуется на каждом
 * вызове — ключевая оптимизация против сканирования рефлексией на каждый вызов.</p>
 *
 * @param parameter параметр метода
 * @param resolver  резолвер, выбранный для этого параметра
 */
public record ParameterBinding(Parameter parameter, CommandArgumentResolver resolver) {

    /**
     * Вычисляет значение параметра для конкретного вызова.
     *
     * @param invocation контекст вызова
     * @return значение для подстановки в аргумент метода
     */
    public Object resolve(CommandInvocation invocation) {
        return resolver.resolve(parameter, invocation);
    }
}
