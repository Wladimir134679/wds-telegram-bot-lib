package ru.wds.smp.wdstelegrambotlib.command;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import ru.wds.smp.wdstelegrambotlib.command.resolver.CommandArgumentResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * Единая точка построения привязок параметров метода-обработчика к резолверам.
 *
 * <p>Это общий «binder» для обеих систем — обычных команд ({@code CommandRegistry})
 * и шагов диалогов ({@code DialogRegistry}). Раньше логика подбора резолвера и
 * сборки {@link ParameterBinding} дублировалась в каждом реестре; теперь она здесь
 * в одном экземпляре. Это и есть «единая точка обработки методов»: добавил резолвер
 * в контекст — его автоматически получают и команды, и диалоги, забыть «прокинуть»
 * в одном из мест невозможно.</p>
 *
 * <p>Резолверы сортируются один раз при создании (по {@code @Order}/{@code Ordered});
 * подбор для каждого параметра выполняется один раз при регистрации обработчика, а
 * не на каждый вызов.</p>
 */
public class ArgumentBinder {

    private final List<CommandArgumentResolver> resolvers;

    /**
     * @param resolvers все резолверы аргументов из контекста (будут отсортированы)
     */
    public ArgumentBinder(List<CommandArgumentResolver> resolvers) {
        List<CommandArgumentResolver> sorted = new ArrayList<>(resolvers);
        AnnotationAwareOrderComparator.sort(sorted);
        this.resolvers = List.copyOf(sorted);
    }

    /**
     * Строит привязки для всех параметров метода в порядке их объявления.
     *
     * @param method метод-обработчик (команда или шаг диалога)
     * @return неизменяемый список привязок
     * @throws IllegalStateException если для какого-то параметра нет подходящего резолвера
     */
    public List<ParameterBinding> bind(Method method) {
        Parameter[] parameters = method.getParameters();
        List<ParameterBinding> bindings = new ArrayList<>(parameters.length);
        for (Parameter parameter : parameters) {
            bindings.add(new ParameterBinding(parameter, findResolver(parameter, method)));
        }
        return List.copyOf(bindings);
    }

    private CommandArgumentResolver findResolver(Parameter parameter, Method method) {
        for (CommandArgumentResolver resolver : resolvers) {
            if (resolver.supports(parameter)) {
                return resolver;
            }
        }
        throw new IllegalStateException("Не найден резолвер для параметра типа "
                + parameter.getType().getName() + " в методе " + method
                + ". Используйте @Text, @Param/@Payload, @ChatId/@UserId, DialogContext, "
                + "медиа-тип (Document/Video/Voice/PhotoSize/...) или поддерживаемый контекстный тип.");
    }
}
