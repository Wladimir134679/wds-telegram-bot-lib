package ru.wds.smp.wdstelegrambotlib.handler;

import org.springframework.core.Ordered;

/**
 * Константы приоритетов для звеньев цепочки обработки апдейтов
 * ({@link UpdateHandler}).
 *
 * <p>Семантика — как у Spring {@link Ordered}: <b>чем меньше число, тем раньше</b>
 * вызывается обработчик. Значения подобраны с запасом между уровнями, чтобы можно
 * было вставлять промежуточные этапы, не переписывая существующие.</p>
 *
 * <p>Рекомендуемая раскладка цепочки (от раннего к позднему):</p>
 * <ol>
 *   <li>{@link #SECURITY} — бан/whitelist/анти-флуд, может прервать цепочку;</li>
 *   <li>{@link #LOGGING} — логирование/метрики входящих апдейтов;</li>
 *   <li>{@link #PRE_PROCESSING} — предобработка, обогащение контекста;</li>
 *   <li>{@link #COMMAND_PROCESSING} — маршрутизация и вызов команд (последний этап).</li>
 * </ol>
 *
 * <p>Класс — утилитный держатель констант, не предназначен для инстанцирования.</p>
 */
public final class HandlerPriority {

    /** Грубые проверки доступа до маршрутизации: бан, whitelist, анти-флуд. */
    public static final int SECURITY = 100;

    /** Логирование и метрики входящих апдейтов. */
    public static final int LOGGING = 300;

    /** Предобработка апдейта, обогащение будущего контекста. */
    public static final int PRE_PROCESSING = 500;

    /** Системные callback (например, «закрыть клавиатуру») — раньше обычной маршрутизации. */
    public static final int SYSTEM_CALLBACK = 900;

    /** Маршрутизация и вызов команд — обычно последнее звено цепочки. */
    public static final int COMMAND_PROCESSING = 1000;

    /** Приоритет по умолчанию для обработчиков, не указавших свой порядок. */
    public static final int DEFAULT = COMMAND_PROCESSING;

    private HandlerPriority() {
        throw new AssertionError("Утилитный класс не предназначен для инстанцирования");
    }
}
