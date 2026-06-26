package ru.wds.smp.wdstelegrambotlib.command.resolver;

import ru.wds.smp.wdstelegrambotlib.command.CommandArgumentException;
import ru.wds.smp.wdstelegrambotlib.command.CommandInvocation;
import ru.wds.smp.wdstelegrambotlib.command.callback.Callback;
import ru.wds.smp.wdstelegrambotlib.command.annotation.Param;

import java.lang.reflect.Parameter;

/**
 * Резолвер именованного параметра callback-команды, помеченного {@link Param}.
 *
 * <p>Берёт значение параметра кнопки по имени и приводит к типу параметра метода.
 * Поддерживаемые типы: {@link String}, {@code long}/{@link Long},
 * {@code int}/{@link Integer}, {@code double}/{@link Double},
 * {@code boolean}/{@link Boolean}, {@code enum}.</p>
 */
public class ParamArgumentResolver implements CommandArgumentResolver {

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(Param.class);
    }

    @Override
    public Object resolve(Parameter parameter, CommandInvocation invocation) {
        Param annotation = parameter.getAnnotation(Param.class);
        String name = annotation.value();
        Callback callback = invocation.getCallback();
        String raw = callback != null ? callback.param(name) : null;

        if (raw == null) {
            if (annotation.required()) {
                throw new CommandArgumentException("Отсутствует обязательный параметр '" + name + "' в callback");
            }
            return defaultValue(parameter.getType());
        }
        return convert(raw, parameter.getType(), name);
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convert(String raw, Class<?> type, String name) {
        try {
            if (type == String.class) {
                return raw;
            }
            if (type == long.class || type == Long.class) {
                return Long.parseLong(raw);
            }
            if (type == int.class || type == Integer.class) {
                return Integer.parseInt(raw);
            }
            if (type == double.class || type == Double.class) {
                return Double.parseDouble(raw);
            }
            if (type == boolean.class || type == Boolean.class) {
                return Boolean.parseBoolean(raw);
            }
            if (type.isEnum()) {
                return Enum.valueOf((Class) type, raw);
            }
        } catch (IllegalArgumentException e) {
            throw new CommandArgumentException("Параметр '" + name + "' ('" + raw
                    + "') нельзя привести к " + type.getSimpleName(), e);
        }
        throw new CommandArgumentException("Неподдерживаемый тип параметра '" + name + "': " + type.getName());
    }

    private Object defaultValue(Class<?> type) {
        if (type == long.class) {
            return 0L;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == double.class) {
            return 0d;
        }
        if (type == boolean.class) {
            return false;
        }
        return null;
    }
}
