# Безопасность: проверки доступа, роли и ограничения команд

Документ — как встроить авторизацию в библиотеку: проверки на уровне команд,
роли/права, ограничения по типу чата, анти-флуд. Опирается на уже существующие
точки расширения (цепочка `HandlerBotUpdate` и диспетчер `CommandContainer`),
разобранные в [REVIEW_01_CLASSES.md](REVIEW_01_CLASSES.md).

> Важно: в Telegram-боте нет «пароля». Идентичность даёт сам Telegram —
> `user.id` (стабильный, числовой) и тип чата. На этом и строим. Логин/сессии
> Spring Security здесь избыточны; нужна лёгкая своя модель авторизации.

---

## 1. Куда встраивать безопасность (две точки)

Архитектура даёт два естественных уровня перехвата:

1. **Глобальный — в цепочке обработчиков.** Новый `HandlerBotUpdate` с высоким
   приоритетом (раньше `PROCESSING_COMMAND = 100`), который может «отбить»
   апдетй до маршрутизации (бан, глобальный whitelist, анти-флуд).
2. **Точечный — в диспетчере команд.** Перед `commandContainer.executeFirst(...)`
   проверяем требования конкретной команды (роль/право/тип чата). Это даёт
   декларативные аннотации над методами-командами.

Рекомендуется **оба**: грубый фильтр глобально, тонкие проверки — на команде.

```
Update ──▶ [SecurityUpdateHandler prio=10]  // бан/whitelist/флуд → может прервать
        ──▶ [LoggingHandler prio=30]
        ──▶ [ProcessingCommand prio=100] ──▶ CommandContainer
                                                  └─▶ AccessChecker (роль/право/чат)
                                                        ├─ allow → executeFirst(...)
                                                        └─ deny  → onAccessDenied(...)
```

---

## 2. Модель ролей и прав

Минимальная, но расширяемая модель:

- **Роль** — именованная группа (`OWNER`, `ADMIN`, `USER`, `GUEST`, кастомные).
- **Право (permission)** — конкретное действие (`user.ban`, `report.view`).
- Роль агрегирует права; команда требует роль **или** право.

Не зашивай роли в enum намертво — пусть это будут строки/набор, чтобы потребитель
библиотеки добавлял свои.

```java
public interface TelegramUserDetails {
    long userId();
    Set<String> roles();
    Set<String> permissions();
    default boolean hasRole(String role) { return roles().contains(role); }
    default boolean hasPermission(String p) { return permissions().contains(p); }
    boolean isBanned();
}
```

### Откуда брать роли — `RoleProvider` (SPI для потребителя)

Библиотека **не** должна знать, где хранятся роли. Даём интерфейс, реализацию
пишет потребитель (БД, кэш, конфиг):

```java
public interface TelegramUserDetailsService {
    TelegramUserDetails loadUser(long userId);   // никогда не null → GUEST по умолчанию
}
```

Дефолтная реализация (если потребитель ничего не дал) — все `USER`, владельцы из
конфига `OWNER`:

```java
@Bean
@ConditionalOnMissingBean
TelegramUserDetailsService defaultUserDetailsService(TelegramSecurityProperty prop) {
    return userId -> {
        Set<String> roles = prop.getOwnerIds().contains(userId)
                ? Set.of("OWNER", "ADMIN", "USER")
                : Set.of("USER");
        return new SimpleUserDetails(userId, roles, /*perms*/ Set.of(),
                                     prop.getBannedIds().contains(userId));
    };
}
```

Свойства:

```yaml
telegram:
  bot:
    security:
      enabled: true
      owner-ids: [123456789]
      banned-ids: []
      default-role: USER
      deny-message: "⛔ Недостаточно прав для этой команды."
```

---

## 3. SecurityContext на время обработки апдейта

Чтобы не таскать `TelegramUserDetails` через все методы, кладём его в контекст
текущего апдейта. Так как polling однопоточный (а если многопоточный — через
`ThreadLocal`), удобно завести `SecurityContextHolder` либо просто положить
`userDetails` в существующий `CommandContext`.

Минимально — расширить `CommandContext`:

```java
public class CommandContext {
    // ...существующие поля...
    private TelegramUserDetails userDetails;   // ← добавить
}
```

И сделать его доступным для инъекции в команду (резолвер параметров в
`CommandContainer.getObjectParameter`):

```java
if (type == TelegramUserDetails.class)
    return commandContext.getUserDetails();
```

Тогда команда сможет принимать пользователя прямо в сигнатуре:

```java
@CommandFirst
public void ban(TelegramLongPollingEngine engine, TelegramUserDetails me, @ParamName("userId") Long target) { ... }
```

---

## 4. Декларативные аннотации над командами

Самый удобный для потребителя способ. Добавляем аннотации уровня метода/класса:

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value();              // нужна любая из ролей
    boolean all() default false;   // true → нужны все
}

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String[] value();
}

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowChatType {
    ChatType[] value();            // PRIVATE, GROUP, SUPERGROUP, CHANNEL
}
```

Использование в «контроллере» команды:

```java
@CommandNames({"/ban"})
public class BanCommand {

    @CommandFirst
    @RequireRole("ADMIN")
    @AllowChatType(ChatType.GROUP)
    public void ban(...) { ... }
}
```

### Проверка перед вызовом метода

Это правильнее всего встроить в `CommandContainer.executeMethod(...)` — там, где
сейчас рефлексией находится метод. **Важно (см. критику ядра):** аннотации
команды нужно **закэшировать при регистрации**, а не сканировать на каждый вызов.

```java
private void executeMethod(Method method, CommandContext ctx) {
    AccessDecision decision = accessChecker.check(method, ctx);   // ← кэш аннотаций внутри
    if (!decision.allowed()) {
        accessDeniedHandler.handle(ctx, decision);                // вежливый отказ
        return;
    }
    // ...текущая инъекция аргументов и method.invoke(...)
}
```

`AccessChecker` инкапсулирует логику: собирает требования метода + класса,
сверяет с `ctx.getUserDetails()` и типом чата.

```java
public AccessDecision check(Method method, CommandContext ctx) {
    var u = ctx.getUserDetails();
    if (u.isBanned()) return AccessDecision.deny("banned");

    var roleReq = resolve(method, RequireRole.class);   // метод важнее класса
    if (roleReq != null && !matchesRole(u, roleReq))
        return AccessDecision.deny("role");

    var permReq = resolve(method, RequirePermission.class);
    if (permReq != null && !u.permissions().containsAll(List.of(permReq.value())))
        return AccessDecision.deny("permission");

    var chatReq = resolve(method, AllowChatType.class);
    if (chatReq != null && !allowedChat(ctx, chatReq))
        return AccessDecision.deny("chatType");

    return AccessDecision.allow();
}
```

---

## 5. Обработка отказа — `AccessDeniedHandler`

Нельзя просто молча проглатывать — пользователь должен понять, что ему отказано
(или, наоборот, для скрытых команд — тишина). Делаем настраиваемым:

```java
public interface AccessDeniedHandler {
    void handle(CommandContext ctx, AccessDecision decision);
}
```

Дефолт — ответить сообщением из `security.deny-message`; потребитель может
заменить (логировать попытку, тихо игнорировать, уведомлять админов).

```java
@Bean
@ConditionalOnMissingBean
AccessDeniedHandler defaultAccessDeniedHandler(TelegramSecurityProperty prop) {
    return (ctx, d) -> {
        long chatId = extractChatId(ctx);
        ctx.getEngine().executeNotException(
            SendMessage.builder().chatId(chatId).text(prop.getDenyMessage()).build());
        log.warn("Access denied: user={}, reason={}, command={}",
                 ctx.getUserDetails().userId(), d.reason(), ctx.getName());
    };
}
```

---

## 6. Глобальный фильтр — `SecurityUpdateHandler`

Грубые проверки до маршрутизации (бан, белый список, режим обслуживания):

```java
@Service
@ConditionalOnProperty("telegram.bot.security.enabled")
public class SecurityUpdateHandler implements HandlerBotUpdate {

    @Override public int priority() { return 10; } // раньше команд

    @Override
    public void update(TelegramLongPollingEngine engine, Update update) {
        Long userId = extractUserId(update);
        if (userId == null) return;
        var u = userDetailsService.loadUser(userId);
        if (u.isBanned()) {
            log.info("Blocked banned user {}", userId);
            // прервать дальнейшую обработку (см. п.7 про прерывание цепочки)
        }
    }
}
```

⚠️ Сейчас цепочка (`TelegramLongPollingEngine.consume`) гоняет **все**
обработчики `forEach` без возможности прервать. Чтобы фильтр реально «отбивал»
апдейт, нужно дать механизм прерывания.

---

## 7. Нужна доработка ядра: прерывание цепочки

Без этого безопасность на глобальном уровне неполноценна. Варианты:

**Вариант A — обработчик возвращает признак «продолжать ли»:**

```java
public interface HandlerBotUpdate {
    boolean update(TelegramLongPollingEngine engine, Update update); // false → стоп
    int priority();
}
```

```java
public void consume(Update update) {
    for (HandlerBotUpdate h : handlerBotUpdates) {
        try {
            if (!h.update(this, update)) break;          // прерывание
        } catch (Exception e) {                          // + изоляция ошибок
            log.error("Handler {} failed", h.getClass().getSimpleName(), e);
        }
    }
}
```

**Вариант B — исключение `AccessDeniedException`**, которое ловится в `consume`
и останавливает цепочку. Менее явно, но не ломает текущую сигнатуру.

Рекомендую **A**: заодно закрывает замечание из критики про отсутствие изоляции
ошибок в цепочке.

---

## 8. Анти-флуд / rate limiting

Частый вектор злоупотреблений. Реализуется как ещё один ранний `HandlerBotUpdate`
поверх существующего `CacheMap`/Caffeine (счётчик запросов на `userId` за окно):

```java
@Override
public boolean update(TelegramLongPollingEngine engine, Update update) {
    Long userId = extractUserId(update);
    int count = counter.merge(userId, 1, Integer::sum);   // окно через TTL-кэш
    if (count > prop.getRateLimit().getMaxPerWindow()) {
        log.warn("Rate limit exceeded: user={}", userId);
        return false; // прервать
    }
    return true;
}
```

Параметры: `max-per-window`, `window` (`Duration`). Опционально — временный
авто-бан при систематическом превышении.

---

## 9. Ограничения по типу чата и админству в группе

Частые требования:
- **Только личка** (`PRIVATE`) — для команд с приватными данными.
- **Только админ группы** — проверка через `engine` →
  `getChatMember(chatId, userId)` и статус `administrator`/`creator`.
  Результат кэшировать (TTL ~1–5 мин), т.к. это сетевой вызов в Telegram API.

```java
@AllowChatType(ChatType.PRIVATE)        // декларативно
// или программно для «админ группы»:
boolean isGroupAdmin = chatAdminCache.isAdmin(chatId, userId, engine);
```

---

## 10. Защита `callback_data` (важно)

Callback-кнопки — отдельная поверхность атаки: пользователь может прислать
**любой** `callback_data`, в т.ч. от чужой кнопки (нажав на пересланное/старое
сообщение). Поэтому:

- Авторизацию по роли/праву применять к callback-командам **так же**, как к
  текстовым (тот же `AccessChecker`).
- Не доверять полезной нагрузке в `callback_data` — валидировать, что
  пользователь имеет право на цель действия (например, `target=userId` ≠ право
  банить кого угодно).
- Если кнопка предназначена конкретному пользователю — класть в payload
  «кому адресовано» и сверять с `from.id` (или хранить владельца на сервере по
  короткому ключу — связано с лимитом 64 байта из общей критики).

---

## 11. Аудит и логирование безопасности

- Логировать **отказы** и срабатывания фильтров (`userId`, команда, причина) —
  отдельный логгер/уровень, чтобы можно было мониторить злоупотребления.
- **Не** логировать содержимое сообщений и токен (см. критику в OVERVIEW).
- Опционально — событие `AccessDeniedEvent` через `ApplicationEventPublisher`,
  чтобы потребитель мог реагировать (метрики, алерты админам).

---

## 12. Что добавить в библиотеку — чек-лист

- [ ] `TelegramUserDetails` + `TelegramUserDetailsService` (SPI) + дефолтная
      реализация из конфига (owner/banned).
- [ ] `TelegramSecurityProperty` (`enabled`, `owner-ids`, `banned-ids`,
      `deny-message`, `rate-limit`).
- [ ] Аннотации `@RequireRole`, `@RequirePermission`, `@AllowChatType`.
- [ ] `AccessChecker` с **кэшированием аннотаций при регистрации команды**.
- [ ] Встроить проверку в `CommandContainer.executeMethod(...)`.
- [ ] `AccessDeniedHandler` (дефолт + переопределяемый).
- [ ] Резолвер параметра `TelegramUserDetails` в инъекции аргументов команды.
- [ ] `SecurityUpdateHandler` (бан/whitelist) — ранний приоритет.
- [ ] **Доработать ядро: прерывание цепочки обработчиков** (Вариант A) — без
      этого глобальные фильтры неполноценны.
- [ ] Анти-флуд `RateLimitHandler` поверх TTL-кэша.
- [ ] Кэш админов чата (`getChatMember` + TTL).
- [ ] Аудит-логгер отказов / событие `AccessDeniedEvent`.
- [ ] Тесты: роль есть/нет, бан, тип чата, флуд, callback от чужой кнопки.

---

## Порядок внедрения (рекомендация)

1. Сначала **прерывание цепочки + изоляция ошибок** (п.7) — это база и заодно
   закрывает баг из общей критики.
2. Затем `TelegramUserDetails` + `TelegramSecurityProperty` + дефолт из конфига.
3. Декларативные аннотации + `AccessChecker` в диспетчере команд (п.4–5).
4. `SecurityUpdateHandler` (бан) и анти-флуд (п.6, 8).
5. Тонкости: админ группы, защита callback, аудит (п.9–11).

Связанные документы: общая критика — [REVIEW_00_OVERVIEW.md](REVIEW_00_OVERVIEW.md),
поклассовый разбор — [REVIEW_01_CLASSES.md](REVIEW_01_CLASSES.md),
план переноса — [REVIEW_02_MIGRATION.md](REVIEW_02_MIGRATION.md).
