package ru.wds.smp.wdstelegrambotlib.command;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.wds.smp.wdstelegrambotlib.command.callback.CallbackPayloadStore;
import ru.wds.smp.wdstelegrambotlib.core.TelegramBotSender;
import ru.wds.smp.wdstelegrambotlib.handler.HandlerPriority;
import ru.wds.smp.wdstelegrambotlib.handler.UpdateHandler;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Звено цепочки, выполняющее маршрутизацию текстовых команд — финальный этап
 * обработки апдейта (приоритет {@link HandlerPriority#COMMAND_PROCESSING}).
 *
 * <p>Сопоставление в два шага:</p>
 * <ol>
 *   <li><b>Точное совпадение всего текста</b> с именем команды — так работают
 *       reply-кнопки: подпись «Каталог» матчит {@code @CommandMapping("Каталог")};</li>
 *   <li>иначе — классический разбор {@code /команда хвост}: первый токен как имя
 *       команды, остаток как {@code @Text}.</li>
 * </ol>
 *
 * <p>Не прерывает цепочку. Ошибки аргументов ({@link CommandArgumentException})
 * обрабатываются мягко.</p>
 */
@Slf4j
public class CommandUpdateHandler implements UpdateHandler {

    private final CommandRegistry registry;
    private final CommandParser parser;
    private final CommandReturnValueHandler returnValueHandler;
    private final CallbackPayloadStore payloadStore;

    public CommandUpdateHandler(CommandRegistry registry, CommandParser parser,
                                CommandReturnValueHandler returnValueHandler,
                                CallbackPayloadStore payloadStore) {
        this.registry = registry;
        this.parser = parser;
        this.returnValueHandler = returnValueHandler;
        this.payloadStore = payloadStore;
    }

    @Override
    public boolean handle(TelegramBotSender sender, Update update) {
        if (!update.hasMessage()) {
            return true;
        }
        Message message = update.getMessage();
        String text = message.getText();
        if (text == null || text.isBlank()) {
            return true;
        }

        CommandDefinition definition;
        ParsedCommand parsed;

        // Шаг 1: точное совпадение всего текста (reply-кнопки, команды без аргументов).
        String fullName = normalize(text);
        definition = registry.find(fullName, CommandType.MESSAGE);
        if (definition != null) {
            parsed = new ParsedCommand(fullName, "", List.of());
        } else {
            // Шаг 2: классический разбор «/команда хвост».
            Optional<ParsedCommand> parsedOpt = parser.parse(text);
            if (parsedOpt.isEmpty()) {
                return true;
            }
            parsed = parsedOpt.get();
            definition = registry.find(parsed.name(), CommandType.MESSAGE);
            if (definition == null) {
                return true; // команда не найдена — пропускаем дальше
            }
        }

        CommandInvocation invocation = CommandInvocation.fromMessage(update, sender, parsed, payloadStore);
        try {
            Object result = definition.invoke(invocation);
            returnValueHandler.handle(result, invocation);
        } catch (CommandArgumentException e) {
            log.debug("Ошибка аргументов команды '{}': {}", parsed.name(), e.getMessage());
            if (invocation.getChatId() != null) {
                sender.executeQuietly(SendMessage.builder()
                        .chatId(invocation.getChatId())
                        .text("⚠️ " + e.getMessage())
                        .build());
            }
        }
        return true;
    }

    @Override
    public int getOrder() {
        return HandlerPriority.COMMAND_PROCESSING;
    }

    private String normalize(String text) {
        String name = text.strip();
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        return name.toLowerCase(Locale.ROOT);
    }
}
