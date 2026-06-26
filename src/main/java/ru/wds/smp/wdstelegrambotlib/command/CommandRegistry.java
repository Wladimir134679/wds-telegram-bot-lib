package ru.wds.smp.wdstelegrambotlib.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import ru.wds.smp.wdstelegrambotlib.command.annotation.BotController;
import ru.wds.smp.wdstelegrambotlib.command.annotation.CommandMapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Реестр команд: при старте находит все {@link BotController}-бины и строит карту
 * «(имя, тип) → {@link CommandDefinition}».
 *
 * <p>Регистрация выполняется один раз после полной инициализации контекста
 * ({@link SmartInitializingSingleton}). Для каждого метода с {@link CommandMapping}
 * сразу подбираются и кэшируются резолверы параметров.</p>
 *
 * <p>Исправляет недетерминированность старого проекта:</p>
 * <ul>
 *   <li>конфликт имён (одна команда у двух методов одного типа) → ошибка при
 *       старте с понятным сообщением, а не «молчаливая перезапись»;</li>
 *   <li>имена нормализуются (без {@code /}, нижний регистр) единообразно с парсером;</li>
 *   <li>параметр без подходящего резолвера → ошибка при старте, а не {@code null} в рантайме.</li>
 * </ul>
 */
@Slf4j
public class CommandRegistry implements SmartInitializingSingleton {

    private final ApplicationContext applicationContext;
    private final ArgumentBinder argumentBinder;
    private final Map<CommandKey, CommandDefinition> commands = new HashMap<>();

    public CommandRegistry(ApplicationContext applicationContext, ArgumentBinder argumentBinder) {
        this.applicationContext = applicationContext;
        this.argumentBinder = argumentBinder;
    }

    @Override
    public void afterSingletonsInstantiated() {
        build();
    }

    /**
     * Находит команду по имени и типу.
     *
     * @param name нормализованное имя команды (как из {@link CommandParser})
     * @param type тип команды
     * @return определение команды, либо {@code null}, если не найдено
     */
    public CommandDefinition find(String name, CommandType type) {
        if (name == null) {
            return null;
        }
        return commands.get(new CommandKey(name.toLowerCase(Locale.ROOT), type));
    }

    /** @return число зарегистрированных команд (с учётом алиасов) */
    public int size() {
        return commands.size();
    }

    private void build() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(BotController.class);
        for (Object bean : beans.values()) {
            Class<?> userClass = ClassUtils.getUserClass(bean);
            for (Method method : userClass.getDeclaredMethods()) {
                CommandMapping mapping = method.getAnnotation(CommandMapping.class);
                if (mapping == null) {
                    continue;
                }
                registerMethod(bean, method, mapping);
            }
        }
        log.info("Зарегистрировано команд: {} {}", commands.size(), commands.keySet());
    }

    private void registerMethod(Object bean, Method method, CommandMapping mapping) {
        if (mapping.value().length == 0) {
            throw new IllegalStateException("@CommandMapping без имён команд на методе " + method);
        }
        List<ParameterBinding> bindings = argumentBinder.bind(method);

        for (String rawName : mapping.value()) {
            String name = normalize(rawName);
            if (name.isEmpty()) {
                throw new IllegalStateException("Пустое имя команды в @CommandMapping на методе " + method);
            }
            CommandKey key = new CommandKey(name, mapping.type());
            CommandDefinition existing = commands.get(key);
            if (existing != null) {
                throw new IllegalStateException("Конфликт команд: '" + name + "' (" + mapping.type()
                        + ") уже зарегистрирована на " + existing.getMethod()
                        + ", повторная регистрация на " + method);
            }
            commands.put(key, new CommandDefinition(bean, method, bindings, name, mapping.type()));
        }
    }

    private String normalize(String rawName) {
        String name = rawName.strip();
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        return name.toLowerCase(Locale.ROOT);
    }

    /**
     * Ключ команды в реестре: нормализованное имя + тип.
     */
    private record CommandKey(String name, CommandType type) {
    }
}
