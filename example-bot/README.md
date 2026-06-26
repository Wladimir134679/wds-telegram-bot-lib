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
