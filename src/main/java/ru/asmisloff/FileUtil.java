package ru.asmisloff;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Collections;
import java.util.Objects;

public class FileUtil {

    public static List<String> prompt() {
        try (var reader = Files.newBufferedReader(Path.of("prompt.md"))) {
            return reader.lines().toList();
        } catch (IOException ex) {
            System.out.println("Не удалось прочитать prompt.md из файловой системы");
        }
        try (var ins = FileUtil.class.getResourceAsStream("prompt.md");
             var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(ins)))
        ) {
            return reader.lines().toList();
        } catch (IOException | NullPointerException ex) {
            System.out.println("Не удалось прочитать промпт из classpath");
            throw new IllegalStateException(ex);
        }
    }

    public static List<Path> find(String pattern) {
        try (var files = Files.walk(Path.of("."))) {
            return files
                    .filter(path -> Files.isRegularFile(path) && containsIgnoreCase(path.toString(), pattern))
                    .toList();
        } catch (IOException e) {
            System.out.println("Не удалось получить список файлов");
            return Collections.emptyList();
        }
    }

    /**
     * Копирует содержимое текстового файла в выходной поток.
     *
     * @param sourcePath путь к исходному файлу
     * @param writer     BufferedWriter для записи результатов
     * @throws IOException              если произошла ошибка ввода-вывода
     * @throws IllegalArgumentException если sourcePath или writer равны null
     */
    public static void append(Path sourcePath, BufferedWriter writer) throws IOException {
        if (sourcePath == null) {
            throw new IllegalArgumentException("Не задан sourcePath");
        }
        if (writer == null) {
            throw new IllegalArgumentException("Не задан writer");
        }

        try (BufferedReader reader = Files.newBufferedReader(sourcePath)) {
            char[] buffer = new char[8192]; // 8KB буфер
            int charsRead;

            while ((charsRead = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, charsRead);
            }
        }
    }


    private static boolean containsIgnoreCase(@NotNull String str, @NotNull String substr) {
        for (int i = 0; i <= str.length() - substr.length(); i++) {
            if (str.regionMatches(true, i, substr, 0, substr.length())) {
                return true;
            }
        }
        return false;
    }
}
