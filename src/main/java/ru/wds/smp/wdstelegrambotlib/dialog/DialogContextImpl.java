package ru.wds.smp.wdstelegrambotlib.dialog;

import java.util.List;
import java.util.Optional;

/**
 * Реализация {@link DialogContext} поверх {@link DialogState}.
 *
 * <p>Создаётся {@link DialogUpdateHandler} на один апдейт. Управляющие методы
 * ({@link #next(String)}, {@link #finish()}, {@link #cancel()}) не выполняют переход
 * сами, а фиксируют намерение в {@link #outcome}/{@link #nextStep}; применяет его
 * хендлер уже после возврата из метода-шага. Это удерживает изменение состояния в
 * одном месте и упрощает рассуждение о согласованности.</p>
 */
class DialogContextImpl implements DialogContext {

    /** Исход шага: что хендлер сделает с состоянием после вызова метода. */
    enum Outcome {
        /** Остаться на текущем шаге (управляющий метод не вызывался). */
        STAY,
        /** Перейти на {@link #nextStep}. */
        NEXT,
        /** Успешно завершить диалог. */
        FINISH,
        /** Отменить диалог. */
        CANCEL
    }

    private final DialogKey key;
    private final DialogState state;

    private Outcome outcome = Outcome.STAY;
    private String nextStep;

    DialogContextImpl(DialogKey key, DialogState state) {
        this.key = key;
        this.state = state;
    }

    @Override
    public String dialog() {
        return state.dialogName();
    }

    @Override
    public String step() {
        return state.currentStep();
    }

    @Override
    public long chatId() {
        return key.chatId();
    }

    @Override
    public long userId() {
        return key.userId();
    }

    @Override
    public void set(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("Ключ контекста диалога не должен быть null");
        }
        if (value == null) {
            state.data().remove(key);
        } else {
            state.data().put(key, value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) state.data().get(key);
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = state.data().get(key);
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }

    @Override
    public boolean has(String key) {
        return state.data().containsKey(key);
    }

    @Override
    public void remove(String key) {
        if (key != null) {
            state.data().remove(key);
        }
    }

    @Override
    public void next(String step) {
        if (step == null || step.isBlank()) {
            throw new IllegalArgumentException("Имя следующего шага не должно быть пустым");
        }
        this.outcome = Outcome.NEXT;
        this.nextStep = step;
    }

    @Override
    public void finish() {
        this.outcome = Outcome.FINISH;
        this.nextStep = null;
    }

    @Override
    public void cancel() {
        this.outcome = Outcome.CANCEL;
        this.nextStep = null;
    }

    @Override
    public List<String> history() {
        return state.historySnapshot();
    }

    // --- доступ для хендлера (пакетная видимость) ---

    Outcome outcome() {
        return outcome;
    }

    String nextStep() {
        return nextStep;
    }
}
