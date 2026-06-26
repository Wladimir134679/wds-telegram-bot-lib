package ru.wds.smp.wdstelegrambotlib.command;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Разбирает текст входящего сообщения в {@link ParsedCommand}.
 *
 * <p>Учитывает замечания к старому разбору ({@code split(" ")}):</p>
 * <ul>
 *   <li>разбиение по <b>любым</b> пробельным символам (пробелы, табы, переводы
 *       строк) через regex {@code \\s+}, а не по одному пробелу;</li>
 *   <li>ведущий {@code /} отрезается;</li>
 *   <li>суффикс {@code @имя_бота} отрезается; если команда адресована другому боту
 *       (имя в суффиксе не совпадает с нашим), разбор возвращает
 *       {@link Optional#empty()};</li>
 *   <li>имя приводится к нижнему регистру для регистронезависимого сопоставления.</li>
 * </ul>
 *
 * <p>Класс потокобезопасен и неизменяем.</p>
 */
public class CommandParser {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final String botUsername;

    /**
     * @param botUsername username бота без {@code @} (для отрезания суффикса
     *                    {@code @имя_бота}); может быть {@code null}/пустым
     */
    public CommandParser(String botUsername) {
        this.botUsername = botUsername;
    }

    /**
     * Разбирает текст сообщения в команду.
     *
     * @param text текст входящего сообщения (может быть {@code null})
     * @return разобранная команда, либо {@link Optional#empty()}, если текст пуст
     *         или команда адресована другому боту
     */
    public Optional<ParsedCommand> parse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String trimmed = text.strip();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        String[] tokens = WHITESPACE.split(trimmed);
        String first = tokens[0];

        // Отрезаем ведущий '/'.
        if (first.startsWith("/")) {
            first = first.substring(1);
        }

        // Отрезаем суффикс @имя_бота и проверяем адресата.
        int at = first.indexOf('@');
        if (at >= 0) {
            String mention = first.substring(at + 1);
            first = first.substring(0, at);
            if (botUsername != null && !botUsername.isBlank()
                    && !mention.equalsIgnoreCase(botUsername)) {
                // Команда явно адресована другому боту — игнорируем.
                return Optional.empty();
            }
        }

        if (first.isEmpty()) {
            return Optional.empty();
        }

        String name = first.toLowerCase(Locale.ROOT);
        String rawArguments = trimmed.substring(tokens[0].length()).strip();
        List<String> arguments = tokens.length > 1
                ? Arrays.asList(Arrays.copyOfRange(tokens, 1, tokens.length))
                : List.of();

        return Optional.of(new ParsedCommand(name, rawArguments, arguments));
    }
}
