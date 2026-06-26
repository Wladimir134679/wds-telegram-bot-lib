package ru.wds.smp.wdstelegrambotlib.dialog;

import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Описание одной диалог-машины: имя (ключ), команды-триггеры, метод старта и карта
 * шагов.
 *
 * <p>Строится один раз при старте {@link DialogRegistry} из бина с аннотацией
 * {@link ru.wds.smp.wdstelegrambotlib.dialog.annotation.Dialog}.</p>
 */
@Getter
public class DialogDefinition {

    private final String name;
    private final List<String> triggers;
    private final DialogStepDefinition start;
    private final Map<String, DialogStepDefinition> steps;
    private final Map<String, DialogStepDefinition> callbacks;

    /**
     * @param name      ключ диалога (первое имя-триггер)
     * @param triggers  нормализованные имена команд-триггеров
     * @param start     определение метода {@code @DialogStart}
     * @param steps     карта «имя шага → метод {@code @DialogStep}» (текстовый ввод)
     * @param callbacks карта «имя действия → метод {@code @DialogCallback}» (нажатия кнопок)
     */
    public DialogDefinition(String name, List<String> triggers, DialogStepDefinition start,
                            Map<String, DialogStepDefinition> steps,
                            Map<String, DialogStepDefinition> callbacks) {
        this.name = name;
        this.triggers = List.copyOf(triggers);
        this.start = start;
        this.steps = Map.copyOf(steps);
        this.callbacks = Map.copyOf(callbacks);
    }

    /**
     * Находит определение текстового шага по имени.
     *
     * @param step имя шага
     * @return определение шага либо {@code null}, если шага нет
     */
    public DialogStepDefinition step(String step) {
        return step == null ? null : steps.get(step);
    }

    /**
     * Находит определение callback-обработчика по имени действия.
     *
     * @param action имя действия (как в {@code Callback.to("...")})
     * @return определение обработчика либо {@code null}, если действия нет
     */
    public DialogStepDefinition callback(String action) {
        return action == null ? null : callbacks.get(action);
    }
}
