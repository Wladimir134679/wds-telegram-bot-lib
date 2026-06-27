# wds-telegram-bot-lib

**Spring Boot starter для написания Telegram-ботов в стиле Spring MVC.**

Подключаешь зависимость — и пишешь «контроллеры команд» с аннотациями и инъекцией
параметров по типу, почти как `@Controller` в вебе. Маршрутизация входящих
`Update`, разбор аргументов, формирование ответов, callback-кнопки, пошаговые
диалоги и фоновые сессии с TTL — на стороне библиотеки.

Транспорт — `telegrambots-longpolling` 10.0.0 (long polling, без webhook).

---

## Содержание

- [Возможности](#возможности)
- [Требования](#требования)
- [Подключение](#подключение)
- [Конфигурация (`telegram.bot.*`)](#конфигурация-telegrambot)
- [Быстрый старт](#быстрый-старт)
- [Команды](#команды)
  - [`@BotController` и `@CommandMapping`](#botcontroller-и-commandmapping)
  - [Инъекция аргументов](#инъекция-аргументов)
  - [Возвращаемое значение метода](#возвращаемое-значение-метода)
- [Клавиатуры и callback-кнопки](#клавиатуры-и-callback-кнопки)
  - [Inline-клавиатура](#inline-клавиатура)
  - [Reply-клавиатура](#reply-клавиатура)
  - [Callback-команды и `@Param`](#callback-команды-и-param)
  - [«Большие» данные: `ctx.save(...)` + `@Payload`](#большие-данные-ctxsave--payload)
  - [Системная кнопка «Закрыть»](#системная-кнопка-закрыть)
- [Цепочка обработчиков с приоритетами](#цепочка-обработчиков-с-приоритетами)
- [Диалоги (пошаговые сценарии)](#диалоги-пошаговые-сценарии)
- [Заглушка для необработанных сообщений](#заглушка-для-необработанных-сообщений)
- [Безопасность и логи](#безопасность-и-логи)
- [Точки расширения и переопределение бинов](#точки-расширения-и-переопределение-бинов)
- [Сборка](#сборка)
- [Демо-проект](#демо-проект)

---

## Возможности

| Возможность | Кратко |
|---|---|
| Контроллеры команд | `@BotController` + `@CommandMapping` — как `@Controller`/`@RequestMapping` |
| Инъекция аргументов по типу | `Update`, `Message`, `Chat`, `User`, `TelegramBotSender`, медиа и т.д. |
| Аннотированные параметры | `@Text`, `@Param`, `@Payload`, `@ChatId`, `@UserId` |
| Гибкий ответ | вернул `String` / `SendMessage` / `EditMessageText` / коллекцию — библиотека отправит |
| Inline- и reply-клавиатуры | беглые билдеры `Keyboards.inline()` / `Keyboards.reply()` |
| Callback-кнопки | компактный кодек `callback_data` с проверкой лимита 64 байта |
| «Большие» данные в callback | серверное хранилище с TTL + `@Payload` |
| Цепочка обработчиков с приоритетами | свой `UpdateHandler` — логирование, метрики, антифлуд, доступ |
| Диалоги (визарды) | `@Dialog` / `@DialogStart` / `@DialogStep` с состоянием и TTL |
| Заглушка | ответ на сообщения, не подхваченные ни командой, ни диалогом |
| Авто-конфигурация | настоящий starter: `@AutoConfiguration` + `@ConditionalOn*`, без `@ComponentScan` |

---

## Требования

- **Java 21**
- **Spring Boot 4.1.x**
- Зависимости транспорта: `org.telegram:telegrambots-longpolling:10.0.0`,
  `org.telegram:telegrambots-client:10.0.0`
- Токен бота от [@BotFather](https://t.me/BotFather)

---

## Публикация в локальный репозиторий

Библиотека собирается обычным (не `bootJar`) jar-артефактом и публикуется плагином
`maven-publish`. Координаты артефакта:

```
ru.wds.smp:wds-telegram-bot-lib:0.0.1-SNAPSHOT
```

Чтобы положить артефакт в локальный Maven-репозиторий (`~/.m2/repository`), откуда
его подхватит `mavenLocal()` любого потребителя на той же машине:

```bash
./gradlew publishToMavenLocal
```

Публикуются три артефакта: основной `jar`, `-sources.jar` и `-javadoc.jar` (чтобы в
IDE потребителя работали навигация по коду и подсказки документации).

> Это **библиотека**, а не приложение: `bootJar` отключён, основным артефактом
> оставлен обычный `jar` — так потребитель видит классы и
> `META-INF/spring/...imports` напрямую.

Команды сборки/публикации выполняет разработчик на своей стороне (см. `CLAUDE.md`,
раздел 0).

---

## Подключение

После публикации в локальный репозиторий подключите библиотеку как обычную
зависимость:

```kotlin
repositories {
    mavenLocal()   // локальный ~/.m2, куда положил publishToMavenLocal
    mavenCentral()
}

dependencies {
    implementation("ru.wds.smp:wds-telegram-bot-lib:0.0.1-SNAPSHOT")
}
```

Типы Telegram Bot API (`Update`, `SendMessage`, `InlineKeyboardMarkup` и т.д.)
присутствуют в публичных сигнатурах методов-команд и аннотаций библиотеки, поэтому
подключены как `api` и **экспортируются потребителю транзитивно** — отдельно
объявлять `telegrambots-longpolling`/`telegrambots-client` **не нужно**.

**Альтернатива — подпроект в одной сборке Gradle** (как в `example-bot`), без
публикации:

```kotlin
// settings.gradle.kts потребителя
include("my-bot")

// build.gradle.kts модуля my-bot
dependencies {
    implementation(project(":wds-telegram-bot-lib"))
}
```

Авто-конфигурация подхватывается автоматически через
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
— **никакого ручного `@ComponentScan` по пакетам библиотеки не нужно.**

**Активация.** Библиотека инертна, пока не задан `telegram.bot.token`: без токена
не создаётся ни один бин (см. `@ConditionalOnProperty` на авто-конфигурации).

---

## Конфигурация (`telegram.bot.*`)

Все свойства — под префиксом `telegram.bot`. Типобезопасная модель —
`TelegramBotProperties`.

```yaml
telegram:
  bot:
    # Токен бота от @BotFather. ОБЯЗАТЕЛЕН — без него библиотека не активируется.
    # Берите из переменной окружения, НЕ коммитьте реальный токен.
    token: ${TELEGRAM_BOT_TOKEN:}

    # Username бота без ведущего @ (например, my_cool_bot). Необязателен.
    # Используется, чтобы отрезать суффикс @имя_бота у команд в группах.
    username: ${TELEGRAM_BOT_USERNAME:}

    # Встроенный обработчик-логгер входящих апдейтов (уровень DEBUG, без текста и PII).
    log-updates: true            # по умолчанию true

    callback:
      # Время жизни «больших» данных, сохранённых через ctx.save(...) для callback.
      payload-ttl: 10m           # Duration, по умолчанию 10 минут

    dialog:
      # Слой диалогов (пошаговые сценарии с состоянием).
      enabled: true              # по умолчанию true (работает «из коробки»)
      # Сколько живёт простаивающая диалоговая сессия до сброса.
      ttl: 5m                    # Duration, по умолчанию 5 минут

    fallback:
      # Ответ на сообщение, которое не подхватили ни команда, ни диалог.
      # Пусто/не задано = заглушка выключена.
      message: ""                # по умолчанию не задан
      # Отвечать только в личных чатах (чтобы не спамить в группах).
      private-only: true         # по умолчанию true
```

### Таблица полей

| Свойство | Тип | По умолчанию | Назначение |
|---|---|---|---|
| `telegram.bot.token` | `String` | — (обязателен) | Токен бота. Без него библиотека не активируется. Секрет, маскируется в логах. |
| `telegram.bot.username` | `String` | пусто | Username без `@`. Нужен для отрезания суффикса `@имя_бота` у команд в группах. |
| `telegram.bot.log-updates` | `boolean` | `true` | Включает встроенный `LoggingUpdateHandler` (DEBUG, без PII). |
| `telegram.bot.callback.payload-ttl` | `Duration` | `10m` | TTL «больших» данных callback. Должен быть положительным. |
| `telegram.bot.dialog.enabled` | `boolean` | `true` | Слой диалогов. Включён по умолчанию; при `false` — no-op хранилище состояний. |
| `telegram.bot.dialog.ttl` | `Duration` | `5m` | TTL простаивающей диалоговой сессии. Должен быть положительным. |
| `telegram.bot.fallback.message` | `String` | пусто | Текст заглушки. Пусто = заглушка выключена. |
| `telegram.bot.fallback.private-only` | `boolean` | `true` | Отвечать заглушкой только в личных чатах. |

> `Duration` принимает форматы Spring: `10m`, `30s`, `1h`, `500ms` и т.п. Единицы
> измерения согласованы по всей цепочке (конфиг → логи → планировщик очистки).

---

## Быстрый старт

1. Получи токен у [@BotFather](https://t.me/BotFather).
2. Задай токен (через переменную окружения, не в коде):

   ```powershell
   $env:TELEGRAM_BOT_TOKEN = "123456:ABC-DEF..."
   ```

3. Опиши `application.yaml` (минимум — токен; см. раздел конфигурации).
4. Напиши контроллер команд:

   ```java
   @BotController
   public class StartController {

       @CommandMapping("/start")
       public String start(User user) {
           return "Привет, " + user.getFirstName() + "!";
       }
   }
   ```

5. Обычное Spring Boot приложение потребителя:

   ```java
   @SpringBootApplication
   public class MyBotApplication {
       public static void main(String[] args) {
           SpringApplication.run(MyBotApplication.class, args);
       }
   }
   ```

6. Собери и запусти приложение на своей стороне, затем напиши боту `/start`.

---

## Команды

### `@BotController` и `@CommandMapping`

`@BotController` — мета-аннотация над `@Component`: помеченный класс становится
обычным Spring-бином и подхватывается сканированием **приложения-потребителя**.
Внутри него методы-обработчики помечаются `@CommandMapping`.

```java
@BotController
public class EchoController {

    // несколько алиасов; ведущий "/" необязателен; сопоставление регистронезависимое
    @CommandMapping({"say", "echo"})
    public String say(@Text String text) {
        return text;
    }
}
```

Параметры аннотации `@CommandMapping`:

| Параметр | Тип | По умолчанию | Назначение |
|---|---|---|---|
| `value` | `String[]` | — (минимум один) | Имена/алиасы команды. Ведущий `/` необязателен. Можно указать подпись reply-кнопки (например, `"📋 Меню"`). |
| `type` | `CommandType` | `MESSAGE` | `MESSAGE` — текстовая команда; `CALLBACK` — нажатие inline-кнопки. |

Нормализация имён:

- ведущий `/` отрезается — `"/start"` и `"start"` эквивалентны;
- сопоставление **регистронезависимое**;
- суффикс `@имя_бота` (как в группах) отрезается при разборе (для этого задайте
  `telegram.bot.username`);
- **конфликт имён** двух команд одного типа → библиотека падает при старте с
  понятной ошибкой (детерминированно, без «молчаливой перезаписи»).

### Инъекция аргументов

Аргументы метода резолвятся **по типу и аннотациям**, в любом порядке. Одно и то
же ядро инъекции работает в командах, callback-обработчиках и шагах диалога.

**Контекстные типы (по типу параметра, без аннотаций):**

| Тип | Что инъектируется |
|---|---|
| `Update` | Сырой апдейт Telegram |
| `Message` | Сообщение (для callback — сообщение под кнопкой) |
| `CallbackQuery` | Запрос callback (только для callback-команд) |
| `Callback` | Разобранный callback (имя + параметры кнопки) |
| `Chat` | Чат-источник |
| `User` | Пользователь-инициатор |
| `TelegramBotSender` | Абстракция отправки/выполнения методов API |
| `ParsedCommand` | Разобранная команда (имя + аргументы) |
| `CommandInvocation` | Полный контекст вызова (chatId, messageId, `save(...)` и т.д.) |
| `DialogContext` | Контекст диалога (в обычной команде вернёт `null`) |

**Медиа-вложения (по типу параметра):** `PhotoSize` (наибольший размер),
`Document`, `Video`, `Voice`, `Audio`, `VideoNote`, `Sticker`, `Animation`,
`Contact`, `Location`. Если вложения нет — `null`.

**Аннотированные параметры:**

| Аннотация | Применяется к | Источник значения |
|---|---|---|
| `@Text` | `String` | Весь «хвост» текстовой команды (для `/say привет всем` → `"привет всем"`). В шаге диалога — весь текст сообщения целиком. |
| `@Param("имя")` | примитивы/`String`/`enum` | Именованный параметр callback-кнопки (`.arg("имя", ...)`). |
| `@Payload("имя")` | любой объект | «Большие» данные из серверного хранилища по ссылке из кнопки. |
| `@ChatId` | `long`/`Long` | Идентификатор чата текущего апдейта. |
| `@UserId` | `long`/`Long` | Идентификатор пользователя-инициатора. |

`@Param` поддерживает типы: `String`, `long`/`Long`, `int`/`Integer`,
`double`/`Double`, `boolean`/`Boolean`, `enum` (по имени константы). Флаг
`required` (по умолчанию `false`): при `true` и отсутствии значения бросается
`CommandArgumentException`; при `false` — подставляется `null`/ноль.

### Возвращаемое значение метода

То, что метод вернул, библиотека интерпретирует как ответ пользователю
(`CommandReturnValueHandler`):

| Возвращаемый тип | Поведение |
|---|---|
| `void` / `null` | Ничего не отправляется (метод всё сделал сам через `TelegramBotSender`). |
| `String` / `CharSequence` | Отправляется текстом в чат-источник. |
| `BotApiMethod<?>` | Выполняется как есть (`SendMessage`, `EditMessageText`, `AnswerCallbackQuery`, …). |
| `Optional<?>` | Разворачивается (пустой — ничего). |
| `Iterable<?>` / массив | Каждый элемент обрабатывается рекурсивно (несколько ответов за вызов). |

Ошибки отправки логируются и **не пробрасываются** — сбой ответа не ломает
обработку апдейта.

---

## Клавиатуры и callback-кнопки

Единая точка входа — `Keyboards`. Два раздельных билдера, потому что Telegram не
позволяет совмещать их в одном сообщении.

### Inline-клавиатура

Кнопки под сообщением (callback или ссылки):

```java
InlineKeyboardMarkup kb = Keyboards.inline()
        .row()
            .button("➖", Callback.to("counter").arg("value", value - 1))
            .button(String.valueOf(value), Callback.to("counter").arg("value", value))
            .button("➕", Callback.to("counter").arg("value", value + 1))
        .row()
            .button("Открыть #42", Callback.to("open").arg("id", 42))
            .url("Документация", "https://core.telegram.org/bots/api")
        .row()
            .button("✖ Закрыть", Callback.close())
        .build();
```

Кодирование `Callback` в строку `callback_data` и **проверка лимита 64 байта**
выполняются автоматически — про кодек думать не нужно.

### Reply-клавиатура

Кнопки снизу экрана; нажатие отправляет подпись текстом в чат (её можно поймать
обычным `@CommandMapping`, указав подпись среди алиасов). Убрать клавиатуру —
`Keyboards.removeReply()`.

### Callback-команды и `@Param`

Кнопка несёт имя команды и именованные параметры; обработчик — метод с
`type = CommandType.CALLBACK`, параметры читаются через `@Param`:

```java
@CommandMapping(value = "counter", type = CommandType.CALLBACK)
public EditMessageText counter(@Param("value") int value, CommandInvocation ctx) {
    return EditMessageText.builder()
            .chatId(String.valueOf(ctx.getChatId()))
            .messageId(ctx.getMessageId())
            .text("Текущее значение: " + value)
            .replyMarkup(counterKeyboard(value))
            .build();
}
```

`EditMessageText` перерисовывает то же сообщение — без спама новыми сообщениями.

### «Большие» данные: `ctx.save(...)` + `@Payload`

`callback_data` ограничен 64 байтами, поэтому толстый объект туда не положить.
Сохрани данные на сервере и положи в кнопку короткую ссылку:

```java
@CommandMapping("profile")
public SendMessage profile(User user, Chat chat, CommandInvocation ctx) {
    DemoProfile demo = new DemoProfile(user.getId(), user.getFirstName(), "…большие данные…");
    String ref = ctx.save(demo); // объект → короткая ссылка (payloadId)

    return SendMessage.builder()
            .chatId(chat.getId())
            .text("Нажми, чтобы раскрыть:")
            .replyMarkup(Keyboards.inline()
                    .row().button("👤 Показать", Callback.to("profile_show").arg("ref", ref))
                    .build())
            .build();
}

@CommandMapping(value = "profile_show", type = CommandType.CALLBACK)
public String show(@Payload(value = "ref", required = true) DemoProfile profile) {
    return "id: " + profile.userId() + ", имя: " + profile.displayName();
}
```

Данные живут `telegram.bot.callback.payload-ttl` и чистятся фоново. При `required
= true` и истёкшем TTL библиотека мягко сообщит пользователю, что данные устарели
(`CallbackPayloadExpiredException`); при `required = false` — подставит `null`.

### Системная кнопка «Закрыть»

`Callback.close()` — зарезервированный callback. Библиотека сама убирает
inline-клавиатуру с сообщения, **без отдельного метода-обработчика**.

---

## Цепочка обработчиков с приоритетами

Каждый входящий `Update` прогоняется через список бинов `UpdateHandler`,
отсортированных по приоритету (`getOrder()`, семантика Spring `Ordered` — меньше
число = раньше). Новый этап обработки (логирование, метрики, антифлуд, проверка
доступа) — это просто новый бин в контексте; Spring соберёт их в общий список.

```java
@Component
public class AntiFloodHandler implements UpdateHandler {

    @Override
    public boolean handle(TelegramBotSender sender, Update update) {
        if (flooded(update)) {
            return false; // прервать цепочку — апдейт «поглощён»
        }
        return true;      // продолжить — передать следующему обработчику
    }

    @Override
    public int getOrder() {
        return HandlerPriority.SECURITY; // выполнится раньше маршрутизации команд
    }
}
```

Готовые константы приоритетов — в `HandlerPriority` (от раннего к позднему):

| Константа | Значение | Назначение |
|---|---|---|
| `SECURITY` | 100 | Бан / whitelist / антифлуд (может прервать цепочку). |
| `LOGGING` | 300 | Логирование и метрики. |
| `PRE_PROCESSING` | 500 | Предобработка, обогащение контекста. |
| `SYSTEM_CALLBACK` | 900 | Системные callback (например, «закрыть клавиатуру»). |
| `COMMAND_PROCESSING` | 1000 | Маршрутизация и вызов команд (совпавшая команда прерывает цепочку). |
| `DIALOG_PROCESSING` | 1500 | Диалоги (после обычных команд — «новая команда побеждает»). |
| `FALLBACK` | 2000 | Финальная заглушка. |
| `DEFAULT` | = `COMMAND_PROCESSING` | Приоритет по умолчанию. |

**Изоляция ошибок:** диспетчер оборачивает каждый вызов в try/catch — исключение
в одном обработчике не роняет обработку всего апдейта.

---

## Диалоги (пошаговые сценарии)

Диалог — конечный автомат «как форма-визард»: команда-триггер запускает сценарий,
последующие сообщения попадают в текущий шаг. Состояние хранится между шагами в
пределах TTL, без внешнего хранилища.

**Включён по умолчанию** (`telegram.bot.dialog.enabled: true`). Чтобы отключить
слой, явно задайте `telegram.bot.dialog.enabled: false` — тогда используется no-op
хранилище и звено диалогов не создаётся.

```java
@Dialog({"calc", "🧮 Калькулятор"})   // триггеры; первое имя — ключ диалога
public class CalcDialog {

    @DialogStart
    public String start(DialogContext ctx) {
        ctx.next("ask-a");
        return "Введите число A:";
    }

    @DialogStep("ask-a")
    public String askA(@Text String input, DialogContext ctx) {
        Double a = parse(input);
        if (a == null) {
            return "Это не число. Введите A:"; // нет перехода — остаёмся на шаге
        }
        ctx.set("a", a);
        ctx.next("ask-b");
        return "Введите число B:";
    }

    @DialogStep("ask-b")
    public String askB(@Text String input, DialogContext ctx) {
        double a = ctx.<Double>get("a");
        double b = Double.parseDouble(input);
        ctx.finish();                          // завершить, состояние очищается
        return "Сумма = " + (a + b);
    }
}
```

Аннотации:

| Аннотация | Применяется к | Назначение |
|---|---|---|
| `@Dialog({"триггер", ...})` | класс | Объявляет диалог-машину. Имена-триггеры запускают её, первое — ключ диалога. Мета-`@Component`. |
| `@DialogStart` | метод | Точка входа (ровно один на диалог). Обычно задаёт первый вопрос и `ctx.next(...)`. |
| `@DialogStep("шаг")` | метод | Шаг автомата. Один метод может обслуживать несколько шагов (перечислить имена). |

Управление автоматом через `DialogContext`:

- `ctx.next("шаг")` — перейти на шаг (ждать следующего сообщения);
- `ctx.finish()` — успешно завершить (состояние очищается);
- `ctx.cancel()` — отменить (состояние очищается);
- ничего не вызвать — остаться на текущем шаге (например, при невалидном вводе);
- `ctx.set/get/has/remove` — «корзина» данных между шагами;
- `ctx.history()` — история пройденных шагов;
- `ctx.chatId()` / `ctx.userId()` / `ctx.dialog()` / `ctx.step()`.

**Инъекция в шаги — та же**, что в командах: `@Text`, `@ChatId`/`@UserId`, медиа
(`PhotoSize`/`Document`/`Voice`/…), `Message`, `Chat`, `User`,
`TelegramBotSender`, плюс `DialogContext`. Возвращаемое значение шага — обычный
ответ пользователю.

**Маршрутизация.** Обычная команда всегда «побеждает» активный диалог (сбрасывает
его). Если команды нет, но диалог активен — сообщение идёт в текущий шаг. Не
дублируйте имена триггеров с именами обычных команд (обычная команда выигрывает).

**Запуск диалога из команды** — бин `DialogManager` (доступен при включённых
диалогах): «команда, которая продолжилась».

```java
@BotController
public class DialogLauncherController {

    private final DialogManager dialogManager;

    public DialogLauncherController(DialogManager dialogManager) {
        this.dialogManager = dialogManager;
    }

    @CommandMapping("calc2")
    public void calc2(CommandInvocation ctx) {
        dialogManager.start(ctx, "calc"); // первый вопрос отправит сам @DialogStart
    }
}
```

---

## Заглушка для необработанных сообщений

Если сообщение не подхватили ни команда, ни диалог — можно ответить заглушкой.
Задайте непустой `telegram.bot.fallback.message` (иначе бин не создаётся):

```yaml
telegram:
  bot:
    fallback:
      message: "Не понимаю. Наберите /start или /calc."
      private-only: true   # в группах молчим, чтобы не спамить
```

---

## Безопасность и логи

- **Токен — секрет.** Берите из переменной окружения, не коммитьте. В логах токен
  маскируется (`maskedToken()`), `toString()` свойств его не раскрывает.
- Встроенный логгер апдейтов пишет на **DEBUG** и **без текста сообщений и PII**.
  Не логируйте целиком `Update`, тексты пользователей и токен на уровне INFO.
- `callback_data` — компактный формат (`cmd:arg`), не толстый JSON; лимит 64 байта
  проверяется автоматически. Для крупных данных — серверное хранилище + `@Payload`.

---

## Точки расширения и переопределение бинов

Авто-конфигурация — настоящий starter: все бины объявлены через `@Bean` с
`@ConditionalOnMissingBean`. Любой можно **переопределить** своим бином того же
типа (он «выиграет»):

- `TelegramClient` — например, подменить на мок в тестах;
- `TelegramBotSender` — своя обёртка отправки;
- `CommandReturnValueHandler` — свои типы ответов;
- `CallbackPayloadStore` / `DialogStateStore` — внешнее хранилище вместо in-memory;
- любой `CommandArgumentResolver` — свой резолвер аргументов (подхватится
  автоматически и в командах, и в диалогах);
- любой `UpdateHandler` — новое звено цепочки.

---

## Сборка

> Это **библиотека (Spring Boot starter)**, а не приложение. Плагин
> `org.springframework.boot` по умолчанию делает основным артефактом исполняемый
> `bootJar` (классы переупакованы в `BOOT-INF`), который нельзя подключить как
> обычную зависимость. Поэтому `bootJar` отключён, а основным артефактом оставлен
> обычный `jar` — тогда потребитель видит классы и
> `META-INF/spring/...imports` напрямую.

Команды сборки/тестов/запуска выполняет разработчик на своей стороне (см.
`CLAUDE.md`, раздел 0). Типичные команды:

```bash
./gradlew jar                       # собрать библиотеку
./gradlew publishToMavenLocal       # положить в локальный ~/.m2 для потребителей
./gradlew :example-bot:bootRun      # запустить демо-бот (нужен TELEGRAM_BOT_TOKEN)
```

---

## Демо-проект

Подпроект [`example-bot`](example-bot/README.md) — «потребитель», показывающий все
возможности на живых контроллерах: команды и `@Text`, inline-кнопки и callback,
«большие» данные через `@Payload`, кнопку «Закрыть», своё звено цепочки
(`UpdateMetricsHandler`), диалоги с ветвлением (`CalcDialog`), приём медиа
(`UploadDialog`) и запуск диалога из команды (`DialogLauncherController`).
