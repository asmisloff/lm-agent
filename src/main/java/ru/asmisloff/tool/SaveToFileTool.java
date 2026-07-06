package ru.asmisloff.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Инструмент сохранения текста в файл.
 * Создаёт файл по указанному пути, записывает в него переданное содержимое,
 * а затем добавляет в Git (только вновь созданные файлы).
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
    public void exec() {
        log.info("Сохранение текста в файл: {}", path);
        Path filePath = Path.of(path);
        Path resolved = CWD.resolve(path).normalize();
        if (!resolved.startsWith(CWD)) {
            log.warn("Попытка записи за пределы рабочей директории ({}): {}", CWD, resolved);
            return;
        }
        boolean isNewFile = !Files.exists(filePath);
        try (var writer = Files.newBufferedWriter(filePath, isNewFile ? CREATE_NEW : WRITE)) {
            writeNormalized(content, writer);
            if (!content.endsWith("\n") && !content.endsWith("\r")) {
                writer.write(System.lineSeparator());
            }
            log.info("Файл успешно сохранен: {}", path);
        } catch (IOException e) {
            log.error("Ошибка записи в файл {}", path, e);
            return;
        }

        if (isNewFile) {
            addToGit(filePath);
        }
    }

    /**
     * Добавляет файл в индекс Git командой {@code git add}.
     *
     * @param filePath путь к файлу
     */
    private void addToGit(Path filePath) {
        try {
            var pb = new ProcessBuilder("git", "add", filePath.toAbsolutePath().toString());
            pb.directory(Optional.ofNullable(filePath.getParent())
                    .map(Path::toFile)
                    .orElse(new File("./"))
            );
            var process = pb.start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                log.error("git add завершился по таймауту");
                return;
            }
            var exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("Файл добавлен в Git: {}", filePath);
            } else {
                String errorOutput = new String(process.getErrorStream().readAllBytes());
                log.warn("Не удалось добавить файл в Git (exit code {}): {}", exitCode, errorOutput);
            }
        } catch (IOException e) {
            log.warn("Ошибка при добавлении файла в Git: {}", filePath, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Поток прерван при добавлении файла в Git: {}", filePath, e);
        }
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
