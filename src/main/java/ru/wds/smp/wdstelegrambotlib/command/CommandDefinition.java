package ru.wds.smp.wdstelegrambotlib.command;

import lombok.Getter;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Зарегистрированная команда: целевой бин-контроллер, его метод и предрассчитанные
 * привязки параметров.
 *
 * <p>Создаётся один раз при старте ({@link CommandRegistry}); сам вызов делегируется
 * общему ядру {@link HandlerMethod} — тому же, что используют шаги диалогов, поэтому
 * рефлексия и маппинг исключений существуют в проекте в единственном экземпляре.
 * На каждый вызов выполняется только резолв значений и {@link Method#invoke}, без
 * повторного сканирования.</p>
 */
@Getter
public class CommandDefinition {

    private final HandlerMethod handlerMethod;
    private final String name;
    private final CommandType type;

    public CommandDefinition(Object bean, Method method, List<ParameterBinding> parameterBindings,
                             String name, CommandType type) {
        this.name = name;
        this.type = type;
        this.handlerMethod = new HandlerMethod(bean, method, parameterBindings,
                () -> "Команда '" + name + "'");
    }

    /** @return метод команды (для сообщений об ошибках/конфликтах) */
    public Method getMethod() {
        return handlerMethod.getMethod();
    }

    /**
     * Резолвит аргументы и вызывает метод-команду.
     *
     * @param invocation контекст вызова
     * @return значение, которое вернул метод (может быть {@code null}/void)
     */
    public Object invoke(CommandInvocation invocation) {
        return handlerMethod.invoke(invocation);
    }
}
