package ru.wds.smp.wdstelegrambotlib.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.wds.smp.wdstelegrambotlib.handler.UpdateHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    /**
     * @param sender   абстракция отправки, передаётся в каждый обработчик
     * @param handlers звенья цепочки; список копируется и сортируется внутри —
     *                 исходная коллекция не мутируется
     */
    public TelegramUpdateDispatcher(TelegramBotSender sender, List<UpdateHandler> handlers) {
        this.sender = Objects.requireNonNull(sender, "sender не должен быть null");
        List<UpdateHandler> copy = new ArrayList<>(Objects.requireNonNull(handlers, "handlers не должен быть null"));
        AnnotationAwareOrderComparator.sort(copy);
        this.handlers = List.copyOf(copy);
        log.info("Цепочка обработки апдейтов собрана: {} обработчик(ов): {}",
                this.handlers.size(), handlerNames());
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
