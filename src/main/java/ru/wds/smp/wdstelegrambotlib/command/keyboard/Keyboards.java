package ru.wds.smp.wdstelegrambotlib.command.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import ru.wds.smp.wdstelegrambotlib.command.callback.CallbackCodec;

/**
 * Точка входа для построения клавиатур — единый «дизайн-стиль» библиотеки.
 *
 * <p>Два раздельных билдера, потому что Telegram не позволяет совмещать их в одном
 * сообщении:</p>
 * <ul>
 *   <li>{@link #inline()} — кнопки под сообщением с callback (навигация, действия,
 *       редактирование сообщения);</li>
 *   <li>{@link #reply()} — кнопки снизу экрана; нажатие отправляет текст в чат.</li>
 * </ul>
 *
 * <p>Билдеры не требуют Spring — их можно вызывать где угодно (кодек callback
 * создаётся внутренне и не виден снаружи).</p>
 */
public final class Keyboards {

    private static final CallbackCodec CODEC = new CallbackCodec();

    private Keyboards() {
        throw new AssertionError("Утилитный класс не предназначен для инстанцирования");
    }

    /**
     * @return новый билдер inline-клавиатуры
     */
    public static InlineKeyboardBuilder inline() {
        return new InlineKeyboardBuilder(CODEC);
    }

    /**
     * @return новый билдер reply-клавиатуры
     */
    public static ReplyKeyboardBuilder reply() {
        return new ReplyKeyboardBuilder();
    }

    /**
     * Разметка, убирающая reply-клавиатуру у пользователя.
     *
     * @return {@link ReplyKeyboardRemove}
     */
    public static ReplyKeyboardRemove removeReply() {
        return ReplyKeyboardRemove.builder().removeKeyboard(true).build();
    }
}
