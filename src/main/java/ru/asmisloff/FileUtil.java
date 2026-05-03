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
     * Читает содержимое файла в список строк.
     *
     * @param path Пусть к файлу.
     * @return Список строк из файла.
     * @throws IllegalStateException в случае ошибки при работе с файлом.
     */
    public static List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException ex) {
            throw new IllegalStateException(String.format("Не удалось прочитать файл %s", path.toAbsolutePath()), ex);
        }
    }

    /**
     * Читает содержимое файла в строку.
     *
     * @param path путь к файлу
     * @return содержимое файла в виде строки
     * @throws IllegalStateException при ошибке ввода-вывода
     */
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
     * @param out     Объект {@link Appendable} для вывода найденных путей.
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
     * Проверяет, содержит ли строка указанную подстроку без учета регистра.
     *
     * @param str    строка, в которой производится поиск
     * @param substr искомая подстрока
     * @return {@code true}, если подстрока найдена; иначе {@code false}
     */
    private static boolean containsIgnoreCase(@NotNull String str, @NotNull String substr) {
        for (int i = 0; i <= str.length() - substr.length(); i++) {
            if (str.regionMatches(true, i, substr, 0, substr.length())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Определяет, является ли строка объявлением пакета, импорта или состоит только из пробелов.
     *
     * @param line исходная строка
     * @return {@code true}, если строка пустая, содержит только пробелы, начинается с "import" или "package"; иначе {@code false}
     */
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
