package ru.asmisloff;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Утилиты для работы с Git.
 */
@UtilityClass
@Log4j2
public class GitUtil {

    /**
     * Возвращает diff (patch) указанного коммита.
     *
     * @param id хеш коммита
     * @return вывод {@code git show <id>} или {@code null} при ошибке
     */
    public static String getPatch(String id) {
        Process process = null;
        try {
            process = new ProcessBuilder("git", "show", id)
                    .directory(Path.of(System.getProperty("user.dir")).toFile())
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                log.error("git show завершился по таймауту для коммита {}", id);
                return null;
            }
            int exitCode = process.exitValue();
            String output = new String(process.getInputStream().readAllBytes());
            if (exitCode == 0) {
                return output;
            } else {
                log.warn("git show завершился с ошибкой (exit code {}): {}", exitCode, output);
                return null;
            }
        } catch (IOException e) {
            log.warn("Ошибка при получении патча для коммита {}: {}", id, e.toString());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Поток прерван при получении патча для коммита {}", id, e);
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * Добавляет файл в индекс Git командой {@code git add}.
     *
     * @param filePath путь к файлу
     */
    public static void gitAdd(Path filePath) {
        Process process = null;
        try {
            process = new ProcessBuilder("git", "add", filePath.toAbsolutePath().toString())
                    .directory(filePath.getParent().toFile())
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                log.error("git add завершился по таймауту для файла {}", filePath);
                return;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("Файл добавлен в Git: {}", filePath);
            } else {
                String stderr = new String(process.getInputStream().readAllBytes());
                log.warn("Не удалось добавить файл в Git (exit code {}): {}. stderr: {}", exitCode, filePath, stderr);
            }
        } catch (IOException e) {
            log.warn("Ошибка при добавлении файла в Git: {}", filePath, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Поток прерван при добавлении файла в Git: {}", filePath, e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
