package ru.wds.smp.wdstelegrambotlib.dialog;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Реализация {@link DialogStateStore} в оперативной памяти с TTL и фоновой
 * очисткой по образцу
 * {@link ru.wds.smp.wdstelegrambotlib.command.callback.InMemoryCallbackPayloadStore}.
 *
 * <p>Учтены те же требования к TTL, что заданы в архитектуре проекта:</p>
 * <ul>
 *   <li>единицы времени согласованы через {@link Duration}, а не «голый» int;</li>
 *   <li>интервал очистки не вырождается в ноль: {@code max(ttl/2, 1с)} — никаких
 *       {@code ttl/4 == 0} → {@link IllegalArgumentException} в планировщике;</li>
 *   <li>потокобезопасность единообразна: {@link ConcurrentHashMap} +
 *       {@code computeIfAbsent}/{@code remove(key,value)}, без внешнего
 *       {@code synchronized};</li>
 *   <li>TTL продлевается при активности диалога (на {@code save}) и при старте —
 *       сессия живёт, пока пользователь продолжает диалог, и протухает при простое.</li>
 * </ul>
 */
@Slf4j
public class InMemoryDialogStateStore implements DialogStateStore, AutoCloseable {

    /** Состояние + момент истечения; обновляется целиком при продлении TTL. */
    private record Entry(DialogState state, long expiresAtMillis) {
    }

    private final ConcurrentHashMap<DialogKey, Entry> store = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final ScheduledExecutorService cleaner;

    /**
     * @param ttl время жизни простаивающей диалоговой сессии; должно быть положительным
     */
    public InMemoryDialogStateStore(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("TTL диалоговых сессий должен быть положительным, получено: " + ttl);
        }
        this.ttl = ttl;
        long intervalMillis = Math.max(ttl.dividedBy(2).toMillis(), 1000L);
        this.cleaner = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "dialog-session-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        this.cleaner.scheduleAtFixedRate(this::evictExpired, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
        log.debug("Хранилище диалоговых сессий запущено: ttl={}, интервал очистки={}мс", ttl, intervalMillis);
    }

    @Override
    public Optional<DialogState> find(DialogKey key) {
        if (key == null) {
            return Optional.empty();
        }
        Entry entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAtMillis() < System.currentTimeMillis()) {
            store.remove(key, entry);
            return Optional.empty();
        }
        return Optional.of(entry.state());
    }

    @Override
    public DialogState start(DialogKey key, String dialogName) {
        DialogState state = new DialogState(dialogName);
        store.put(key, new Entry(state, expiry()));
        return state;
    }

    @Override
    public void save(DialogKey key, DialogState state) {
        store.put(key, new Entry(state, expiry()));
    }

    @Override
    public void remove(DialogKey key) {
        if (key != null) {
            store.remove(key);
        }
    }

    /** Останавливает фоновую очистку. Вызывается Spring при остановке контекста. */
    @Override
    public void close() {
        cleaner.shutdownNow();
        store.clear();
    }

    private long expiry() {
        return System.currentTimeMillis() + ttl.toMillis();
    }

    private void evictExpired() {
        try {
            long now = System.currentTimeMillis();
            store.entrySet().removeIf(e -> e.getValue().expiresAtMillis() < now);
        } catch (Exception e) {
            log.warn("Ошибка при фоновой очистке диалоговых сессий", e);
        }
    }
}
