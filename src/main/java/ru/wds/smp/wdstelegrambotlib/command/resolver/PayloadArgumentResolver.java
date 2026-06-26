package ru.wds.smp.wdstelegrambotlib.command.resolver;

import ru.wds.smp.wdstelegrambotlib.command.CommandInvocation;
import ru.wds.smp.wdstelegrambotlib.command.annotation.Payload;
import ru.wds.smp.wdstelegrambotlib.command.callback.Callback;
import ru.wds.smp.wdstelegrambotlib.command.callback.CallbackPayloadExpiredException;
import ru.wds.smp.wdstelegrambotlib.command.callback.CallbackPayloadStore;

import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.Optional;

/**
 * Резолвер параметра, помеченного {@link Payload}: трактует значение именованного
 * параметра кнопки как ссылку на данные в {@link CallbackPayloadStore}, достаёт
 * объект и приводит к типу параметра.
 *
 * <p>При отсутствии/истечении обязательных данных бросает
 * {@link CallbackPayloadExpiredException}.</p>
 */
public class PayloadArgumentResolver implements CommandArgumentResolver {

    private final CallbackPayloadStore payloadStore;

    public PayloadArgumentResolver(CallbackPayloadStore payloadStore) {
        this.payloadStore = Objects.requireNonNull(payloadStore, "payloadStore не должен быть null");
    }

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(Payload.class);
    }

    @Override
    public Object resolve(Parameter parameter, CommandInvocation invocation) {
        Payload annotation = parameter.getAnnotation(Payload.class);
        String name = annotation.value();
        Callback callback = invocation.getCallback();
        String ref = callback != null ? callback.param(name) : null;

        if (ref == null) {
            if (annotation.required()) {
                throw new CallbackPayloadExpiredException("Кнопка не содержит ссылки на данные ('" + name + "')");
            }
            return null;
        }

        Optional<?> value = payloadStore.get(ref, parameter.getType());
        if (value.isEmpty()) {
            if (annotation.required()) {
                throw new CallbackPayloadExpiredException("Данные устарели или недоступны — повторите действие");
            }
            return null;
        }
        return value.get();
    }

    @Override
    public int getOrder() {
        return 90;
    }
}
