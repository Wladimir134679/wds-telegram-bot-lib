package ru.wds.smp.wdstelegrambotlib.command.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ru.wds.smp.wdstelegrambotlib.command.callback.Callback;
import ru.wds.smp.wdstelegrambotlib.command.callback.CallbackCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Беглый билдер inline-клавиатуры (кнопки под сообщением с {@code callback_data}).
 *
 * <p>Кодирование {@link Callback} в строку {@code callback_data} (и проверка лимита
 * 64 байта) выполняется автоматически — разработчик про кодек не думает.</p>
 *
 * <pre>{@code
 * InlineKeyboardMarkup kb = Keyboards.inline()
 *     .row()
 *         .button("◀️", Callback.to("nav").arg("page", page - 1).arg("rules", ref))
 *         .button("▶️", Callback.to("nav").arg("page", page + 1).arg("rules", ref))
 *     .row()
 *         .button("Открыть A", Callback.to("open").arg("id", 42))
 *         .url("Сайт", "https://example.com")
 *     .row()
 *         .button("✖ Закрыть", Callback.close())
 *     .build();
 * }</pre>
 *
 * <p>Если вызвать {@link #button} до первого {@link #row()}, строка создаётся
 * автоматически. Билдер одноразовый и не потокобезопасный.</p>
 */
public final class InlineKeyboardBuilder {

    private final CallbackCodec codec;
    private final List<InlineKeyboardRow> rows = new ArrayList<>();
    private List<InlineKeyboardButton> current;

    InlineKeyboardBuilder(CallbackCodec codec) {
        this.codec = codec;
    }

    /** Начинает новую строку кнопок. */
    public InlineKeyboardBuilder row() {
        flush();
        current = new ArrayList<>();
        return this;
    }

    /**
     * Добавляет кнопку с callback в текущую строку.
     *
     * @param text     подпись кнопки
     * @param callback данные callback (кодируются автоматически)
     */
    public InlineKeyboardBuilder button(String text, Callback callback) {
        ensureRow();
        current.add(InlineKeyboardButton.builder()
                .text(text)
                .callbackData(codec.encode(callback))
                .build());
        return this;
    }

    /**
     * Добавляет кнопку-ссылку (открывает URL, без callback).
     *
     * @param text подпись кнопки
     * @param url  адрес
     */
    public InlineKeyboardBuilder url(String text, String url) {
        ensureRow();
        current.add(InlineKeyboardButton.builder()
                .text(text)
                .url(url)
                .build());
        return this;
    }

    /** Собирает разметку. */
    public InlineKeyboardMarkup build() {
        flush();
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private void ensureRow() {
        if (current == null) {
            current = new ArrayList<>();
        }
    }

    private void flush() {
        if (current != null && !current.isEmpty()) {
            rows.add(new InlineKeyboardRow(current));
        }
        current = null;
    }
}
