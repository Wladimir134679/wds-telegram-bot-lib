package ru.wds.smp.wdstelegrambotlib.command.resolver;

import org.telegram.telegrambots.meta.api.objects.User;
import ru.wds.smp.wdstelegrambotlib.command.CommandInvocation;
import ru.wds.smp.wdstelegrambotlib.command.annotation.ChatId;
import ru.wds.smp.wdstelegrambotlib.command.annotation.UserId;

import java.lang.reflect.Parameter;

/**
 * Резолвер идентификаторов {@code @ChatId}/{@code @UserId} в параметры
 * {@code long}/{@link Long}.
 *
 * <p>Общий для команд, callback и диалогов — значения берутся из единого
 * {@link CommandInvocation}. Для отсутствующего значения: {@code 0L} для примитива
 * {@code long}, {@code null} для {@link Long}.</p>
 */
public class IdArgumentResolver implements CommandArgumentResolver {

    @Override
    public boolean supports(Parameter parameter) {
        boolean annotated = parameter.isAnnotationPresent(ChatId.class)
                || parameter.isAnnotationPresent(UserId.class);
        return annotated && (parameter.getType() == long.class || parameter.getType() == Long.class);
    }

    @Override
    public Object resolve(Parameter parameter, CommandInvocation invocation) {
        Long value;
        if (parameter.isAnnotationPresent(ChatId.class)) {
            value = invocation.getChatId();
        } else {
            User user = invocation.getUser();
            value = user != null ? user.getId() : null;
        }
        if (value == null && parameter.getType() == long.class) {
            return 0L;
        }
        return value;
    }

    @Override
    public int getOrder() {
        return 110;
    }
}
