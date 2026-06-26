package ru.wds.smp.examplebot.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.wds.smp.wdstelegrambotlib.core.TelegramBotSender;
import ru.wds.smp.wdstelegrambotlib.handler.HandlerPriority;
import ru.wds.smp.wdstelegrambotlib.handler.UpdateHandler;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Демонстрация точки расширения «цепочка обработчиков с приоритетами».
 *
 * <p>Это собственное звено потребителя: достаточно объявить бин
 * {@link UpdateHandler}, и библиотека сама вставит его в цепочку согласно
 * {@link #getOrder()}. Здесь — простая «метрика»: счётчик входящих апдейтов с
 * логированием на DEBUG (без текста и PII).</p>
 *
 * <p>Приоритет {@link HandlerPriority#PRE_PROCESSING} ставит звено <b>раньше</b>
 * маршрутизации команд ({@link HandlerPriority#COMMAND_PROCESSING}), но позже
 * проверок доступа. Метод возвращает {@code true} — апдейт передаётся дальше по
 * цепочке. Вернув {@code false}, можно было бы «поглотить» апдейт (например, в
 * анти-флуде или баноне).</p>
 */
@Slf4j
@Component
public class UpdateMetricsHandler implements UpdateHandler {

    private final AtomicLong counter = new AtomicLong();

    @Override
    public boolean handle(TelegramBotSender sender, Update update) {
        long total = counter.incrementAndGet();
        String kind = update.hasCallbackQuery() ? "callback"
                : update.hasMessage() ? "message"
                : "other";
        log.debug("Входящий апдейт #{} типа {}", total, kind);
        return true; // не прерываем цепочку — пропускаем апдейт дальше
    }

    @Override
    public int getOrder() {
        return HandlerPriority.PRE_PROCESSING;
    }
}
