package ru.asmisloff.command;

/**
 * Интерфейс команды, выполняющей действие с переданными аргументами.
 */
public interface Command {

    /**
     * Выполняет команду.
     *
     * @param args аргументы команды
     */
    void exec(String... args);
}