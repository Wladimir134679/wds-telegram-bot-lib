package ru.wds.smp.wdstelegrambotlib.command;

import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

/**
 * Общее ядро вызова метода-обработчика: целевой бин, метод и предрассчитанные
 * привязки параметров. Единственная реализация рефлексивного вызова и маппинга
 * исключений — её переиспользуют и обычные команды ({@link CommandDefinition}), и
 * шаги диалогов ({@code DialogStepDefinition}).
 *
 * <p>Это аналог {@code HandlerMethod} из Spring MVC: одна сущность «метод +
 * аргументы», поверх которой разные слои строят свою маршрутизацию. Раньше логика
 * {@code invoke()} дублировалась в команде и в шаге диалога; теперь она здесь.</p>
 *
 * <p><b>Проброс исключений.</b> {@link RuntimeException}/{@link Error} из
 * пользовательского кода пробрасываются как есть (тип и причина сохраняются);
 * проверяемые исключения и ошибки доступа оборачиваются в
 * {@link CommandInvocationException} с понятной меткой обработчика.</p>
 */
@Getter
public class HandlerMethod {

    private final Object bean;
    private final Method method;
    private final List<ParameterBinding> parameterBindings;
    private final Supplier<String> label;

    /**
     * @param bean     бин-обработчик
     * @param method   метод
     * @param bindings привязки параметров (предрассчитанные)
     * @param label    поставщик человекочитаемой метки для сообщений об ошибках
     *                 (например, {@code "команда 'start'"} или {@code "шаг 'calc#ask-a'"})
     */
    public HandlerMethod(Object bean, Method method, List<ParameterBinding> bindings, Supplier<String> label) {
        this.bean = bean;
        this.method = method;
        this.parameterBindings = List.copyOf(bindings);
        this.label = label;
        // Бин-обработчик может быть из другого пакета/не-public — открываем доступ один раз.
        this.method.setAccessible(true);
    }

    /**
     * Резолвит аргументы и вызывает метод.
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
            throw new CommandInvocationException(label.get() + " завершилась с ошибкой", cause);
        } catch (IllegalAccessException e) {
            throw new CommandInvocationException("Нет доступа к методу: " + label.get(), e);
        }
    }
}
