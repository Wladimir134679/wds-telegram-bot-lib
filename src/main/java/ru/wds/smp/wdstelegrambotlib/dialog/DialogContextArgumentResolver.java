package ru.wds.smp.wdstelegrambotlib.dialog;

import ru.wds.smp.wdstelegrambotlib.command.CommandInvocation;
import ru.wds.smp.wdstelegrambotlib.command.resolver.CommandArgumentResolver;

import java.lang.reflect.Parameter;

/**
 * Резолвер параметра типа {@link DialogContext}: подставляет контекст текущего
 * диалога из {@link CommandInvocation}.
 *
 * <p>Регистрируется как обычный {@link CommandArgumentResolver}, поэтому работает в
 * единой машинерии инъекции аргументов вместе с {@code @Text}, контекстными типами
 * и т.д. Для обычных команд и callback контекст диалога равен {@code null}.</p>
 */
public class DialogContextArgumentResolver implements CommandArgumentResolver {

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.getType() == DialogContext.class;
    }

    @Override
    public Object resolve(Parameter parameter, CommandInvocation invocation) {
        return invocation.getDialogContext();
    }

    @Override
    public int getOrder() {
        // Чуть раньше общего контекстного резолвера (200); тип уникален, пересечений нет.
        return 150;
    }
}
