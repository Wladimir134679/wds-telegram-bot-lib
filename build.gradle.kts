plugins {
    `java-library`
    `maven-publish`
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.wds.smp"
version = "0.0.1-SNAPSHOT"
description = "Spring Boot starter для написания Telegram-ботов в стиле Spring MVC"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    // Публикуем рядом с jar исходники и javadoc, чтобы IDE потребителя показывала
    // документацию и навигацию по коду библиотеки.
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // spring-boot-starter нужен самой библиотеке для работы; у приложения-потребителя
    // он есть и так, поэтому держим его как implementation (не «протекает» наружу).
    implementation("org.springframework.boot:spring-boot-starter")

    // Типы Telegram Bot API (Update, SendMessage, InlineKeyboardMarkup и т.д.)
    // присутствуют в ПУБЛИЧНЫХ сигнатурах методов-команд и аннотаций библиотеки,
    // поэтому подключаем их как api: они экспортируются потребителю транзитивно,
    // и ему НЕ нужно объявлять их вручную.
    api("org.telegram:telegrambots-longpolling:10.0.0")
    api("org.telegram:telegrambots-client:10.0.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Русские комментарии и Javadoc — кодировка исходников строго UTF-8.
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// Javadoc на русском: UTF-8 и мягкий doclint, чтобы сборка javadoc-jar не падала
// из-за строгих проверок ссылок/тегов.
tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Это библиотека (Spring Boot starter), а не приложение. Применённый плагин
// org.springframework.boot по умолчанию делает основным артефактом исполняемый
// bootJar (классы переупакованы в BOOT-INF), который нельзя подключить как
// обычную зависимость из другого модуля. Поэтому отключаем bootJar и оставляем
// обычный jar основным артефактом — тогда потребитель видит классы и
// META-INF/spring/...imports напрямую.
tasks.named("bootJar") {
    enabled = false
}
tasks.named("jar") {
    enabled = true
}

// Публикация артефакта в Maven-репозиторий. Координаты:
//   ru.wds.smp:wds-telegram-bot-lib:<version>
// Для локального использования потребителями: ./gradlew publishToMavenLocal
// (артефакт попадёт в ~/.m2/repository, откуда его берёт mavenLocal()).
publishing {
    publications {
        create<MavenPublication>("library") {
            // Берём обычный jar (не bootJar) вместе с корректными scope зависимостей:
            // api → compile, implementation → runtime в сгенерированном POM.
            from(components["java"])

            pom {
                name.set("wds-telegram-bot-lib")
                description.set(project.description)
            }
        }
    }
}
