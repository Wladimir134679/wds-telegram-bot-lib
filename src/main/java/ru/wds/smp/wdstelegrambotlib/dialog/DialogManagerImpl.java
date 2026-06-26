package ru.wds.smp.wdstelegrambotlib.dialog;

import org.telegram.telegrambots.meta.api.objects.User;
import ru.wds.smp.wdstelegrambotlib.command.CommandInvocation;

/**
 * Реализация {@link DialogManager} поверх {@link DialogExecutor} и
 * {@link DialogStateStore}.
 */
public class DialogManagerImpl implements DialogManager {

    private final DialogRegistry registry;
    private final DialogStateStore store;
    private final DialogExecutor executor;

    public DialogManagerImpl(DialogRegistry registry, DialogStateStore store, DialogExecutor executor) {
        this.registry = registry;
        this.store = store;
        this.executor = executor;
    }

    @Override
    public void start(CommandInvocation source, String dialogName) {
        DialogDefinition dialog = registry.find(dialogName);
        if (dialog == null) {
            throw new IllegalArgumentException("Диалог '" + dialogName + "' не зарегистрирован");
        }
        Long chatId = source.getChatId();
        User user = source.getUser();
        if (chatId == null || user == null || user.getId() == null) {
            throw new IllegalStateException("Невозможно запустить диалог '" + dialogName
                    + "': неизвестен чат или пользователь");
        }
        DialogKey key = new DialogKey(chatId, user.getId());
        executor.start(source.getSender(), source.getUpdate(), key, dialog, "");
    }

    @Override
    public boolean isActive(long chatId, long userId) {
        return store.find(new DialogKey(chatId, userId)).isPresent();
    }

    @Override
    public void cancel(long chatId, long userId) {
        store.remove(new DialogKey(chatId, userId));
    }
}
