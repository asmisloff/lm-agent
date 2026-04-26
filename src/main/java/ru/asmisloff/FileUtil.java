package ru.asmisloff;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.lang.Character.isWhitespace;

@Log4j2
@UtilityClass
public class FileUtil {

    /**
     * Читает содержимое файла в одну строку.
     *
     * @param path Пусть к файлу.
     * @return Строка с содержимым файла.
     * @throws IllegalStateException в случае ошибки при работе с файлом.
     */
    public static List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException ex) {
            throw new IllegalStateException(String.format("Не удалось прочитать файл %s", path.toAbsolutePath()), ex);
        }
    }

    public static String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException(String.format("Не удалось прочитать файл %s", path.toAbsolutePath()), ex);
        }
    }

    /**
     * Читает код из файла *.java или *.kt, пропуская объявление пакета и импорты.
     *
     * @param path путь к файлу с кодом.
     * @return Код без объявления пакета и импортов одной строкой.
     */
    public static String readCode(Path path) {
        try (var reader = Files.newBufferedReader(path)) {
            String line = reader.readLine();
            while (line != null && isPackageImportOrBlankLine(line)) {
                line = reader.readLine();
            }
            var sb = new StringBuilder();
            while (line != null) {
                sb.append(line).append('\n');
                line = reader.readLine();
            }
            var end = sb.length() - 1;
            while (end >= 0 && isWhitespace(sb.charAt(end))) {
                sb.setLength(end--);
            }
            return sb.toString();
        } catch (IOException ex) {
            log.error("Ошибка чтения файла {}", path, ex);
            throw new IllegalStateException(String.format("Ошибка чтения из файла %s", path), ex);
        }
    }

    /**
     * Найти все файлы, имена которых содержат {@code pattern} без учета регистра.
     *
     * @param root    Путь к корневой директории для поиска файлов.
     * @param pattern Подстрока, по вхождению которой отбираются файлы.
     */
    public static void find(Path root, String pattern, Appendable out) {
        try (var files = Files.walk(root)) {
            files
                .filter(Files::isRegularFile)
                .map(Path::toString)
                .filter(path -> containsIgnoreCase(path, pattern))
                .forEach(path -> {
                    try {
                        out.append(path);
                        out.append('\n');
                    } catch (IOException ex) {
                        log.error("Ошибка при выводе имени файла", ex);
                        System.out.println(path);
                    }
                });
        } catch (IOException e) {
            log.error("Не удалось получить список файлов");
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

    private static boolean isPackageImportOrBlankLine(String line) {
        int offset = 0;
        var len = line.length();
        while (offset < len && isWhitespace(line.charAt(offset))) {
            ++offset;
        }
        if (offset == len) {
            return true;
        }
        String[] prefixes = {"import", "package"};
        for (var prefix : prefixes) {
            if (line.startsWith(prefix, offset)) {
                return true;
            }
        }
        return false;
    }
}
