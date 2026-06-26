package ru.wds.smp.wdstelegrambotlib.command.callback;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Содержимое inline-кнопки в удобной форме: имя команды + именованные параметры.
 *
 * <p>Это публичная, «человеческая» модель callback — разработчик работает только с
 * ней, а кодирование в строку {@code callback_data} (и лимит 64 байта) спрятано в
 * {@link CallbackCodec} и применяется автоматически при сборке клавиатуры
 * ({@link ru.wds.smp.wdstelegrambotlib.command.keyboard.Keyboards}).</p>
 *
 * <p><b>Параметры — это просто ключ→значение.</b> Значение может быть:</p>
 * <ul>
 *   <li>пользовательским примитивом, который целиком влезает в кнопку
 *       ({@code .arg("page", 2)}, {@code .arg("id", 42L)}) — на стороне команды
 *       читается через {@code @Param("page")};</li>
 *   <li>короткой <b>ссылкой</b> на «большие» данные из
 *       {@link CallbackPayloadStore} ({@code .arg("rules", ref)}) — на стороне
 *       команды читается через {@code @Payload("rules")}.</li>
 * </ul>
 *
 * <p>Пример:</p>
 * <pre>{@code
 * Callback.to("nav").arg("page", page + 1).arg("rules", ref)
 * }</pre>
 *
 * <p>Особый случай — {@link #close()}: системный callback «закрыть клавиатуру»,
 * который библиотека обрабатывает сама (убирает inline-клавиатуру с сообщения),
 * без отдельного метода-обработчика.</p>
 */
public final class Callback {

    /** Зарезервированное имя системного callback «закрыть клавиатуру». */
    public static final String SYSTEM_CLOSE = "__close";

    private final String command;
    private final Map<String, String> params;

    private Callback(String command, Map<String, String> params) {
        this.command = command;
        this.params = params;
    }

    /**
     * Начинает построение callback для команды с заданным именем.
     *
     * @param command имя команды (как в {@code @CommandMapping(type = CALLBACK)})
     * @return новый изменяемый билдер callback
     */
    public static Callback to(String command) {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("Имя команды callback не должно быть пустым");
        }
        return new Callback(command, new LinkedHashMap<>());
    }

    /**
     * Системный callback «закрыть клавиатуру». Библиотека сама уберёт inline-клавиатуру
     * с сообщения, на котором нажата такая кнопка.
     *
     * @return системный callback закрытия
     */
    public static Callback close() {
        return new Callback(SYSTEM_CLOSE, new LinkedHashMap<>());
    }

    /**
     * Добавляет именованный параметр. Значение приводится к строке (для enum — имя
     * константы). Помните про лимит 64 байта: имена и значения должны быть короткими,
     * для крупных данных используйте {@link CallbackPayloadStore} и кладите сюда ссылку.
     *
     * @param name  имя параметра
     * @param value значение (не {@code null})
     * @return этот же билдер для цепочки вызовов
     */
    public Callback arg(String name, Object value) {
        Objects.requireNonNull(name, "name не должен быть null");
        Objects.requireNonNull(value, "value не должен быть null");
        params.put(name, stringify(value));
        return this;
    }

    /** @return имя команды */
    public String command() {
        return command;
    }

    /** @return значение параметра по имени, либо {@code null} */
    public String param(String name) {
        return params.get(name);
    }

    /** @return неизменяемое отображение всех параметров (порядок добавления сохранён) */
    public Map<String, String> params() {
        return Collections.unmodifiableMap(params);
    }

    /** @return {@code true}, если это системный callback закрытия клавиатуры */
    public boolean isSystemClose() {
        return SYSTEM_CLOSE.equals(command);
    }

    /** Внутренний конструктор для {@link CallbackCodec#decode(String)}. */
    static Callback raw(String command, Map<String, String> params) {
        return new Callback(command, new LinkedHashMap<>(params));
    }

    private static String stringify(Object value) {
        return value instanceof Enum<?> e ? e.name() : String.valueOf(value);
    }
}
