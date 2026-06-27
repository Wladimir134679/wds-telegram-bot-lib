package ru.wds.smp.wdstelegrambotlib.command;

import lombok.Builder;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.wds.smp.wdstelegrambotlib.command.callback.Callback;
import ru.wds.smp.wdstelegrambotlib.command.callback.CallbackPayloadStore;
import ru.wds.smp.wdstelegrambotlib.core.TelegramBotSender;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogContext;

/**
 * Контекст одного вызова команды: всё, что нужно резолверам аргументов и
 * обработчику возвращаемого значения.
 *
 * <p>Иммутабельный объект (создаётся билдером на каждый апдейт). Поля, неприменимые
 * к текущему типу команды, равны {@code null}: для текстовой команды это
 * {@link #callbackQuery}/{@link #callback}, для callback — {@link #command}.</p>
 *
 * <p>Через {@link #save(Object)} команда может положить «большие» данные в
 * {@link CallbackPayloadStore} и получить короткую ссылку, не обращаясь к хранилищу
 * напрямую — это и есть «специальная функция сохранить что угодно».</p>
 */
@Getter
@Builder
public class CommandInvocation {

    /** Исходный апдейт целиком. */
    private final Update update;

    /** Сообщение (для команд {@link CommandType#MESSAGE}; для callback — если доступно). */
    private final Message message;

    /** Callback-запрос (для команд {@link CommandType#CALLBACK}); {@code null} для текстовых. */
    private final CallbackQuery callbackQuery;

    /** Разобранные данные нажатой кнопки (для callback); {@code null} для текстовых. */
    private final Callback callback;

    /** Чат, в котором пришла команда. */
    private final Chat chat;

    /** Пользователь-инициатор команды. */
    private final User user;

    /** Абстракция отправки для ответа пользователю. */
    private final TelegramBotSender sender;

    /** Разобранная текстовая команда (имя + хвост); {@code null} для callback. */
    private final ParsedCommand command;

    /** Идентификатор чата для ответа (может быть {@code null}, если чат недоступен). */
    private final Long chatId;

    /** Идентификатор сообщения-источника (для callback — сообщение с кнопкой); может быть {@code null}. */
    private final Integer messageId;

    /** Хранилище «больших» данных (для {@link #save(Object)}). */
    private final CallbackPayloadStore payloadStore;

    /**
     * Контекст диалога (для вызовов методов {@code @DialogStart}/{@code @DialogStep});
     * {@code null} для обычных команд и callback. Резолвится в параметры типа
     * {@link DialogContext}.
     */
    private final DialogContext dialogContext;

    /**
     * Сохраняет «большие» данные в хранилище и возвращает короткую ссылку, которую
     * можно положить в кнопку как параметр ({@code .arg("name", ref)}) и прочитать в
     * callback через {@code @Payload("name")}.
     *
     * @param data произвольные данные (не {@code null})
     * @return ссылка ({@code payloadId})
     */
    public String save(Object data) {
        if (payloadStore == null) {
            throw new IllegalStateException("Хранилище callback-данных недоступно");
        }
        return payloadStore.save(data);
    }

    /**
     * Собирает контекст для текстовой команды из апдейта-сообщения.
     *
     * @param update  апдейт, у которого {@code hasMessage()} истинно
     * @param sender  абстракция отправки
     * @param command разобранная команда
     * @param store   хранилище «больших» данных
     * @return контекст вызова
     */
    public static CommandInvocation fromMessage(Update update, TelegramBotSender sender,
                                                ParsedCommand command, CallbackPayloadStore store) {
        Message message = update.getMessage();
        Chat chat = message.getChat();
        return CommandInvocation.builder()
                .update(update)
                .message(message)
                .chat(chat)
                .user(message.getFrom())
                .sender(sender)
                .command(command)
                .chatId(chat != null ? chat.getId() : null)
                .messageId(message.getMessageId())
                .payloadStore(store)
                .build();
    }

    /**
     * Собирает контекст для вызова шага диалога из апдейта-сообщения.
     *
     * <p>В отличие от {@link #fromMessage}, {@code command} здесь синтетический: его
     * {@code rawArguments} равны всему тексту сообщения, поэтому {@code @Text String}
     * в шаге получает ввод пользователя целиком (а не «хвост после имени команды»).
     * Дополнительно проставляется {@link #dialogContext}.</p>
     *
     * @param update        апдейт-сообщение
     * @param sender        абстракция отправки
     * @param command       синтетическая команда с текстом сообщения как аргументами
     * @param store         хранилище «больших» данных
     * @param dialogContext контекст диалога текущего пользователя
     * @return контекст вызова шага
     */
    public static CommandInvocation fromDialog(Update update, TelegramBotSender sender,
                                               ParsedCommand command, CallbackPayloadStore store,
                                               DialogContext dialogContext) {
        Message message = update.getMessage();
        Chat chat = message.getChat();
        return CommandInvocation.builder()
                .update(update)
                .message(message)
                .chat(chat)
                .user(message.getFrom())
                .sender(sender)
                .command(command)
                .chatId(chat != null ? chat.getId() : null)
                .messageId(message.getMessageId())
                .payloadStore(store)
                .dialogContext(dialogContext)
                .build();
    }

    /**
     * Собирает контекст для callback-команды из апдейта с {@code callback_query}.
     *
     * <p>Сообщение-источник может быть «недоступным» ({@code InaccessibleMessage}),
     * тогда {@code getMessage()} равно {@code null}, но {@code getChatId()} и
     * {@code getMessageId()} всё равно вычисляются.</p>
     *
     * @param update   апдейт, у которого {@code hasCallbackQuery()} истинно
     * @param sender   абстракция отправки
     * @param callback разобранные данные нажатой кнопки
     * @param store    хранилище «больших» данных
     * @return контекст вызова
     */
    public static CommandInvocation fromCallback(Update update, TelegramBotSender sender,
                                                 Callback callback, CallbackPayloadStore store) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        MaybeInaccessibleMessage source = callbackQuery.getMessage();
        Message message = (source instanceof Message m) ? m : null;
        Chat chat = source != null ? source.getChat() : null;
        Long chatId = chat != null ? chat.getId() : (source != null ? source.getChatId() : null);
        Integer messageId = source != null ? source.getMessageId() : null;

        return CommandInvocation.builder()
                .update(update)
                .message(message)
                .callbackQuery(callbackQuery)
                .callback(callback)
                .chat(chat)
                .user(callbackQuery.getFrom())
                .sender(sender)
                .chatId(chatId)
                .messageId(messageId)
                .payloadStore(store)
                .build();
    }
}
