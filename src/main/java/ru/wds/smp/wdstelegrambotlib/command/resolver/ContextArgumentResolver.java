package ru.wds.smp.wdstelegrambotlib.command.resolver;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.wds.smp.wdstelegrambotlib.command.CommandInvocation;
import ru.wds.smp.wdstelegrambotlib.command.ParsedCommand;
import ru.wds.smp.wdstelegrambotlib.command.callback.Callback;
import ru.wds.smp.wdstelegrambotlib.core.TelegramBotSender;

import java.lang.reflect.Parameter;
import java.util.Set;

/**
 * Резолвер «контекстных» параметров по типу: объекты Telegram и инфраструктуры,
 * доступные из {@link CommandInvocation}.
 *
 * <p>Поддерживаемые типы: {@link Update}, {@link Message}, {@link CallbackQuery},
 * {@link Callback}, {@link Chat}, {@link User}, {@link TelegramBotSender},
 * {@link ParsedCommand}, {@link CommandInvocation}. Любой можно объявить параметром
 * метода-команды в любом порядке.</p>
 *
 * <p>Имеет относительно низкий приоритет, чтобы аннотированные параметры
 * ({@code @Text}/{@code @Param}/{@code @Payload}) резолвились раньше.</p>
 */
public class ContextArgumentResolver implements CommandArgumentResolver {

    private static final Set<Class<?>> SUPPORTED = Set.of(
            Update.class, Message.class, CallbackQuery.class, Callback.class, Chat.class, User.class,
            TelegramBotSender.class, ParsedCommand.class, CommandInvocation.class);

    @Override
    public boolean supports(Parameter parameter) {
        return SUPPORTED.contains(parameter.getType());
    }

    @Override
    public Object resolve(Parameter parameter, CommandInvocation invocation) {
        Class<?> type = parameter.getType();
        if (type == Update.class) {
            return invocation.getUpdate();
        }
        if (type == Message.class) {
            return invocation.getMessage();
        }
        if (type == CallbackQuery.class) {
            return invocation.getCallbackQuery();
        }
        if (type == Callback.class) {
            return invocation.getCallback();
        }
        if (type == Chat.class) {
            return invocation.getChat();
        }
        if (type == User.class) {
            return invocation.getUser();
        }
        if (type == TelegramBotSender.class) {
            return invocation.getSender();
        }
        if (type == ParsedCommand.class) {
            return invocation.getCommand();
        }
        if (type == CommandInvocation.class) {
            return invocation;
        }
        return null;
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
