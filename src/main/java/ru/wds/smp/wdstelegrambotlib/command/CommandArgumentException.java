package ru.wds.smp.wdstelegrambotlib.command;

/**
 * Ошибка разбора/преобразования аргумента команды: отсутствует обязательный
 * аргумент, либо значение нельзя привести к типу параметра (например, нечисловой
 * текст в {@code Long}).
 *
 * <p>Это «пользовательская» по природе ошибка (некорректный ввод), поэтому её
 * разумно перехватывать и отвечать понятным сообщением, а не считать сбоем бота.</p>
 */
public class CommandArgumentException extends RuntimeException {

    public CommandArgumentException(String message) {
        super(message);
    }

    public CommandArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
