package ru.wds.smp.wdstelegrambotlib.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.wds.smp.wdstelegrambotlib.command.CallbackUpdateHandler;
import ru.wds.smp.wdstelegrambotlib.command.CommandParser;
import ru.wds.smp.wdstelegrambotlib.command.CommandRegistry;
import ru.wds.smp.wdstelegrambotlib.command.CommandReturnValueHandler;
import ru.wds.smp.wdstelegrambotlib.command.CommandUpdateHandler;
import ru.wds.smp.wdstelegrambotlib.command.SystemCallbackHandler;
import ru.wds.smp.wdstelegrambotlib.command.callback.CallbackCodec;
import ru.wds.smp.wdstelegrambotlib.command.callback.CallbackPayloadStore;
import ru.wds.smp.wdstelegrambotlib.command.callback.InMemoryCallbackPayloadStore;
import ru.wds.smp.wdstelegrambotlib.command.resolver.CommandArgumentResolver;
import ru.wds.smp.wdstelegrambotlib.command.resolver.ContextArgumentResolver;
import ru.wds.smp.wdstelegrambotlib.command.resolver.ParamArgumentResolver;
import ru.wds.smp.wdstelegrambotlib.command.resolver.PayloadArgumentResolver;
import ru.wds.smp.wdstelegrambotlib.command.resolver.TextArgumentResolver;
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

    // ---------------------------------------------------------------------
    // Маршрутизация команд (MVC-подобный слой)
    // ---------------------------------------------------------------------

    /**
     * Парсер текста сообщения в команду; знает username бота для отрезания
     * суффикса {@code @имя_бота}.
     */
    @Bean
    @ConditionalOnMissingBean
    public CommandParser commandParser(TelegramBotProperties properties) {
        return new CommandParser(properties.getUsername());
    }

    /**
     * Резолвер «хвоста» текстовой команды {@code @Text}.
     */
    @Bean
    @ConditionalOnMissingBean
    public TextArgumentResolver textArgumentResolver() {
        return new TextArgumentResolver();
    }

    /**
     * Резолвер именованных параметров callback {@code @Param}.
     */
    @Bean
    @ConditionalOnMissingBean
    public ParamArgumentResolver paramArgumentResolver() {
        return new ParamArgumentResolver();
    }

    /**
     * Резолвер контекстных параметров по типу (Update, Message, Chat, User и т.д.).
     */
    @Bean
    @ConditionalOnMissingBean
    public ContextArgumentResolver contextArgumentResolver() {
        return new ContextArgumentResolver();
    }

    /**
     * Реестр команд: собирает все {@link CommandArgumentResolver} из контекста и
     * строит карту команд при старте.
     */
    @Bean
    @ConditionalOnMissingBean
    public CommandRegistry commandRegistry(ApplicationContext applicationContext,
                                           ObjectProvider<CommandArgumentResolver> resolvers) {
        return new CommandRegistry(applicationContext, resolvers.orderedStream().toList());
    }

    /**
     * Обработчик возвращаемого значения метода-команды (текст, {@code BotApiMethod} и т.п.).
     */
    @Bean
    @ConditionalOnMissingBean
    public CommandReturnValueHandler commandReturnValueHandler() {
        return new CommandReturnValueHandler();
    }

    /**
     * Звено цепочки, маршрутизирующее текстовые команды (приоритет — последний).
     */
    @Bean
    @ConditionalOnMissingBean
    public CommandUpdateHandler commandUpdateHandler(CommandRegistry commandRegistry,
                                                     CommandParser commandParser,
                                                     CommandReturnValueHandler commandReturnValueHandler,
                                                     CallbackPayloadStore callbackPayloadStore) {
        return new CommandUpdateHandler(commandRegistry, commandParser, commandReturnValueHandler, callbackPayloadStore);
    }

    // ---------------------------------------------------------------------
    // Callback-кнопки и хранилище «больших» данных
    // ---------------------------------------------------------------------

    /**
     * Внутренний кодек {@code callback_data} (лимит 64 байта). Используется
     * билдерами клавиатур и хендлерами callback.
     */
    @Bean
    @ConditionalOnMissingBean
    public CallbackCodec callbackCodec() {
        return new CallbackCodec();
    }

    /**
     * Хранилище «больших» данных для callback-кнопок с TTL-очисткой. Закрывается
     * при остановке контекста (фоновый поток очистки).
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public CallbackPayloadStore callbackPayloadStore(TelegramBotProperties properties) {
        return new InMemoryCallbackPayloadStore(properties.getCallback().getPayloadTtl());
    }

    /**
     * Резолвер параметров {@code @Payload} — достаёт «большие» данные из хранилища.
     */
    @Bean
    @ConditionalOnMissingBean
    public PayloadArgumentResolver payloadArgumentResolver(CallbackPayloadStore callbackPayloadStore) {
        return new PayloadArgumentResolver(callbackPayloadStore);
    }

    /**
     * Предобрабатывающее звено для системных callback (например, {@code Callback.close()}).
     */
    @Bean
    @ConditionalOnMissingBean
    public SystemCallbackHandler systemCallbackHandler(CallbackCodec callbackCodec) {
        return new SystemCallbackHandler(callbackCodec);
    }

    /**
     * Звено цепочки, маршрутизирующее нажатия inline-кнопок (callback-команды).
     */
    @Bean
    @ConditionalOnMissingBean
    public CallbackUpdateHandler callbackUpdateHandler(CommandRegistry commandRegistry,
                                                       CallbackCodec callbackCodec,
                                                       CommandReturnValueHandler commandReturnValueHandler,
                                                       CallbackPayloadStore callbackPayloadStore) {
        return new CallbackUpdateHandler(commandRegistry, callbackCodec, commandReturnValueHandler, callbackPayloadStore);
    }
}
