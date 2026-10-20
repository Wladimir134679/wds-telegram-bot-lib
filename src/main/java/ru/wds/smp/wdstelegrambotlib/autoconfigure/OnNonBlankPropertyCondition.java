package ru.wds.smp.wdstelegrambotlib.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Реализация {@link ConditionalOnNonBlankProperty}: выполняется, только если
 * значение свойства не {@code null} и содержит непробельный текст.
 *
 * <p>Значение свойства в отчёт условий не попадает — там может быть секрет (токен).</p>
 */
class OnNonBlankPropertyCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes =
                metadata.getAnnotationAttributes(ConditionalOnNonBlankProperty.class.getName());
        String name = (String) attributes.get("value");
        String value = context.getEnvironment().getProperty(name);
        if (StringUtils.hasText(value)) {
            return ConditionOutcome.match("свойство '" + name + "' задано");
        }
        return ConditionOutcome.noMatch("свойство '" + name + "' не задано или пустое");
    }
}
