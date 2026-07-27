package ru.asmisloff.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Nullable;

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

    @Override
    @Nullable
    public String exec() {
        log.info("Чтение файла: {}", path);
        Path resolvedPath = CWD.resolve(path).normalize();
        String result;
        if (resolvedPath.startsWith(CWD)) {
            try {
                result = Files.readString(resolvedPath);
                log.info("Файл прочитан: {} ({} символов)", path, result.length());
            } catch (IOException e) {
                result = "Ошибка чтения файла " + path + ": " + e.getMessage();
                log.error(result, e);
            }
        } else {
            result = "Попытка чтения за пределами рабочей директории: " + resolvedPath;
            log.warn(result);
        }
        return result;
    }
}
