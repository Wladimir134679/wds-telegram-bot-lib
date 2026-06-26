# example-bot — демонстрационный бот библиотеки `wds-telegram-bot-lib`

Подпроект-«потребитель»: показывает, как подключить библиотеку и писать
обработчики команд в стиле Spring MVC. Сборку и запуск выполняет разработчик
вручную (см. корневой `CLAUDE.md`, раздел 0).

## Что демонстрируется

| Возможность | Где смотреть |
|---|---|
| Текстовые команды + `@Text` (хвост сообщения) | `controller/EchoController` (`/say`, `/upper`) |
| Приветствие, reply-клавиатура, алиасы команд | `controller/StartController` (`/start`, `/help`) |
| Inline-кнопки и callback, редактирование сообщения (`EditMessageText`), `@Param` | `controller/MenuController` (`/menu`, callback `counter`, `open`) |
| «Большие» данные через `ctx.save(...)` + `@Payload` и TTL | `controller/ProfileController` (`/profile`, callback `profile_show`) |
| Системный callback «закрыть клавиатуру» (`Callback.close()`) | `controller/StartController` (`/close_demo`) |
| Цепочка обработчиков с приоритетами (своё звено) | `handler/UpdateMetricsHandler` |
| Диалоги: пошаговый сценарий с состоянием, TTL и **ветвлением** | `dialog/CalcDialog` (`/calc`) |
| Приём медиа в диалоге (фото/файл/голос) | `dialog/UploadDialog` (`/upload`) |
| Команда запускает диалог (`DialogManager`), `@ChatId`/`@UserId` | `controller/DialogLauncherController` (`/calc2`, `/whoami`) |
| Заглушка на необработанные сообщения | `telegram.bot.fallback.message` в `application.yaml` |

## Диалоги (пошаговые сценарии)

`dialog/CalcDialog` показывает диалог-машину «как форма-визард». Команда `/calc`
запускает сценарий, дальше каждый шаг — отдельный метод:

- `@Dialog({"calc", ...})` — класс-диалог, в скобках команды-триггеры;
- `@DialogStart` — точка входа (запускается командой-триггером);
- `@DialogStep("имя")` — шаг; ввод пользователя приходит через `@Text String`,
  состояние — через `DialogContext` (`ctx.set/get`), переходы — `ctx.next("...")`,
  завершение — `ctx.finish()`;
- **ветвление**: после ввода числа A для операции `√` диалог сразу считает результат
  и завершается, а для бинарных операций спрашивает число B — это обычный `if` с
  разными `ctx.next(...)`, без громоздкого `switch`.

Маршрутизация: обычная команда всегда «побеждает» активный диалог (сбрасывает его);
если команды нет, но диалог активен — сообщение идёт в текущий шаг; если нет ни
команды, ни диалога — срабатывает заглушка `telegram.bot.fallback.message`.

Включается в `application.yaml`: `telegram.bot.dialog.enabled: true`
(+ `ttl` — время простоя до сброса сессии).

### Общая инъекция аргументов

Команды и диалоги используют **одно ядро** инъекции аргументов (общий
`ArgumentBinder` + `HandlerMethod`), поэтому в шаг диалога инъектируется ровно то
же, что и в команду: `Update`, `Message`, `Chat`, `User`, `TelegramBotSender`,
`@Text`, `@ChatId`/`@UserId`, медиа-типы (`PhotoSize`/`Document`/`Video`/`Voice`/…),
плюс `DialogContext`. Добавленный резолвер автоматически доступен и там, и там —
нет двух мест, где можно «забыть прокинуть класс».

`DialogManager` (бин библиотеки) позволяет запустить диалог из обычной команды —
`dialogManager.start(ctx, "calc")` — то есть «команда, которая продолжилась».

## Запуск (выполняет разработчик)

1. Получи токен у [@BotFather](https://t.me/BotFather).
2. Задай его через переменную окружения (не коммить токен в репозиторий!):

   ```bash
   export TELEGRAM_BOT_TOKEN="123456:ABC-DEF..."
   export TELEGRAM_BOT_USERNAME="my_demo_bot"   # необязательно
   ```

   В PowerShell:

   ```powershell
   $env:TELEGRAM_BOT_TOKEN = "123456:ABC-DEF..."
   ```

3. Запусти подпроект:

   ```bash
   ./gradlew :example-bot:bootRun
   ```

4. Напиши боту `/start` в Telegram.

> Если `TELEGRAM_BOT_TOKEN` не задан, авто-конфигурация библиотеки активируется
> с пустым токеном и регистрация long polling завершится ошибкой — это ожидаемо,
> просто задай токен.
