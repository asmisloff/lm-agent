package ru.asmisloff.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import ru.asmisloff.GitUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Инструмент сохранения текста в файл.
 * Создаёт файл по указанному пути, записывает в него переданное содержимое, вновь созданные файлы добавляет в Git.
 */
@Data
@Log4j2
@NoArgsConstructor
@JsonClassDescription("Сохранить текст в файл.")
public class SaveToFileTool implements Tool {

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
     * Текст, записываемый в файл
     */
    @JsonPropertyDescription("Текст для записи в файл")
    private String content;

    /**
     * Сохраняет {@link #content} в файл {@link #path}.
     * Разделители строк нормализуются под {@link System#lineSeparator()}.
     * Если контент не оканчивается переводом строки, он добавляется автоматически.
     * После успешной записи вновь созданный файл добавляется в Git ({@code git add}).
     */
    @Override
    public String exec() {
        log.info("Сохранение текста в файл: {}", path);
        Path resolvedPath = CWD.resolve(path).normalize();
        if (!resolvedPath.startsWith(CWD)) {
            log.warn("Попытка записи за пределы рабочей директории ({}): {}", CWD, resolvedPath);
            return "Попытка записи за пределы рабочей директории";
        }
        boolean isNewFile = !Files.exists(resolvedPath);
        try (var writer = Files.newBufferedWriter(resolvedPath, isNewFile ? CREATE_NEW : WRITE)) {
            writeNormalized(content, writer);
            if (!content.endsWith("\n") && !content.endsWith("\r")) {
                writer.write(System.lineSeparator());
            }
            log.info("Файл успешно сохранен: {}", path);
        } catch (IOException ex) {
            log.error("Ошибка записи в файл {}", path, ex);
            return "Ошибка записи в файл: " + ex.getMessage();
        }

        if (isNewFile) {
            GitUtil.gitAdd(resolvedPath);
        }
        return "Файл успешно сохранен";
    }

    /**
     * Пишет содержимое в {@code writer}, заменяя все виды разделителей строк
     * на {@link System#lineSeparator()} без выделения дополнительной памяти.
     * <p>Логика замены: {@code \r\n} → системный разделитель, {@code \r} → системный разделитель,
     * {@code \n} → системный разделитель.
     *
     * @param raw    исходная строка
     * @param writer целевой поток вывода
     * @throws IOException при ошибке записи
     */
    private void writeNormalized(String raw, BufferedWriter writer) throws IOException {
        int len = raw.length();
        for (int i = 0; i < len; i++) {
            char c = raw.charAt(i);
            if (c == '\r') {
                writer.write(System.lineSeparator());
                if (i + 1 < len && raw.charAt(i + 1) == '\n') {
                    i++;
                }
            } else if (c == '\n') {
                writer.write(System.lineSeparator());
            } else {
                writer.write(c);
            }
        }
    }
}
