package ru.wds.smp.wdstelegrambotlib.command;

/**
 * Сигнализирует о сбое при вызове метода-команды на уровне инфраструктуры
 * (недоступность метода, проверяемое исключение пользовательского кода и т.п.).
 *
 * <p>Исключения-{@link RuntimeException} из пользовательского кода пробрасываются
 * как есть, без потери типа и причины. В {@code CommandInvocationException}
 * оборачиваются лишь случаи, которые иначе потеряли бы контекст
 * (проверяемые исключения, ошибки доступа к методу).</p>
 */
public class CommandInvocationException extends RuntimeException {

    public CommandInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
