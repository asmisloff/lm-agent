package ru.asmisloff.command;

import ru.asmisloff.Props;
import ru.asmisloff.Prompt;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * Команда для вывода в консоль промпта после раскрытия всех тегов.
 */
public class PreviewCommand implements Command {

    private final Props props;

    /**
     * Создаёт команду с заданными настройками.
     *
     * @param props настройки приложения
     */
    public PreviewCommand(Props props) {
        this.props = props;
    }

    /**
     * Выполняет команду preview: загружает промпт из файла и выводит его раскрытое содержимое.
     *
     * @param args ровно один аргумент – путь к файлу промпта
     * @throws IllegalArgumentException если количество аргументов не равно 1
     */
    @Override
    public void exec(String... args) {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    String.format("Некорректный список аргументов команды preview: %s", Arrays.toString(args))
            );
        }
        var prompt = new Prompt(Path.of(args[0]), props);
        prompt.getUserLines().forEach(System.out::println);
    }
}