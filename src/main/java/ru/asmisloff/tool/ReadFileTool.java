package ru.asmisloff.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Инструмент чтения содержимого файла
 */
@Data
@Log4j2
@NoArgsConstructor
@JsonClassDescription("Прочитать содержимое файла.")
public class ReadFileTool implements Tool {

    /**
     * Текущая рабочая директория.
     */
    private static final Path CWD = Path.of(System.getProperty("user.dir"));

    /**
     * Путь к целевому файлу
     */
    @JsonPropertyDescription("Путь к файлу")
    private String path;

    /**
     * Прочитанное содержимое (выходное поле, не десериализуется)
     */
    @JsonIgnore
    private String content;

    @Override
    public void exec() {
        log.info("Чтение файла: {}", path);
        Path resolvedPath = CWD.resolve(path).normalize();
        if (!resolvedPath.startsWith(CWD)) {
            content = "Попытка чтения за пределами рабочей директории: " + resolvedPath;
            log.warn(content);
            return;
        }
        try {
            content = Files.readString(resolvedPath);
            log.info("Файл прочитан: {} ({} символов)", path, content.length());
        } catch (IOException e) {
            String errorMsg = "Ошибка чтения файла " + path + ": " + e.getMessage();
            log.error(errorMsg, e);
            content = errorMsg;
        }
    }
}
