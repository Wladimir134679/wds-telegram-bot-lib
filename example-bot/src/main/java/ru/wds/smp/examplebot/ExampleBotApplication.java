package ru.wds.smp.examplebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа демонстрационного бота.
 *
 * <p>Это <b>потребитель</b> библиотеки {@code wds-telegram-bot-lib}: обычное
 * Spring Boot приложение, которое не настраивает транспорт Telegram вручную, а
 * получает его из авто-конфигурации стартера. Достаточно задать
 * {@code telegram.bot.token} (см. {@code application.yaml}) и описать
 * контроллеры команд в этом же пакете — {@code @ComponentScan} приложения
 * подхватит их, а реестр команд библиотеки построит карту маршрутизации.</p>
 *
 * <p>Контроллеры лежат в подпакете {@code controller}, кастомное звено цепочки
 * обработчиков — в {@code handler}.</p>
 */
@SpringBootApplication
public class ExampleBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleBotApplication.class, args);
    }
}
