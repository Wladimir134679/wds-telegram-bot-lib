package ru.wds.smp.wdstelegrambotlib.dialog;

import java.util.Optional;

/**
 * Пустая реализация {@link DialogStateStore} для случая, когда слой диалогов
 * выключен ({@code telegram.bot.dialog.enabled=false}).
 *
 * <p>Ничего не хранит: {@link #find(DialogKey)} всегда пуст, поэтому диалог никогда
 * не считается активным. Нужна, чтобы остальные бины (например,
 * {@link ru.wds.smp.wdstelegrambotlib.command.CommandUpdateHandler}, который
 * сбрасывает диалог при новой команде) могли безусловно обращаться к стору без
 * null-проверок.</p>
 */
public class NoOpDialogStateStore implements DialogStateStore {

    @Override
    public Optional<DialogState> find(DialogKey key) {
        return Optional.empty();
    }

    @Override
    public DialogState start(DialogKey key, String dialogName) {
        // Диалоги отключены — состояние не хранится; возвращаем «одноразовый» объект.
        return new DialogState(dialogName);
    }

    @Override
    public void save(DialogKey key, DialogState state) {
        // no-op
    }

    @Override
    public void remove(DialogKey key) {
        // no-op
    }
}
