package ru.wds.smp.wdstelegrambotlib.command.resolver;

import org.telegram.telegrambots.meta.api.objects.Audio;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Video;
import org.telegram.telegrambots.meta.api.objects.VideoNote;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.api.objects.games.Animation;
import org.telegram.telegrambots.meta.api.objects.location.Location;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker;
import ru.wds.smp.wdstelegrambotlib.command.CommandInvocation;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Set;

/**
 * Резолвер вложений сообщения по типу: фото, видео, голосовые, файлы и т.д.
 *
 * <p>Достаёт вложение из {@link CommandInvocation#getMessage()} по типу параметра.
 * Если в сообщении нет вложения этого типа (или сообщения нет вовсе) — возвращает
 * {@code null}. Резолвер общий, поэтому одинаково работает в обычных командах и в
 * шагах диалога — что и нужно для сценариев «пришли фото/файл на шаге диалога».</p>
 *
 * <p>Поддерживаемые типы: {@link PhotoSize} (наибольший размер из присланных),
 * {@link Document}, {@link Video}, {@link Voice}, {@link Audio}, {@link VideoNote},
 * {@link Sticker}, {@link Animation}, {@link Contact}, {@link Location}.</p>
 */
public class MediaArgumentResolver implements CommandArgumentResolver {

    private static final Set<Class<?>> SUPPORTED = Set.of(
            PhotoSize.class, Document.class, Video.class, Voice.class, Audio.class,
            VideoNote.class, Sticker.class, Animation.class, Contact.class, Location.class);

    @Override
    public boolean supports(Parameter parameter) {
        return SUPPORTED.contains(parameter.getType());
    }

    @Override
    public Object resolve(Parameter parameter, CommandInvocation invocation) {
        Message message = invocation.getMessage();
        if (message == null) {
            return null;
        }
        Class<?> type = parameter.getType();
        if (type == PhotoSize.class) {
            return largestPhoto(message.getPhoto());
        }
        if (type == Document.class) {
            return message.getDocument();
        }
        if (type == Video.class) {
            return message.getVideo();
        }
        if (type == Voice.class) {
            return message.getVoice();
        }
        if (type == Audio.class) {
            return message.getAudio();
        }
        if (type == VideoNote.class) {
            return message.getVideoNote();
        }
        if (type == Sticker.class) {
            return message.getSticker();
        }
        if (type == Animation.class) {
            return message.getAnimation();
        }
        if (type == Contact.class) {
            return message.getContact();
        }
        if (type == Location.class) {
            return message.getLocation();
        }
        return null;
    }

    @Override
    public int getOrder() {
        return 180;
    }

    /** Возвращает наибольший по разрешению вариант фото (Telegram присылает несколько размеров). */
    private PhotoSize largestPhoto(List<PhotoSize> photos) {
        if (photos == null || photos.isEmpty()) {
            return null;
        }
        PhotoSize largest = null;
        long bestArea = -1;
        for (PhotoSize photo : photos) {
            long area = (long) safe(photo.getWidth()) * safe(photo.getHeight());
            if (area >= bestArea) {
                bestArea = area;
                largest = photo;
            }
        }
        return largest;
    }

    private int safe(Integer value) {
        return value != null ? value : 0;
    }
}
