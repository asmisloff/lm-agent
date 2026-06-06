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
     * Открывающий маркер блока кода в markdown.
     */
    private static final String FILE_PATH_PREFIX = ">>> FILE: ";

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
        try {
            return Files.readString(path).stripTrailing();
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
     * Извлечь программный код из файла Markdown.
     * <p>Код должен иметь заголовок с путем к файлу. Также он должен быть заключен в тройные обратные кавычки.</p>
     *
     * @param path путь к файлу Markdown.
     * @return Таблица, где ключ - имя файла, значение - код.
     */
    public static @NotNull Map<String, String> extractCode(Path path) {
        HashMap<String, String> res = null;
        StringBuilder buf = null;
        try (var reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(FILE_PATH_PREFIX)) {
                    String codeFilePath = line.substring(FILE_PATH_PREFIX.length()).trim();
                    res = Objects.requireNonNullElse(res, new HashMap<>());
                    buf = readCode(reader, codeFilePath, res, buf);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Objects.requireNonNullElse(res, Collections.emptyMap());
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

    private static StringBuilder readCode(
            BufferedReader reader,
            String codeFilePath,
            Map<String, String> dest,
            StringBuilder buf
    ) throws IOException {
        String line = reader.readLine();
        if (line != null && line.startsWith(CODE_MARKER)) {
            buf = Objects.requireNonNullElse(buf, new StringBuilder());
            while ((line = reader.readLine()) != null && !line.startsWith(CODE_MARKER)) {
                buf.append(line).append('\n');
            }
            buf.setLength(buf.length() - 1);
            dest.put(codeFilePath, buf.toString());
            buf.setLength(0);
        }
        return buf;
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

}
