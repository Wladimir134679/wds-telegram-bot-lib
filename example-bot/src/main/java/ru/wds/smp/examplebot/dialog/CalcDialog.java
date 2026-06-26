package ru.wds.smp.examplebot.dialog;

import ru.wds.smp.wdstelegrambotlib.command.annotation.Text;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogContext;
import ru.wds.smp.wdstelegrambotlib.dialog.annotation.Dialog;
import ru.wds.smp.wdstelegrambotlib.dialog.annotation.DialogStart;
import ru.wds.smp.wdstelegrambotlib.dialog.annotation.DialogStep;

import java.util.Locale;

/**
 * Демонстрация слоя диалогов: пошаговый калькулятор с ветвлением.
 *
 * <p>Сценарий (ровно как в обсуждении задачи):</p>
 * <ol>
 *   <li>{@code /calc} запускает диалог и спрашивает операцию;</li>
 *   <li>шаг {@code ask-op} запоминает операцию и спрашивает число A;</li>
 *   <li>шаг {@code ask-a} <b>ветвится</b>: для унарной операции (√) сразу считает
 *       результат и завершает диалог, а для бинарной (+ − × ÷) спрашивает число B;</li>
 *   <li>шаг {@code ask-b} считает результат по запомненной операции и завершает диалог.</li>
 * </ol>
 *
 * <p>Видно ключевое: каждый шаг — отдельный метод с инъекцией ввода через
 * {@code @Text} и состоянием через {@link DialogContext}; переходы — явные
 * ({@code ctx.next(...)}); ветвление — обычный {@code if} с разными переходами;
 * невалидный ввод оставляет на том же шаге (просто не вызываем переход).</p>
 */
@Dialog({"calc", "🧮 Калькулятор"})
public class CalcDialog {

    /**
     * Старт диалога: приветствие и выбор операции.
     *
     * @param ctx контекст диалога
     * @return приглашение выбрать операцию
     */
    @DialogStart
    public String start(DialogContext ctx) {
        ctx.next("ask-op");
        return """
                🧮 Калькулятор. Выберите операцию и пришлите её одним символом:
                +  −  ×  ÷  либо  √  (квадратный корень)""";
    }

    /**
     * Шаг выбора операции. Запоминает операцию и спрашивает первое число.
     *
     * @param input ввод пользователя (символ операции)
     * @param ctx   контекст диалога
     * @return приглашение ввести число A
     */
    @DialogStep("ask-op")
    public String askOp(@Text String input, DialogContext ctx) {
        String op = normalizeOp(input);
        if (op == null) {
            // Невалидный ввод — остаёмся на этом же шаге (перехода нет).
            return "Не понял операцию. Пришлите один из: + − × ÷ √";
        }
        ctx.set("op", op);
        ctx.next("ask-a");
        return "Введите число A:";
    }

    /**
     * Шаг ввода числа A. Здесь происходит ветвление по арности операции.
     *
     * @param input ввод пользователя (число A)
     * @param ctx   контекст диалога
     * @return результат (для √) или приглашение ввести число B
     */
    @DialogStep("ask-a")
    public String askA(@Text String input, DialogContext ctx) {
        Double a = parse(input);
        if (a == null) {
            return "Это не число. Введите число A:";
        }
        ctx.set("a", a);

        String op = ctx.get("op");
        if ("sqrt".equals(op)) {           // ветвь унарной операции — второе число не нужно
            ctx.finish();
            return "√%s = %s".formatted(fmt(a), fmt(Math.sqrt(a)));
        }
        ctx.next("ask-b");                 // ветвь бинарной операции — спрашиваем B
        return "Введите число B:";
    }

    /**
     * Шаг ввода числа B. Считает результат бинарной операции и завершает диалог.
     *
     * @param input ввод пользователя (число B)
     * @param ctx   контекст диалога
     * @return итоговый результат
     */
    @DialogStep("ask-b")
    public String askB(@Text String input, DialogContext ctx) {
        Double b = parse(input);
        if (b == null) {
            return "Это не число. Введите число B:";
        }
        double a = ctx.<Double>get("a");
        String op = ctx.get("op");
        Double result = compute(a, b, op);
        ctx.finish();
        if (result == null) {
            return "Деление на ноль невозможно. Диалог завершён, наберите /calc заново.";
        }
        return "%s %s %s = %s".formatted(fmt(a), symbol(op), fmt(b), fmt(result));
    }

    private String normalizeOp(String input) {
        return switch (input.strip().toLowerCase(Locale.ROOT)) {
            case "+", "plus" -> "add";
            case "-", "−", "minus" -> "sub";
            case "*", "x", "×", "mul" -> "mul";
            case "/", "÷", "div" -> "div";
            case "√", "sqrt", "корень" -> "sqrt";
            default -> null;
        };
    }

    private Double compute(double a, double b, String op) {
        return switch (op) {
            case "add" -> a + b;
            case "sub" -> a - b;
            case "mul" -> a * b;
            case "div" -> b == 0 ? null : a / b;
            default -> null;
        };
    }

    private String symbol(String op) {
        return switch (op) {
            case "add" -> "+";
            case "sub" -> "−";
            case "mul" -> "×";
            case "div" -> "÷";
            default -> "?";
        };
    }

    private Double parse(String input) {
        try {
            return Double.valueOf(input.strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String fmt(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }
}
