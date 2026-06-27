package ru.wds.smp.wdstelegrambotlib.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import ru.wds.smp.wdstelegrambotlib.core.TelegramBotSender;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogStateStore;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogUpdateHandler;
import ru.wds.smp.wdstelegrambotlib.dialog.InMemoryDialogStateStore;
import ru.wds.smp.wdstelegrambotlib.dialog.NoOpDialogStateStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Тесты авто-конфигурации стартера через {@link ApplicationContextRunner} — без
 * запуска полноценного приложения и без обращения к сети.
 *
 * <p>Транспорт long polling подменяется моком
 * ({@link TelegramBotsLongPollingApplication}), поэтому {@code TelegramBotLifecycle}
 * при старте контекста не делает реальных вызовов к Telegram.</p>
 */
class TelegramBotAutoConfigurationTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TelegramBotAutoConfiguration.class))
            // Мок транспорта: registerBot/unregisterBot ничего не делают, сети нет.
            .withBean(TelegramBotsLongPollingApplication.class,
                    () -> mock(TelegramBotsLongPollingApplication.class));

    @Test
    void inertWithoutToken() {
        // Без telegram.bot.token авто-конфигурация выключена целиком.
        runner.run(context -> assertThat(context).doesNotHaveBean(TelegramBotSender.class));
    }

    @Test
    void activatesWithToken() {
        runner.withPropertyValues("telegram.bot.token=123:ABC")
                .run(context -> assertThat(context).hasSingleBean(TelegramBotSender.class));
    }

    @Test
    void dialogsEnabledByDefault() {
        // Диалоги работают «из коробки»: создаётся звено и in-memory хранилище.
        runner.withPropertyValues("telegram.bot.token=123:ABC")
                .run(context -> {
                    assertThat(context).hasSingleBean(DialogUpdateHandler.class);
                    assertThat(context).getBean(DialogStateStore.class)
                            .isInstanceOf(InMemoryDialogStateStore.class);
                });
    }

    @Test
    void dialogsCanBeDisabledExplicitly() {
        runner.withPropertyValues("telegram.bot.token=123:ABC", "telegram.bot.dialog.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DialogUpdateHandler.class);
                    assertThat(context).getBean(DialogStateStore.class)
                            .isInstanceOf(NoOpDialogStateStore.class);
                });
    }
}
