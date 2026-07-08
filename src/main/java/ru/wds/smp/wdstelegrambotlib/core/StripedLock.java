package ru.wds.smp.wdstelegrambotlib.core;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Пул блокировок (Striped Lock) для разграничения потоков.
 *
 * <p>Позволяет ассоциировать объекты-ключи (например, {@code chatId}) с конкретными
 * блокировками из фиксированного набора. Это гарантирует, что запросы для одного и
 * того же чата будут обрабатываться строго последовательно (выстраиваться в очередь),
 * в то время как разные чаты будут обрабатываться параллельно без взаимных блокировок.</p>
 *
 * <p>Позволяет обойтись без добавления тяжелой внешней зависимости вроде Guava.</p>
 */
public class StripedLock {

    private final Lock[] locks;

    /**
     * Создает пул блокировок заданного размера.
     *
     * @param stripes размер пула (рекомендуется использовать степень двойки, например, 256)
     */
    public StripedLock(int stripes) {
        if (stripes <= 0) {
            throw new IllegalArgumentException("Размер пула блокировок должен быть больше 0");
        }
        this.locks = new Lock[stripes];
        for (int i = 0; i < stripes; i++) {
            this.locks[i] = new ReentrantLock();
        }
    }

    /**
     * Возвращает блокировку для указанного ключа на основе его хэш-кода.
     *
     * @param key объект-ключ (например, chatId)
     * @return блокировка {@link Lock} из пула
     */
    public Lock get(Object key) {
        if (key == null) {
            return locks[0];
        }
        int index = Math.abs(key.hashCode() % locks.length);
        return locks[index];
    }
}
