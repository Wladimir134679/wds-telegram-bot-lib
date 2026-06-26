package ru.wds.smp.wdstelegrambotlib.dialog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import ru.wds.smp.wdstelegrambotlib.command.ArgumentBinder;
import ru.wds.smp.wdstelegrambotlib.command.ParameterBinding;
import ru.wds.smp.wdstelegrambotlib.dialog.annotation.Dialog;
import ru.wds.smp.wdstelegrambotlib.dialog.annotation.DialogStart;
import ru.wds.smp.wdstelegrambotlib.dialog.annotation.DialogStep;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Реестр диалогов: при старте находит все {@link Dialog}-бины и строит карты
 * «имя диалога → {@link DialogDefinition}» и «триггер → диалог».
 *
 * <p>Построен по образцу
 * {@link ru.wds.smp.wdstelegrambotlib.command.CommandRegistry}: регистрация —
 * один раз после полной инициализации контекста ({@link SmartInitializingSingleton}),
 * привязки параметров методов кэшируются, конфликты имён диалогов/триггеров/шагов
 * приводят к понятной ошибке при старте, а не к «молчаливой перезаписи» в рантайме.</p>
 */
@Slf4j
public class DialogRegistry implements SmartInitializingSingleton {

    private final ApplicationContext applicationContext;
    private final ArgumentBinder argumentBinder;
    private final Map<String, DialogDefinition> byName = new HashMap<>();
    private final Map<String, DialogDefinition> byTrigger = new HashMap<>();

    public DialogRegistry(ApplicationContext applicationContext, ArgumentBinder argumentBinder) {
        this.applicationContext = applicationContext;
        this.argumentBinder = argumentBinder;
    }

    @Override
    public void afterSingletonsInstantiated() {
        build();
    }

    /**
     * Находит диалог по ключу (имени).
     *
     * @param name имя диалога
     * @return определение либо {@code null}
     */
    public DialogDefinition find(String name) {
        return name == null ? null : byName.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Находит диалог по команде-триггеру (нормализованное имя без {@code /}).
     *
     * @param trigger нормализованное имя команды
     * @return определение диалога, который надо запустить, либо {@code null}
     */
    public DialogDefinition findByTrigger(String trigger) {
        return trigger == null ? null : byTrigger.get(trigger.toLowerCase(Locale.ROOT));
    }

    /** @return число зарегистрированных диалогов */
    public int size() {
        return byName.size();
    }

    private void build() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Dialog.class);
        for (Object bean : beans.values()) {
            Class<?> userClass = ClassUtils.getUserClass(bean);
            Dialog dialog = userClass.getAnnotation(Dialog.class);
            if (dialog == null) {
                continue;
            }
            registerDialog(bean, userClass, dialog);
        }
        log.info("Зарегистрировано диалогов: {} {}", byName.size(), byName.keySet());
    }

    private void registerDialog(Object bean, Class<?> userClass, Dialog dialog) {
        if (dialog.value().length == 0) {
            throw new IllegalStateException("@Dialog без имён-триггеров на классе " + userClass.getName());
        }
        List<String> triggers = normalizeTriggers(dialog.value(), userClass);
        String name = triggers.get(0);

        DialogStepDefinition start = null;
        Map<String, DialogStepDefinition> steps = new HashMap<>();

        for (Method method : userClass.getDeclaredMethods()) {
            DialogStart startAnn = method.getAnnotation(DialogStart.class);
            DialogStep stepAnn = method.getAnnotation(DialogStep.class);
            if (startAnn != null && stepAnn != null) {
                throw new IllegalStateException("Метод " + method
                        + " помечен и @DialogStart, и @DialogStep одновременно");
            }
            if (startAnn != null) {
                if (start != null) {
                    throw new IllegalStateException("В диалоге '" + name
                            + "' больше одного метода @DialogStart: " + start.getMethod() + " и " + method);
                }
                start = new DialogStepDefinition(bean, method, argumentBinder.bind(method), name, null);
            } else if (stepAnn != null) {
                registerSteps(bean, method, stepAnn, name, steps);
            }
        }

        if (start == null) {
            throw new IllegalStateException("В диалоге '" + name + "' (" + userClass.getName()
                    + ") нет метода @DialogStart");
        }

        DialogDefinition definition = new DialogDefinition(name, triggers, start, steps);
        if (byName.putIfAbsent(name, definition) != null) {
            throw new IllegalStateException("Конфликт диалогов: ключ '" + name + "' уже занят");
        }
        for (String trigger : triggers) {
            DialogDefinition existing = byTrigger.putIfAbsent(trigger, definition);
            if (existing != null && existing != definition) {
                throw new IllegalStateException("Конфликт триггеров диалогов: '" + trigger
                        + "' заявлен в '" + name + "' и в '" + existing.getName() + "'");
            }
        }
    }

    private void registerSteps(Object bean, Method method, DialogStep stepAnn, String dialogName,
                               Map<String, DialogStepDefinition> steps) {
        if (stepAnn.value().length == 0) {
            throw new IllegalStateException("@DialogStep без имени шага на методе " + method);
        }
        List<ParameterBinding> bindings = argumentBinder.bind(method);
        for (String rawStep : stepAnn.value()) {
            String step = rawStep.strip();
            if (step.isEmpty()) {
                throw new IllegalStateException("Пустое имя шага в @DialogStep на методе " + method);
            }
            DialogStepDefinition existing = steps.putIfAbsent(step,
                    new DialogStepDefinition(bean, method, bindings, dialogName, step));
            if (existing != null) {
                throw new IllegalStateException("Конфликт шагов в диалоге '" + dialogName
                        + "': шаг '" + step + "' объявлен на " + existing.getMethod() + " и " + method);
            }
        }
    }

    private List<String> normalizeTriggers(String[] raw, Class<?> userClass) {
        List<String> triggers = new ArrayList<>(raw.length);
        for (String rawName : raw) {
            String name = rawName.strip();
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            name = name.toLowerCase(Locale.ROOT);
            if (name.isEmpty()) {
                throw new IllegalStateException("Пустое имя-триггер в @Dialog на классе " + userClass.getName());
            }
            if (!triggers.contains(name)) {
                triggers.add(name);
            }
        }
        return triggers;
    }
}
