package ru.wds.smp.wdstelegrambotlib.command;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.wds.smp.wdstelegrambotlib.command.callback.CallbackPayloadStore;
import ru.wds.smp.wdstelegrambotlib.core.TelegramBotSender;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogKey;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogStateStore;
import ru.wds.smp.wdstelegrambotlib.handler.HandlerPriority;
import ru.wds.smp.wdstelegrambotlib.handler.UpdateHandler;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Звено цепочки, выполняющее маршрутизацию текстовых команд
 * (приоритет {@link HandlerPriority#COMMAND_PROCESSING}). Выполняется перед слоем
 * диалогов и заглушкой — совпавшая команда «побеждает» их и прерывает цепочку.
 *
 * <p>Сопоставление в два шага:</p>
 * <ol>
 *   <li><b>Точное совпадение всего текста</b> с именем команды — так работают
 *       reply-кнопки: подпись «Каталог» матчит {@code @CommandMapping("Каталог")};</li>
 *   <li>иначе — классический разбор {@code /команда хвост}: первый токен как имя
 *       команды, остаток как {@code @Text}.</li>
 * </ol>
 *
 * <p><b>Прерывание цепочки.</b> Если команда совпала и была вызвана, звено
 * возвращает {@code false} — апдейт считается обработанным и дальше (в слой
 * диалогов и заглушку) не передаётся. Если команда не найдена — возвращает
 * {@code true}, пропуская апдейт следующим звеньям. Ошибки аргументов
 * ({@link CommandArgumentException}) обрабатываются мягко, но команда всё равно
 * считается обработанной.</p>
 *
 * <p><b>Взаимодействие с диалогами.</b> Совпавшая обычная команда «побеждает»
 * активный диалог пользователя: его состояние сбрасывается через
 * {@link DialogStateStore}. Когда слой диалогов выключен, в контексте — no-op стор,
 * поэтому сброс безвреден.</p>
 */
@Slf4j
public class CommandUpdateHandler implements UpdateHandler {

    private final CommandRegistry registry;
    private final CommandParser parser;
    private final CommandReturnValueHandler returnValueHandler;
    private final CallbackPayloadStore payloadStore;
    private final DialogStateStore dialogStore;

    public CommandUpdateHandler(CommandRegistry registry, CommandParser parser,
                                CommandReturnValueHandler returnValueHandler,
                                CallbackPayloadStore payloadStore,
                                DialogStateStore dialogStore) {
        this.registry = registry;
        this.parser = parser;
        this.returnValueHandler = returnValueHandler;
        this.payloadStore = payloadStore;
        this.dialogStore = dialogStore;
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

        // Обычная команда совпала — она «побеждает» активный диалог пользователя.
        clearDialog(message);

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
        } catch (Exception e) {
            // Команда совпала, но её код упал: апдейт уже «поглощён» — логируем и не
            // пробрасываем ошибку дальше (в диалоги/заглушку).
            log.error("Команда '{}' завершилась с ошибкой", parsed.name(), e);
        }
        // Команда обработана — апдейт не передаём дальше (в диалоги/заглушку).
        return false;
    }

    /** Сбрасывает активный диалог пользователя, приславшего команду (если он есть). */
    private void clearDialog(Message message) {
        Chat chat = message.getChat();
        User from = message.getFrom();
        if (chat != null && chat.getId() != null && from != null && from.getId() != null) {
            dialogStore.remove(new DialogKey(chat.getId(), from.getId()));
        }
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
