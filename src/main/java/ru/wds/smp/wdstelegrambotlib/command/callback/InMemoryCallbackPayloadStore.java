package ru.wds.smp.wdstelegrambotlib.command.callback;

import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Реализация {@link CallbackPayloadStore} в оперативной памяти с TTL и фоновой
 * очисткой.
 *
 * <p>Учтены все замечания к старым TTL-механизмам проекта:</p>
 * <ul>
 *   <li>единицы времени везде согласованы через {@link Duration} (не «голый» int);</li>
 *   <li>интервал очистки не может выродиться в ноль:
 *       {@code max(ttl/2, 1с)} — никаких {@code ttl/4 == 0} →
 *       {@link IllegalArgumentException} в планировщике;</li>
 *   <li>потокобезопасность единообразна: {@link ConcurrentHashMap} +
 *       {@code putIfAbsent}/{@code remove(key, value)}, без внешнего
 *       {@code synchronized};</li>
 *   <li>срок жизни считается от момента сохранения и только по времени
 *       (без продления при доступе).</li>
 * </ul>
 *
 * <p>Ключи генерируются криптослучайно (base64url, без разделителя {@code '|'}),
 * чтобы их нельзя было предугадать и подобрать чужой.</p>
 */
@Slf4j
public class InMemoryCallbackPayloadStore implements CallbackPayloadStore, AutoCloseable {

    private record Entry(Object value, long expiresAtMillis) {
    }

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Duration ttl;
    private final ScheduledExecutorService cleaner;

    /**
     * @param ttl время жизни записей; должно быть положительным
     */
    public InMemoryCallbackPayloadStore(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("TTL хранилища callback-данных должен быть положительным, получено: " + ttl);
        }
        this.ttl = ttl;
        long intervalMillis = Math.max(ttl.dividedBy(2).toMillis(), 1000L);
        this.cleaner = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "callback-payload-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        this.cleaner.scheduleAtFixedRate(this::evictExpired, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
        log.debug("Хранилище callback-данных запущено: ttl={}, интервал очистки={}мс", ttl, intervalMillis);
    }

    @Override
    public String save(Object payload) {
        Objects.requireNonNull(payload, "payload не должен быть null");
        long expiresAt = System.currentTimeMillis() + ttl.toMillis();
        Entry entry = new Entry(payload, expiresAt);
        String id;
        do {
            id = generateId();
        } while (store.putIfAbsent(id, entry) != null);
        return id;
    }

    @Override
    public Optional<Object> get(String payloadId) {
        if (payloadId == null) {
            return Optional.empty();
        }
        Entry entry = store.get(payloadId);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAtMillis() < System.currentTimeMillis()) {
            store.remove(payloadId, entry);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public <T> Optional<T> get(String payloadId, Class<T> type) {
        return get(payloadId).filter(type::isInstance).map(type::cast);
    }

    @Override
    public void remove(String payloadId) {
        if (payloadId != null) {
            store.remove(payloadId);
        }
    }

    /** Останавливает фоновую очистку. Вызывается Spring при остановке контекста. */
    @Override
    public void close() {
        cleaner.shutdownNow();
        store.clear();
    }

    private void evictExpired() {
        try {
            long now = System.currentTimeMillis();
            store.entrySet().removeIf(e -> e.getValue().expiresAtMillis() < now);
        } catch (Exception e) {
            log.warn("Ошибка при фоновой очистке callback-данных", e);
        }
    }

    private String generateId() {
        byte[] buffer = new byte[6];
        random.nextBytes(buffer);
        // base64url не содержит '|' (разделитель callback_data); 6 байт → 8 символов.
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }
}
