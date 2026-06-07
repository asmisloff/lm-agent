package ru.asmisloff.command;

import ru.asmisloff.FileUtil;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * Команда поиска файлов в текущем каталоге по имени.
 */
public class FindCommand implements Command {

    /**
     * Выполняет поиск файлов с именами, соответствующими заданному шаблону.
     *
     * @param args ровно один аргумент – шаблон
     * @throws IllegalArgumentException если количество аргументов не равно 1
     */
    @Override
    public void exec(String... args) {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    String.format("Некорректный список аргументов команды find: %s", Arrays.toString(args))
            );
        }
        FileUtil.find(Path.of("."), args[0], System.out);
    }
}