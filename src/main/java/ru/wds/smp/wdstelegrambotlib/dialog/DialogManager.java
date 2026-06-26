package ru.wds.smp.wdstelegrambotlib.dialog;

import ru.wds.smp.wdstelegrambotlib.command.CommandInvocation;

/**
 * Программное управление диалогами — мостик «команда, которая продолжилась».
 *
 * <p>Инъектируется как обычный бин в контроллеры команд. Позволяет запустить диалог
 * не только командой-триггером {@code @Dialog}, но и из кода любой
 * {@code @CommandMapping}-команды или обработчика callback-кнопки. Так выражается
 * идея «обычная команда плавно переходит в пошаговый сценарий».</p>
 *
 * <p>Доступен только при включённом слое диалогов
 * ({@code telegram.bot.dialog.enabled=true}).</p>
 */
public interface DialogManager {

    /**
     * Запускает диалог для пользователя текущего апдейта и сразу выполняет его
     * {@code @DialogStart} (приветствие/первый вопрос отправляются как обычный ответ).
     * Любой ранее активный диалог заменяется.
     *
     * <p>Чат и пользователь берутся из {@code source}. Вызывающая команда может
     * после этого вернуть {@code null} — первое сообщение диалога отправит сам старт.</p>
     *
     * @param source     контекст текущего вызова (команды/callback), откуда берутся
     *                   чат, пользователь, апдейт и отправитель
     * @param dialogName имя (ключ) диалога — первое имя из его {@code @Dialog}
     * @throws IllegalArgumentException если диалог с таким именем не зарегистрирован
     * @throws IllegalStateException    если в {@code source} неизвестны чат или пользователь
     */
    void start(CommandInvocation source, String dialogName);

    /**
     * @param chatId идентификатор чата
     * @param userId идентификатор пользователя
     * @return {@code true}, если у пользователя есть активный (не истёкший) диалог
     */
    boolean isActive(long chatId, long userId);

    /**
     * Принудительно завершает (отменяет) активный диалог пользователя, если он есть.
     *
     * @param chatId идентификатор чата
     * @param userId идентификатор пользователя
     */
    void cancel(long chatId, long userId);
}
