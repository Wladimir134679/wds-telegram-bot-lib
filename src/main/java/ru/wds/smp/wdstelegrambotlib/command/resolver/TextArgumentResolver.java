package ru.wds.smp.wdstelegrambotlib.command.resolver;

import ru.wds.smp.wdstelegrambotlib.command.CommandInvocation;
import ru.wds.smp.wdstelegrambotlib.command.ParsedCommand;
import ru.wds.smp.wdstelegrambotlib.command.annotation.Text;

import java.lang.reflect.Parameter;

/**
 * Резолвер параметра, помеченного {@link Text}: подставляет «хвост» текстовой
 * команды — всё после её имени (тип {@link String}).
 */
public class TextArgumentResolver implements CommandArgumentResolver {

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(Text.class) && parameter.getType() == String.class;
    }

    @Override
    public Object resolve(Parameter parameter, CommandInvocation invocation) {
        ParsedCommand command = invocation.getCommand();
        return command != null ? command.rawArguments() : "";
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
