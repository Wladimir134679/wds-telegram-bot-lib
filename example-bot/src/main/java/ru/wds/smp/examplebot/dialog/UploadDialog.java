package ru.wds.smp.examplebot.dialog;

import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogContext;
import ru.wds.smp.wdstelegrambotlib.dialog.annotation.Dialog;
import ru.wds.smp.wdstelegrambotlib.dialog.annotation.DialogStart;
import ru.wds.smp.wdstelegrambotlib.dialog.annotation.DialogStep;

/**
 * Демонстрация приёма вложений в диалоге: фото/файл/голосовое инъектируются прямо
 * в шаг общими медиа-резолверами (тот же механизм работает и в обычных командах).
 *
 * <p>Сценарий: {@code /upload} просит прислать вложение; шаг {@code await-file}
 * получает {@link PhotoSize}/{@link Document}/{@link Voice} (null, если данного типа
 * нет) и отвечает, что именно распознал.</p>
 */
@Dialog({"upload", "📎 Загрузить"})
public class UploadDialog {

    /**
     * Старт: просьба прислать вложение.
     *
     * @param ctx контекст диалога
     * @return приглашение
     */
    @DialogStart
    public String start(DialogContext ctx) {
        ctx.next("await-file");
        return "📎 Пришлите фото, файл или голосовое сообщение.";
    }

    /**
     * Шаг приёма вложения. Библиотека сама достаёт нужный тип из сообщения.
     *
     * @param photo    наибольшее фото, если прислано фото (иначе {@code null})
     * @param document файл, если прислан документ (иначе {@code null})
     * @param voice    голосовое, если прислано (иначе {@code null})
     * @param ctx      контекст диалога
     * @return что распознано
     */
    @DialogStep("await-file")
    public String onFile(PhotoSize photo, Document document, Voice voice, DialogContext ctx) {
        ctx.finish();
        if (photo != null) {
            return "Фото принято: %dx%d, fileId=%s".formatted(photo.getWidth(), photo.getHeight(), photo.getFileId());
        }
        if (document != null) {
            return "Файл принят: %s (%s байт)".formatted(document.getFileName(), document.getFileSize());
        }
        if (voice != null) {
            return "Голосовое принято: %d сек".formatted(voice.getDuration());
        }
        // Не вложение — остаёмся бы на шаге, но для простоты завершаем подсказкой.
        return "Это не вложение. Наберите /upload и пришлите фото, файл или голосовое.";
    }
}
