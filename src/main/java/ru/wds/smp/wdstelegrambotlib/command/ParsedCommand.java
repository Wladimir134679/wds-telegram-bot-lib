package ru.wds.smp.wdstelegrambotlib.command;

import java.util.List;

/**
 * Результат разбора входящей команды.
 *
 * @param name          нормализованное имя команды (без ведущего {@code /}, в нижнем
 *                      регистре, без суффикса {@code @имя_бота})
 * @param rawArguments  «сырая» строка аргументов — всё после имени команды
 * @param arguments     позиционные аргументы, разбитые по пробельным символам
 *                      (неизменяемый список)
 */
public record ParsedCommand(String name, String rawArguments, List<String> arguments) {

    public ParsedCommand {
        arguments = List.copyOf(arguments);
    }
}
