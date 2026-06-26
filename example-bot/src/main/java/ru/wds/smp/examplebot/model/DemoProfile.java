package ru.wds.smp.examplebot.model;

/**
 * Демонстрационные «большие» данные, которые не влезают в {@code callback_data}
 * (64 байта) и потому сохраняются на сервере через {@code ctx.save(...)}, а в
 * кнопку кладётся лишь короткая ссылка.
 *
 * @param userId      идентификатор пользователя
 * @param displayName отображаемое имя
 * @param note        произвольная заметка (намеренно длинная — иллюстрирует, что
 *                    payload может быть любого размера)
 */
public record DemoProfile(long userId, String displayName, String note) {
}
