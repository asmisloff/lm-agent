package ru.asmisloff.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import ru.asmisloff.App;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.APPEND;

/**
 * Инструмент, задающий вопрос пользователю.
 * Вопрос дописывается в конец файла промпта с маркерными последовательностями.
 */
@Data
@Log4j2
@NoArgsConstructor
@JsonClassDescription("Задать вопрос пользователю.")
public class AskUserTool implements Tool {

    public static final String MARKER_OPEN = ">>> ASK_USER >>>";
    public static final String MARKER_CLOSE = "<<< ASK_USER <<<";

    @JsonPropertyDescription("Текст вопроса или сообщения для пользователя")
    private String question;

    @Override
    public void exec() {
        log.info("AskUser: {}", question);
        var props = App.getProps();
        var promptFile = Path.of(props.getPromptFileName());
        try (var writer = Files.newBufferedWriter(promptFile, APPEND)) {
            writer.write("\n\n");
            writer.write(MARKER_OPEN);
            writer.write("\n");
            writer.write(question);
            writer.write("\n");
            writer.write(MARKER_CLOSE);
            log.debug("Вопрос записан в {}", promptFile);
        } catch (IOException e) {
            log.error("Ошибка записи вопроса в {}", promptFile, e);
        }
    }
}
