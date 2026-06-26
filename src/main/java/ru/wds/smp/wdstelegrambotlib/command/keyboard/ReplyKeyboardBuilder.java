package ru.wds.smp.wdstelegrambotlib.command.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Беглый билдер reply-клавиатуры (кнопки снизу; нажатие <b>шлёт текст в чат</b>).
 *
 * <p>В отличие от inline-клавиатуры, здесь нет {@code callback_data}: подпись кнопки
 * приходит обычным сообщением. Поэтому reply-кнопку удобно роутить тем же
 * {@code @CommandMapping} с именем, равным подписи кнопки
 * (см. {@link ru.wds.smp.wdstelegrambotlib.command.CommandUpdateHandler}).</p>
 *
 * <p>Inline и reply-клавиатуры <b>нельзя совместить</b> в одном сообщении — это
 * ограничение Telegram, поэтому билдеры раздельные.</p>
 *
 * <pre>{@code
 * ReplyKeyboardMarkup kb = Keyboards.reply()
 *     .row().text("Каталог").text("Помощь")
 *     .row().contact("Поделиться телефоном")
 *     .resize().oneTime()
 *     .build();
 * }</pre>
 */
public final class ReplyKeyboardBuilder {

    private final List<KeyboardRow> rows = new ArrayList<>();
    private List<KeyboardButton> current;
    private boolean resize;
    private boolean oneTime;
    private boolean selective;

    ReplyKeyboardBuilder() {
    }

    /** Начинает новую строку кнопок. */
    public ReplyKeyboardBuilder row() {
        flush();
        current = new ArrayList<>();
        return this;
    }

    /** Добавляет обычную текстовую кнопку (нажатие отправит её подпись как сообщение). */
    public ReplyKeyboardBuilder text(String text) {
        ensureRow();
        current.add(KeyboardButton.builder().text(text).build());
        return this;
    }

    /** Кнопка запроса контакта (телефона) пользователя. */
    public ReplyKeyboardBuilder contact(String text) {
        ensureRow();
        current.add(KeyboardButton.builder().text(text).requestContact(true).build());
        return this;
    }

    /** Кнопка запроса геолокации пользователя. */
    public ReplyKeyboardBuilder location(String text) {
        ensureRow();
        current.add(KeyboardButton.builder().text(text).requestLocation(true).build());
        return this;
    }

    /** Подгонять высоту клавиатуры под число строк. */
    public ReplyKeyboardBuilder resize() {
        this.resize = true;
        return this;
    }

    /** Скрывать клавиатуру после первого нажатия. */
    public ReplyKeyboardBuilder oneTime() {
        this.oneTime = true;
        return this;
    }

    /** Показывать клавиатуру только адресатам сообщения. */
    public ReplyKeyboardBuilder selective() {
        this.selective = true;
        return this;
    }

    /** Собирает разметку. */
    public ReplyKeyboardMarkup build() {
        flush();
        return ReplyKeyboardMarkup.builder()
                .keyboard(rows)
                .resizeKeyboard(resize)
                .oneTimeKeyboard(oneTime)
                .selective(selective)
                .build();
    }

    private void ensureRow() {
        if (current == null) {
            current = new ArrayList<>();
        }
    }

    private void flush() {
        if (current != null && !current.isEmpty()) {
            rows.add(new KeyboardRow(current));
        }
        current = null;
    }
}
