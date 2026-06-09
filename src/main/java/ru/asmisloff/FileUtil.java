package ru.asmisloff;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Log4j2
@UtilityClass
public class FileUtil {

    /**
     * Закрывающий маркер блока кода в markdown.
     */
    private static final String CODE_MARKER = "```";

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
    public static String getFileContent(Path path) {
        try {
            return Files.readString(path).strip();
        } catch (IOException ex) {
            throw new IllegalStateException(String.format("Не удалось прочитать файл %s", path.toAbsolutePath()), ex);
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
     * Извлечь программный код из файла Markdown.
     * <p>Код должен быть заключен в тройные кавычки с указанием метки языка программирования, по правилам Markdown.
     * <p>Первая строка кода должна содержать путь к файлу. Если путь не указан, код будет пропущен.
     *
     * @param path путь к файлу Markdown.
     * @return Таблица, в которой ключ - имя файла, значение - код.
     */
    public static @NotNull LinkedHashMap<String, String> extractCode(Path path) {
        LinkedHashMap<String, String> res = new LinkedHashMap<>();
        StringBuilder buf = new StringBuilder();
        try (var reader = Files.newBufferedReader(path)) {
            String line;
            String filePath;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(CODE_MARKER)) {
                    continue;
                }
                filePath = readFilePath(line, reader);
                if (filePath == null) {
                    continue;
                }
                readUntilCodeMarker(reader, buf);
                res.put(filePath, buf.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    /**
     * Сохранить строку с кодом в файл по указанному пути.
     * <p>Если родительские директории отсутствуют, они будут созданы.</p>
     *
     * @param filePath путь к файлу, включая имя.
     * @param code     строка с исходным кодом для сохранения.
     * @throws IllegalStateException если произошла ошибка ввода-вывода.
     */
    public static void saveCode(String filePath, String code) {
        try {
            Path path = Path.of(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, code);
        } catch (IOException ex) {
            log.error("Ошибка сохранения файла {}", filePath, ex);
            throw new IllegalStateException(String.format("Не удалось сохранить файл %s", filePath), ex);
        }
    }

    /**
     * Проверяет, содержит ли строка указанную подстроку без учета регистра.
     *
     * @param str    строка, в которой производится поиск
     * @param substr искомая подстрока
     * @return {@code true}, если подстрока найдена; иначе {@code false}
     */
    public static boolean containsIgnoreCase(@NotNull String str, @NotNull String substr) {
        for (int i = 0; i <= str.length() - substr.length(); i++) {
            if (str.regionMatches(true, i, substr, 0, substr.length())) {
                return true;
            }
        }
        return false;
    }

    private static void readUntilCodeMarker(BufferedReader reader, StringBuilder buf) throws IOException {
        String line = reader.readLine();
        buf.setLength(0);
        while (line.isBlank()) { // пропустить пустые строки в начале файла
            line = reader.readLine();
        }
        while (line != null && !line.startsWith(CODE_MARKER)) {
            buf.append(line).append('\n');
            line = reader.readLine();
        }
        while (!buf.isEmpty() && Character.isWhitespace(buf.charAt(buf.length() - 1))) {
            buf.setLength(buf.length() - 1);
        }
    }

    private static String readFilePath(String langMark, BufferedReader reader) throws IOException {
        var fileTypeAttributes = Prompt.getFileTypeAttributes().stream()
                .filter(att -> langMark.startsWith(att.langMark()))
                .findFirst()
                .orElse(null);
        if (fileTypeAttributes != null) {
            String line = reader.readLine();
            if (line != null && line.startsWith(fileTypeAttributes.commentPrefix())) {
                var path = line.substring(
                        fileTypeAttributes.commentPrefix().length(),
                        line.lastIndexOf(fileTypeAttributes.commentSuffix())
                );
                if (path.isBlank()) {
                    return null;
                }
                return path.strip();
            }
        }
        return null;
    }

}