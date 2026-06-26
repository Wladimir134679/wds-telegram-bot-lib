package ru.wds.smp.wdstelegrambotlib.dialog;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Внутреннее изменяемое состояние одного диалога пользователя.
 *
 * <p>Хранится в {@link DialogStateStore} и переживает между апдейтами в пределах
 * TTL. Прикладной код напрямую с этим классом не работает — он взаимодействует
 * с диалогом через {@link DialogContext}, который оборачивает это состояние.</p>
 *
 * <p><b>Потокобезопасность.</b> Транспорт long polling по умолчанию однопоточный,
 * но карта данных и история сделаны потокобезопасными «на всякий случай», чтобы
 * параллельная фоновая очистка стора и возможная многопоточная отправка не
 * приводили к гонкам при чтении.</p>
 */
public class DialogState {

    /** Максимум хранимых записей истории шагов (защита от неограниченного роста). */
    static final int MAX_HISTORY = 32;

    private final String dialogName;
    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private final Deque<String> history = new ArrayDeque<>();
    private volatile String currentStep;
    private volatile Integer anchorMessageId;

    DialogState(String dialogName) {
        this.dialogName = dialogName;
    }

    /** @return имя (ключ) запущенного диалога */
    public String dialogName() {
        return dialogName;
    }

    /**
     * @return id «якорного» сообщения — последнего сообщения диалога с клавиатурой,
     *         которое редактируется при навигации; {@code null}, если ещё не отправлено
     */
    Integer anchorMessageId() {
        return anchorMessageId;
    }

    void setAnchorMessageId(Integer anchorMessageId) {
        this.anchorMessageId = anchorMessageId;
    }

    /** @return текущий шаг автомата либо {@code null}, если шаг ещё не назначен */
    public String currentStep() {
        return currentStep;
    }

    void setCurrentStep(String step) {
        this.currentStep = step;
    }

    Map<String, Object> data() {
        return data;
    }

    /**
     * Добавляет шаг в историю (с ограничением размера). Самые старые записи
     * вытесняются.
     *
     * @param step имя шага, на который перешёл автомат
     */
    void pushHistory(String step) {
        if (step == null) {
            return;
        }
        synchronized (history) {
            history.addLast(step);
            while (history.size() > MAX_HISTORY) {
                history.removeFirst();
            }
        }
    }

    /** @return неизменяемый снимок истории шагов (от старых к новым) */
    List<String> historySnapshot() {
        synchronized (history) {
            return List.copyOf(history);
        }
    }

    /** @return неизменяемый снимок накопленных данных (для отладки/логов) */
    Map<String, Object> dataSnapshot() {
        return Collections.unmodifiableMap(Map.copyOf(data));
    }
}
