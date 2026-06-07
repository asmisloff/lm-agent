package ru.asmisloff;

import lombok.extern.log4j.Log4j2;
import ru.asmisloff.command.CommandRegistry;

import java.util.Arrays;

/**
 * Главный класс приложения.
 */
@Log4j2
public class App {

    private static final Props props = new Props();
    private static final CommandRegistry cmdReg = new CommandRegistry(props);

    /**
     * Точка входа в приложение.
     * <p>Первый аргумент задаёт имя команды, остальные передаются команде как параметры.</p>
     *
     * @param args аргументы командной строки.
     * @throws IllegalArgumentException если передан флаг {@code -f} без имени паттерна.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("Не указана команда");
        }
        String cmdName = args[0];
        cmdReg.get(cmdName).exec(Arrays.copyOfRange(args, 1, args.length));
    }
}