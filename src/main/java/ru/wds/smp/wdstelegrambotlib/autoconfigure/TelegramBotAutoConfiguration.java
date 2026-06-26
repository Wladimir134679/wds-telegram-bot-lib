package ru.wds.smp.wdstelegrambotlib.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.wds.smp.wdstelegrambotlib.core.TelegramBotLifecycle;
import ru.wds.smp.wdstelegrambotlib.core.TelegramBotSender;
import ru.wds.smp.wdstelegrambotlib.core.TelegramUpdateDispatcher;
import ru.wds.smp.wdstelegrambotlib.handler.LoggingUpdateHandler;
import ru.wds.smp.wdstelegrambotlib.handler.UpdateHandler;

import java.util.List;

/**
 * Авто-конфигурация библиотеки Telegram-бота — настоящий Spring Boot starter.
 *
 * <p>В отличие от старого {@code InitTelegramBotStarter}, здесь нет
 * {@code @ComponentScan}/{@code @EntityScan}, навязывающих потребителю сканирование
 * пакетов. Все бины объявлены явно через {@code @Bean} и снабжены
 * {@link ConditionalOnMissingBean}, чтобы потребитель мог переопределить любой из
 * них своей реализацией.</p>
 *
 * <p><b>Активация.</b> Конфигурация подключается только при наличии классов
 * транспорта на classpath и заданном свойстве {@code telegram.bot.token}. Без
 * токена библиотека инертна — ни один бин не создаётся.</p>
 *
 * <p>Регистрируется через
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports},
 * а не через {@code spring.factories}.</p>
 */
@AutoConfiguration
@ConditionalOnClass({TelegramBotsLongPollingApplication.class, TelegramClient.class})
@ConditionalOnProperty(prefix = "telegram.bot", name = "token")
@EnableConfigurationProperties(TelegramBotProperties.class)
public class TelegramBotAutoConfiguration {

    /**
     * Клиент Telegram API для исходящих вызовов. Создаётся фабрикой по токену —
     * отдельный бин, чтобы его можно было подменить (в т.ч. на мок в тестах).
     */
    @Bean
    @ConditionalOnMissingBean
    public TelegramClient telegramClient(TelegramBotProperties properties) {
        return new OkHttpTelegramClient(properties.getToken());
    }

    /**
     * Абстракция отправки сообщений и выполнения методов API.
     */
    @Bean
    @ConditionalOnMissingBean
    public TelegramBotSender telegramBotSender(TelegramClient telegramClient) {
        return new TelegramBotSender(telegramClient);
    }

    /**
     * Диспетчер входящих апдейтов: собирает все бины {@link UpdateHandler} из
     * контекста в цепочку (в порядке {@code @Order}/{@link UpdateHandler#getOrder()}).
     */
    @Bean
    @ConditionalOnMissingBean
    public TelegramUpdateDispatcher telegramUpdateDispatcher(TelegramBotSender sender,
                                                             ObjectProvider<UpdateHandler> handlers) {
        List<UpdateHandler> ordered = handlers.orderedStream().toList();
        return new TelegramUpdateDispatcher(sender, ordered);
    }

    /**
     * Транспорт long polling. Делается бином с {@code destroyMethod="close"},
     * чтобы корректно закрываться при остановке контекста.
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public TelegramBotsLongPollingApplication telegramBotsLongPollingApplication() {
        return new TelegramBotsLongPollingApplication();
    }

    /**
     * Управление жизненным циклом бота (регистрация/остановка) через
     * {@link org.springframework.context.SmartLifecycle}.
     */
    @Bean
    @ConditionalOnMissingBean
    public TelegramBotLifecycle telegramBotLifecycle(TelegramBotsLongPollingApplication botsApplication,
                                                     TelegramUpdateDispatcher dispatcher,
                                                     TelegramBotProperties properties) {
        return new TelegramBotLifecycle(botsApplication, dispatcher, properties);
    }

    /**
     * Встроенный обработчик-логгер. Включается свойством
     * {@code telegram.bot.log-updates} (по умолчанию — включён).
     */
    @Bean
    @ConditionalOnProperty(prefix = "telegram.bot", name = "log-updates", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public LoggingUpdateHandler loggingUpdateHandler() {
        return new LoggingUpdateHandler();
    }
}
