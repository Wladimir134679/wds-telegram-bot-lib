package ru.wds.smp.wdstelegrambotlib.dialog;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.wds.smp.wdstelegrambotlib.core.TelegramBotSender;
import ru.wds.smp.wdstelegrambotlib.handler.HandlerPriority;
import ru.wds.smp.wdstelegrambotlib.handler.UpdateHandler;

import java.util.Locale;
import java.util.Optional;

/**
 * Звено цепочки, обслуживающее диалоги — выполняется после маршрутизации обычных
 * команд ({@link HandlerPriority#DIALOG_PROCESSING}).
 *
 * <p>Только маршрутизация; исполнение шагов и изменение состояния делегированы
 * {@link DialogExecutor} (его же использует {@link DialogManager} при программном
 * запуске). Логика приоритетов:</p>
 * <ol>
 *   <li>обычная команда уже отработала раньше и, если совпала, прервала цепочку и
 *       сбросила диалог — «новая команда побеждает»;</li>
 *   <li>сюда апдейт доходит, если команда не совпала. Текст — триггер диалога?
 *       Запускаем диалог (заменяя активный), прерываем цепочку;</li>
 *   <li>иначе активен диалог? Передаём сообщение текущему шагу, прерываем цепочку;</li>
 *   <li>иначе пропускаем дальше (к {@link FallbackUpdateHandler}).</li>
 * </ol>
 */
@Slf4j
public class DialogUpdateHandler implements UpdateHandler {

    private final DialogRegistry registry;
    private final DialogStateStore store;
    private final DialogExecutor executor;

    public DialogUpdateHandler(DialogRegistry registry, DialogStateStore store, DialogExecutor executor) {
        this.registry = registry;
        this.store = store;
        this.executor = executor;
    }

    @Override
    public boolean handle(TelegramBotSender sender, Update update) {
        if (!update.hasMessage()) {
            return true;
        }
        Message message = update.getMessage();
        DialogKey key = keyOf(message);
        if (key == null) {
            return true; // нет чата/пользователя — диалог вести не с кем
        }
        String text = message.getText();
        boolean hasText = text != null && !text.isBlank();

        // 1. Текст — триггер диалога? Запускаем (заменяя активный) — «команда побеждает».
        // (Медиа-сообщения без текста триггером быть не могут, но в активный шаг попадают.)
        if (hasText) {
            DialogDefinition triggered = registry.findByTrigger(normalize(text));
            if (triggered != null) {
                executor.start(sender, update, key, triggered, tailAfterTrigger(text));
                return false;
            }
        }

        // 2. Есть активный диалог? Передаём сообщение его текущему шагу.
        Optional<DialogState> active = store.find(key);
        if (active.isPresent()) {
            boolean handled = executor.step(sender, update, key, active.get());
            return !handled; // шаг выполнен → прерываем цепочку; шаг не найден → пропускаем дальше
        }

        // 3. Не триггер и диалога нет — пропускаем дальше.
        return true;
    }

    @Override
    public int getOrder() {
        return HandlerPriority.DIALOG_PROCESSING;
    }

    private DialogKey keyOf(Message message) {
        Chat chat = message.getChat();
        User from = message.getFrom();
        if (chat == null || chat.getId() == null || from == null || from.getId() == null) {
            return null;
        }
        return new DialogKey(chat.getId(), from.getId());
    }

    private String normalize(String text) {
        String name = text.strip();
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        return name.toLowerCase(Locale.ROOT);
    }

    /** «Хвост» после имени-триггера — для {@code @Text} в {@code @DialogStart}. */
    private String tailAfterTrigger(String text) {
        String stripped = text.strip();
        int space = stripped.indexOf(' ');
        return space < 0 ? "" : stripped.substring(space + 1).strip();
    }
}
