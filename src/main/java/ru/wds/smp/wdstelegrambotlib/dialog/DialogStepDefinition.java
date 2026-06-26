package ru.wds.smp.wdstelegrambotlib.dialog;

import lombok.Getter;
import ru.wds.smp.wdstelegrambotlib.command.CommandInvocation;
import ru.wds.smp.wdstelegrambotlib.command.HandlerMethod;
import ru.wds.smp.wdstelegrambotlib.command.ParameterBinding;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Зарегистрированный шаг диалога (или его старт): обёртка над общим ядром
 * {@link HandlerMethod} плюс метаданные шага.
 *
 * <p>Вызов делегируется тому же {@link HandlerMethod}, что и у обычных команд —
 * никакой отдельной копии рефлексии и маппинга исключений. Привязки параметров
 * строит общий {@code ArgumentBinder}, поэтому набор доступных в шаге аргументов
 * (контекст, {@code @Text}, медиа, {@code @ChatId}/{@code @UserId}, …) ровно тот же,
 * что и в командах.</p>
 */
@Getter
public class DialogStepDefinition {

    private final HandlerMethod handlerMethod;
    private final String dialogName;
    private final String stepName;

    /**
     * @param bean       бин-диалог
     * @param method     метод шага
     * @param bindings   привязки параметров
     * @param dialogName имя диалога
     * @param stepName   имя шага либо {@code null} для {@code @DialogStart}
     */
    public DialogStepDefinition(Object bean, Method method, List<ParameterBinding> bindings,
                                String dialogName, String stepName) {
        this.dialogName = dialogName;
        this.stepName = stepName;
        String label = "Шаг '" + dialogName + (stepName != null ? "#" + stepName : "#<start>") + "'";
        this.handlerMethod = new HandlerMethod(bean, method, bindings, () -> label);
    }

    /** @return метод шага (для сообщений об ошибках/конфликтах) */
    public Method getMethod() {
        return handlerMethod.getMethod();
    }

    /**
     * Резолвит аргументы и вызывает метод шага.
     *
     * @param invocation контекст вызова (с проставленным {@link DialogContext})
     * @return значение, которое вернул метод (трактуется как ответ пользователю)
     */
    public Object invoke(CommandInvocation invocation) {
        return handlerMethod.invoke(invocation);
    }
}
