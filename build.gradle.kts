plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.wds.smp"
version = "0.0.1-SNAPSHOT"
description = "wds-telegram-bot-lib"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.projectlombok:lombok")



    implementation("org.telegram:telegrambots-longpolling:10.0.0")
    implementation("org.telegram:telegrambots-client:10.0.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Это библиотека (Spring Boot starter), а не приложение. Применённый плагин
// org.springframework.boot по умолчанию делает основным артефактом исполняемый
// bootJar (классы переупакованы в BOOT-INF), который нельзя подключить как
// обычную зависимость из другого модуля. Поэтому отключаем bootJar и оставляем
// обычный jar основным артефактом — тогда подпроект example-bot (и любой
// потребитель) видит классы и META-INF/spring/...imports напрямую.
tasks.named("bootJar") {
    enabled = false
}
tasks.named("jar") {
    enabled = true
}
