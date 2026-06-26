plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.wds.smp"
version = "0.0.1-SNAPSHOT"
description = "Демонстрационный Telegram-бот для библиотеки wds-telegram-bot-lib"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Сама библиотека (корневой проект). Через неё подключается авто-конфигурация
    // стартера (META-INF/spring/...imports) — никакого ручного @ComponentScan по
    // пакетам библиотеки не нужно.
    implementation(project(":"))

    implementation("org.springframework.boot:spring-boot-starter")

    // Типы Telegram Bot API (Update, SendMessage, InlineKeyboardMarkup и т.д.)
    // торчат в публичных сигнатурах методов-команд, поэтому объявляем их и здесь:
    // в библиотеке они подключены как implementation и не «протекают» на
    // компиляцию потребителя.
    implementation("org.telegram:telegrambots-longpolling:10.0.0")
    implementation("org.telegram:telegrambots-client:10.0.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}

// example-bot — это запускаемое приложение, ему bootJar нужен (в отличие от
// библиотеки в корне). Поведение плагина по умолчанию подходит, доп. настройка
// не требуется.
