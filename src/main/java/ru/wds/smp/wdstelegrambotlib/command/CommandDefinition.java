package ru.wds.smp.wdstelegrambotlib.command;

import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Зарегистрированная команда: целевой бин-контроллер, его метод и предрассчитанные
 * привязки параметров.
 *
 * <p>Создаётся один раз при старте ({@link CommandRegistry}); метод и резолверы
 * параметров кэшируются. На каждый вызов выполняется только резолв значений и
 * рефлексивный {@link Method#invoke}, без повторного сканирования — это и есть
 * исправление главного перформанс-изъяна старого {@code CommandContainer}.</p>
 *
 * <p><b>Проброс исключений.</b> {@link RuntimeException}/{@link Error} из
 * пользовательского кода пробрасываются как есть (тип и причина сохраняются);
 * проверяемые исключения и ошибки доступа оборачиваются в
 * {@link CommandInvocationException}.</p>
 */
@Getter
public class CommandDefinition {

    private final Object bean;
    private final Method method;
    private final List<ParameterBinding> parameterBindings;
    private final String name;
    private final CommandType type;

    public CommandDefinition(Object bean, Method method, List<ParameterBinding> parameterBindings,
                             String name, CommandType type) {
        this.bean = bean;
        this.method = method;
        this.parameterBindings = List.copyOf(parameterBindings);
        this.name = name;
        this.type = type;
        // Контроллер может быть из другого пакета/не-public — открываем доступ один раз.
        this.method.setAccessible(true);
    }

    /**
     * Резолвит аргументы и вызывает метод-команду.
     *
     * @param invocation контекст вызова
     * @return значение, которое вернул метод (может быть {@code null}/void)
     */
    public Object invoke(CommandInvocation invocation) {
        Object[] args = new Object[parameterBindings.size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = parameterBindings.get(i).resolve(invocation);
        }
        try {
            return method.invoke(bean, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new CommandInvocationException("Команда '" + name + "' завершилась с ошибкой", cause);
        } catch (IllegalAccessException e) {
            throw new CommandInvocationException("Нет доступа к методу команды '" + name + "'", e);
        }
    }
}
