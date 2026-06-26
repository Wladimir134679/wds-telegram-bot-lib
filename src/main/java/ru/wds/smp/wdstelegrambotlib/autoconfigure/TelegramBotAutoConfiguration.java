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
import ru.wds.smp.wdstelegrambotlib.command.ArgumentBinder;
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
import ru.wds.smp.wdstelegrambotlib.command.resolver.IdArgumentResolver;
import ru.wds.smp.wdstelegrambotlib.command.resolver.MediaArgumentResolver;
import ru.wds.smp.wdstelegrambotlib.command.resolver.ParamArgumentResolver;
import ru.wds.smp.wdstelegrambotlib.command.resolver.PayloadArgumentResolver;
import ru.wds.smp.wdstelegrambotlib.command.resolver.TextArgumentResolver;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogCallbackHandler;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogContextArgumentResolver;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogExecutor;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogManager;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogManagerImpl;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogRegistry;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogStateStore;
import ru.wds.smp.wdstelegrambotlib.dialog.DialogUpdateHandler;
import ru.wds.smp.wdstelegrambotlib.dialog.FallbackUpdateHandler;
import ru.wds.smp.wdstelegrambotlib.dialog.InMemoryDialogStateStore;
import ru.wds.smp.wdstelegrambotlib.dialog.NoOpDialogStateStore;
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
     * Резолвер идентификаторов {@code @ChatId}/{@code @UserId} — общий для команд,
     * callback и диалогов.
     */
    @Bean
    @ConditionalOnMissingBean
    public IdArgumentResolver idArgumentResolver() {
        return new IdArgumentResolver();
    }

    /**
     * Резолвер вложений сообщения по типу (фото, видео, голос, файлы) — общий для
     * команд и шагов диалога.
     */
    @Bean
    @ConditionalOnMissingBean
    public MediaArgumentResolver mediaArgumentResolver() {
        return new MediaArgumentResolver();
    }

    /**
     * Единый «binder» аргументов: общая точка построения привязок параметров для
     * команд и диалогов. Собирает все {@link CommandArgumentResolver} из контекста.
     */
    @Bean
    @ConditionalOnMissingBean
    public ArgumentBinder argumentBinder(ObjectProvider<CommandArgumentResolver> resolvers) {
        return new ArgumentBinder(resolvers.orderedStream().toList());
    }

    /**
     * Реестр команд: строит карту команд при старте, используя общий
     * {@link ArgumentBinder}.
     */
    @Bean
    @ConditionalOnMissingBean
    public CommandRegistry commandRegistry(ApplicationContext applicationContext, ArgumentBinder argumentBinder) {
        return new CommandRegistry(applicationContext, argumentBinder);
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
     * Звено цепочки, маршрутизирующее текстовые команды. Зависит от
     * {@link DialogStateStore} (реального либо no-op), чтобы сбрасывать активный
     * диалог при совпадении обычной команды.
     */
    @Bean
    @ConditionalOnMissingBean
    public CommandUpdateHandler commandUpdateHandler(CommandRegistry commandRegistry,
                                                     CommandParser commandParser,
                                                     CommandReturnValueHandler commandReturnValueHandler,
                                                     CallbackPayloadStore callbackPayloadStore,
                                                     DialogStateStore dialogStateStore) {
        return new CommandUpdateHandler(commandRegistry, commandParser, commandReturnValueHandler,
                callbackPayloadStore, dialogStateStore);
    }

    // ---------------------------------------------------------------------
    // Диалоги (пошаговые сценарии с состоянием и TTL)
    // ---------------------------------------------------------------------

    /**
     * Резолвер параметров типа {@code DialogContext}. Регистрируется всегда —
     * безвреден и при выключенных диалогах (в обычных командах вернёт {@code null}).
     */
    @Bean
    @ConditionalOnMissingBean
    public DialogContextArgumentResolver dialogContextArgumentResolver() {
        return new DialogContextArgumentResolver();
    }

    /**
     * Хранилище диалоговых сессий в памяти с TTL — когда диалоги включены
     * ({@code telegram.bot.dialog.enabled=true}). Закрывается при остановке контекста.
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(DialogStateStore.class)
    @ConditionalOnProperty(prefix = "telegram.bot.dialog", name = "enabled", havingValue = "true")
    public DialogStateStore dialogStateStore(TelegramBotProperties properties) {
        return new InMemoryDialogStateStore(properties.getDialog().getTtl());
    }

    /**
     * No-op хранилище диалоговых сессий — когда диалоги выключены (по умолчанию).
     * Нужно, чтобы {@link CommandUpdateHandler} мог безусловно сбрасывать диалог.
     */
    @Bean
    @ConditionalOnMissingBean(DialogStateStore.class)
    @ConditionalOnProperty(prefix = "telegram.bot.dialog", name = "enabled", havingValue = "false", matchIfMissing = true)
    public DialogStateStore noOpDialogStateStore() {
        return new NoOpDialogStateStore();
    }

    /**
     * Реестр диалогов: сканирует {@code @Dialog}-бины и кэширует шаги через общий
     * {@link ArgumentBinder}. Создаётся только при включённых диалогах.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "telegram.bot.dialog", name = "enabled", havingValue = "true")
    public DialogRegistry dialogRegistry(ApplicationContext applicationContext, ArgumentBinder argumentBinder) {
        return new DialogRegistry(applicationContext, argumentBinder);
    }

    /**
     * Общее ядро исполнения диалогов (запуск старта/шага, применение исхода).
     * Используется и звеном маршрутизации, и программным {@link DialogManager}.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "telegram.bot.dialog", name = "enabled", havingValue = "true")
    public DialogExecutor dialogExecutor(DialogRegistry dialogRegistry,
                                         DialogStateStore dialogStateStore,
                                         CommandReturnValueHandler commandReturnValueHandler,
                                         CallbackPayloadStore callbackPayloadStore) {
        return new DialogExecutor(dialogRegistry, dialogStateStore, commandReturnValueHandler, callbackPayloadStore);
    }

    /**
     * Звено цепочки, обслуживающее диалоги (приоритет {@code DIALOG_PROCESSING}).
     * Только маршрутизация; исполнение делегирует {@link DialogExecutor}. Создаётся
     * только при включённых диалогах.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "telegram.bot.dialog", name = "enabled", havingValue = "true")
    public DialogUpdateHandler dialogUpdateHandler(DialogRegistry dialogRegistry,
                                                   DialogStateStore dialogStateStore,
                                                   DialogExecutor dialogExecutor) {
        return new DialogUpdateHandler(dialogRegistry, dialogStateStore, dialogExecutor);
    }

    /**
     * Звено цепочки, направляющее нажатия inline-кнопок в активный диалог
     * ({@code @DialogCallback}). Приоритет — раньше глобальных callback-команд.
     * Создаётся только при включённых диалогах.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "telegram.bot.dialog", name = "enabled", havingValue = "true")
    public DialogCallbackHandler dialogCallbackHandler(CallbackCodec callbackCodec,
                                                       DialogStateStore dialogStateStore,
                                                       DialogExecutor dialogExecutor) {
        return new DialogCallbackHandler(callbackCodec, dialogStateStore, dialogExecutor);
    }

    /**
     * Программное управление диалогами ({@link DialogManager}) — позволяет обычной
     * команде запустить диалог. Создаётся только при включённых диалогах.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "telegram.bot.dialog", name = "enabled", havingValue = "true")
    public DialogManager dialogManager(DialogRegistry dialogRegistry,
                                       DialogStateStore dialogStateStore,
                                       DialogExecutor dialogExecutor) {
        return new DialogManagerImpl(dialogRegistry, dialogStateStore, dialogExecutor);
    }

    /**
     * Заглушка для необработанных сообщений. Создаётся только при заданном непустом
     * {@code telegram.bot.fallback.message}.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "telegram.bot.fallback", name = "message")
    public FallbackUpdateHandler fallbackUpdateHandler(TelegramBotProperties properties) {
        return new FallbackUpdateHandler(properties.getFallback().getMessage(),
                properties.getFallback().isPrivateOnly());
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
