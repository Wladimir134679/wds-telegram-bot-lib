package ru.wds.smp.wdstelegrambotlib.dialog;

/**
 * Ключ диалоговой сессии: пара «чат + пользователь».
 *
 * <p>Диалог ведётся отдельно для каждого пользователя в каждом чате — в группах
 * параллельные мастера разных участников не пересекаются. В личных чатах
 * {@code chatId} и {@code userId} совпадают.</p>
 *
 * @param chatId идентификатор чата
 * @param userId идентификатор пользователя-инициатора
 */
public record DialogKey(long chatId, long userId) {
}
