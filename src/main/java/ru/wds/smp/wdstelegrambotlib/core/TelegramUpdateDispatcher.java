package ru.wds.smp.wdstelegrambotlib.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.wds.smp.wdstelegrambotlib.handler.UpdateHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Диспетчер входящих апдейтов — точка приёма обновлений от Telegram.
 *
 * <p>Реализует {@link LongPollingSingleThreadUpdateConsumer}: транспорт long
 * polling вызывает {@link #consume(Update)} для каждого пришедшего апдейта.
 * Диспетчер прогоняет апдейт через цепочку {@link UpdateHandler}, отсортированную
 * по приоритету.</p>
 *
 * <p>Учтены замечания к старому {@code TelegramLongPollingEngine}:</p>
 * <ul>
 *   <li><b>Не мутирует инжектированный список.</b> При создании делается копия,
 *       которая сортируется и фиксируется как неизменяемая — побочных эффектов над
 *       spring-managed коллекцией нет.</li>
 *   <li><b>Изоляция ошибок.</b> Каждый обработчик обёрнут в try/catch: исключение
 *       в одном звене не роняет обработку всего апдейта.</li>
 *   <li><b>Прерывание цепочки.</b> Если обработчик вернул {@code false}, апдейт
 *       считается поглощённым и дальше не передаётся.</li>
 *   <li><b>Безопасное логирование.</b> Не логирует {@link Update} целиком и текст
 *       сообщений; подробности — только на DEBUG.</li>
 * </ul>
 */
@Slf4j
public class TelegramUpdateDispatcher implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramBotSender sender;
    private final List<UpdateHandler> handlers;
    private final ExecutorService executorService;
    private final StripedLock stripedLock;

    /**
     * Конструктор для обратной совместимости (последовательная обработка в текущем потоке).
     *
     * @param sender   абстракция отправки
     * @param handlers обработчики апдейтов
     */
    public TelegramUpdateDispatcher(TelegramBotSender sender, List<UpdateHandler> handlers) {
        this(sender, handlers, null, null);
    }

    /**
     * Конструктор с поддержкой асинхронного выполнения и сохранения порядка по chatId.
     *
     * @param sender           абстракция отправки, передаётся в каждый обработчик
     * @param handlers         звенья цепочки; список копируется и сортируется внутри
     * @param executorService  пул потоков для асинхронного выполнения (если null, выполняется синхронно)
     * @param stripedLock      пул блокировок по chatId (если null, блокировка не применяется)
     */
    public TelegramUpdateDispatcher(TelegramBotSender sender,
                                    List<UpdateHandler> handlers,
                                    ExecutorService executorService,
                                    StripedLock stripedLock) {
        this.sender = Objects.requireNonNull(sender, "sender не должен быть null");
        this.executorService = executorService;
        this.stripedLock = stripedLock;
        List<UpdateHandler> copy = new ArrayList<>(Objects.requireNonNull(handlers, "handlers не должен быть null"));
        AnnotationAwareOrderComparator.sort(copy);
        this.handlers = List.copyOf(copy);
        log.info("Цепочка обработки апдейтов собрана: {} обработчик(ов): {}. Асинхронный режим: {}",
                this.handlers.size(), handlerNames(), executorService != null ? "ВКЛЮЧЕН" : "ВЫКЛЮЧЕН");
    }

    /**
     * Принимает один апдейт и прогоняет его по цепочке обработчиков.
     *
     * @param update входящий апдейт от Telegram
     */
    @Override
    public void consume(Update update) {
        if (log.isDebugEnabled()) {
            log.debug("Получен апдейт updateId={}", update.getUpdateId());
        }
        if (executorService != null && stripedLock != null) {
            Long chatId = extractChatId(update);
            executorService.submit(() -> {
                java.util.concurrent.locks.Lock lock = stripedLock.get(chatId);
                lock.lock();
                try {
                    dispatchToHandlers(update);
                } finally {
                    lock.unlock();
                }
            });
        } else {
            dispatchToHandlers(update);
        }
    }

    private void dispatchToHandlers(Update update) {
        for (UpdateHandler handler : handlers) {
            try {
                boolean keepGoing = handler.handle(sender, update);
                if (!keepGoing) {
                    if (log.isDebugEnabled()) {
                        log.debug("Цепочка прервана обработчиком {} (updateId={})",
                                handler.getClass().getSimpleName(), update.getUpdateId());
                    }
                    return;
                }
            } catch (Exception e) {
                // Изоляция ошибок: один сбойный обработчик не должен ронять весь апдейт.
                log.error("Обработчик {} завершился с ошибкой (updateId={})",
                        handler.getClass().getSimpleName(), update.getUpdateId(), e);
            }
        }
    }

    /**
     * Вспомогательный метод для извлечения chatId из различных типов апдейтов.
     * Позволяет определить ключ партиционирования для сохранения порядка обработки.
     */
    private Long extractChatId(Update update) {
        if (update == null) {
            return 0L;
        }
        if (update.getMessage() != null) {
            return update.getMessage().getChatId();
        }
        if (update.getCallbackQuery() != null) {
            if (update.getCallbackQuery().getMessage() != null) {
                return update.getCallbackQuery().getMessage().getChatId();
            }
            if (update.getCallbackQuery().getFrom() != null) {
                return update.getCallbackQuery().getFrom().getId();
            }
        }
        if (update.getEditedMessage() != null) {
            return update.getEditedMessage().getChatId();
        }
        if (update.getChannelPost() != null) {
            return update.getChannelPost().getChatId();
        }
        if (update.getEditedChannelPost() != null) {
            return update.getEditedChannelPost().getChatId();
        }
        if (update.getInlineQuery() != null && update.getInlineQuery().getFrom() != null) {
            return update.getInlineQuery().getFrom().getId();
        }
        if (update.getChosenInlineQuery() != null && update.getChosenInlineQuery().getFrom() != null) {
            return update.getChosenInlineQuery().getFrom().getId();
        }
        if (update.getShippingQuery() != null && update.getShippingQuery().getFrom() != null) {
            return update.getShippingQuery().getFrom().getId();
        }
        if (update.getPreCheckoutQuery() != null && update.getPreCheckoutQuery().getFrom() != null) {
            return update.getPreCheckoutQuery().getFrom().getId();
        }
        if (update.getPollAnswer() != null && update.getPollAnswer().getUser() != null) {
            return update.getPollAnswer().getUser().getId();
        }
        if (update.getMyChatMember() != null && update.getMyChatMember().getChat() != null) {
            return update.getMyChatMember().getChat().getId();
        }
        if (update.getChatMember() != null && update.getChatMember().getChat() != null) {
            return update.getChatMember().getChat().getId();
        }
        if (update.getChatJoinRequest() != null && update.getChatJoinRequest().getChat() != null) {
            return update.getChatJoinRequest().getChat().getId();
        }
        return 0L;
    }

    private String handlerNames() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < handlers.size(); i++) {
            UpdateHandler h = handlers.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(h.getClass().getSimpleName()).append("(order=").append(h.getOrder()).append(')');
        }
        return sb.append(']').toString();
    }
}
