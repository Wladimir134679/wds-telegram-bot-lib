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

    /**
     * @param name     ключ диалога (первое имя-триггер)
     * @param triggers нормализованные имена команд-триггеров
     * @param start    определение метода {@code @DialogStart}
     * @param steps    карта «имя шага → определение метода {@code @DialogStep}»
     */
    public DialogDefinition(String name, List<String> triggers,
                            DialogStepDefinition start, Map<String, DialogStepDefinition> steps) {
        this.name = name;
        this.triggers = List.copyOf(triggers);
        this.start = start;
        this.steps = Map.copyOf(steps);
    }

    /**
     * Находит определение шага по имени.
     *
     * @param step имя шага
     * @return определение шага либо {@code null}, если шага нет
     */
    public DialogStepDefinition step(String step) {
        return step == null ? null : steps.get(step);
    }
}
