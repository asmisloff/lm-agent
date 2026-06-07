package ru.asmisloff.command;

import ru.asmisloff.Props;

import java.util.Map;
import java.util.Objects;

/**
 * Реестр команд, связывающий имена команд с их реализациями.
 */
public class CommandRegistry {

    private final Map<String, Command> reg;

    /**
     * Создаёт реестр с набором предопределённых команд.
     *
     * @param props настройки приложения
     */
    public CommandRegistry(Props props) {
        reg = Map.of(
                "send", new SendPromptCommand(props),
                "find", new FindCommand(),
                "preview", new PreviewCommand(props)
        );
    }

    /**
     * Возвращает команду по имени.
     *
     * @param name имя команды
     * @return команда
     * @throws NullPointerException если имя команды не зарегистрировано
     */
    public Command get(String name) {
        return Objects.requireNonNull(
                reg.get(name),
                () -> String.format("Неизвестная команда: %s", name)
        );
    }
}