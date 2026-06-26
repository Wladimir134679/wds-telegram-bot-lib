package ru.wds.smp.wdstelegrambotlib.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.wds.smp.wdstelegrambotlib.autoconfigure.TelegramBotProperties;

import java.util.Objects;

/**
 * Управление жизненным циклом бота: регистрация long polling при старте контекста
 * и корректная остановка при его закрытии.
 *
 * <p>Реализовано через {@link SmartLifecycle}, что устраняет проблемы старого
 * {@code TelegramBotApiStarterService}:</p>
 * <ul>
 *   <li><b>Нет двойного старта.</b> Регистрация выполняется один раз в
 *       {@link #start()}, без связки {@code @PostConstruct} + {@code @EventListener}.</li>
 *   <li><b>Контекст готов.</b> {@link SmartLifecycle} вызывается после полной
 *       инициализации контекста, а не в {@code @PostConstruct}.</li>
 *   <li><b>Нет NPE при остановке.</b> Если регистрация не удалась, сессия остаётся
 *       {@code null}, и {@link #stop()} это корректно учитывает.</li>
 * </ul>
 *
 * <p>Фаза жизненного цикла — {@link Integer#MAX_VALUE}: бот стартует последним
 * (когда всё остальное готово принимать) и останавливается первым при выключении.</p>
 */
@Slf4j
public class TelegramBotLifecycle implements SmartLifecycle {

    private final TelegramBotsLongPollingApplication botsApplication;
    private final TelegramUpdateDispatcher dispatcher;
    private final TelegramBotProperties properties;

    private volatile boolean running = false;
    private BotSession botSession;

    public TelegramBotLifecycle(TelegramBotsLongPollingApplication botsApplication,
                                TelegramUpdateDispatcher dispatcher,
                                TelegramBotProperties properties) {
        this.botsApplication = Objects.requireNonNull(botsApplication, "botsApplication не должен быть null");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher не должен быть null");
        this.properties = Objects.requireNonNull(properties, "properties не должен быть null");
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        try {
            botSession = botsApplication.registerBot(properties.getToken(), dispatcher);
            running = true;
            log.info("Telegram-бот зарегистрирован и запущен (long polling), token={}",
                    properties.maskedToken());
        } catch (TelegramApiException e) {
            throw new IllegalStateException(
                    "Не удалось зарегистрировать Telegram-бота для long polling", e);
        }
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        try {
            // unregisterBot останавливает сессию и снимает регистрацию.
            botsApplication.unregisterBot(properties.getToken());
            log.info("Telegram-бот остановлен");
        } catch (TelegramApiException e) {
            log.warn("Ошибка при остановке Telegram-бота", e);
        } finally {
            botSession = null;
        }
    }

    @Override
    public synchronized boolean isRunning() {
        return running;
    }

    /**
     * Бот стартует последним и останавливается первым относительно остальных
     * компонентов с меньшей фазой.
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
