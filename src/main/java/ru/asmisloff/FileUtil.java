package ru.asmisloff;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import ru.asmisloff.command.FullReplacePatch;
import ru.asmisloff.command.Patch;
import ru.asmisloff.command.SearchReplacePatch;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

@Log4j2
@UtilityClass
public class FileUtil {

    /**
     * Маркер блока кода в markdown.
     */
    private static final String CODE_MARKER = "```";

    /**
     * Маркер начала search-replace патча.
     */
    private static final String PATCH_BEGIN_PREFIX = "[PATCH_BEGIN:";

    /**
     * Маркер конца search-replace патча.
     */
    private static final String PATCH_END = "[PATCH_END]";

    /**
     * Разделитель search и replace секций.
     */
    private static final String SEARCH_MARKER = "--- SEARCH ---";
    private static final String REPLACE_MARKER = "--- REPLACE ---";

    /**
     * Расширения файлов, включаемые в карту проекта.
     */
    private static final Set<String> PROJECT_MAP_EXTENSIONS = Set.of(".java", ".xml", ".properties");

    /**
     * Директории, исключаемые из обхода при построении карты проекта.
     */
    private static final Set<String> PROJECT_MAP_EXCLUDED_DIRS = Set.of("target", ".git", ".idea");

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
            return Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException(String.format("Не удалось прочитать файл %s", path.toAbsolutePath()), ex);
        }
    }

    public static void writeString(Path path, String s) {
        try {
            Files.writeString(path, s, StandardOpenOption.CREATE);
        } catch (IOException ex) {
            throw new IllegalStateException(String.format("Не удалось записать в файл %s", path.toAbsolutePath()), ex);
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
     * Поддерживает блоки FullReplace (в тройных кавычках) и SearchReplace (в маркерах PATCH_BEGIN/PATCH_END).
     *
     * @param path путь к файлу Markdown.
     * @return таблица: ключ — путь к файлу, значение — патч.
     */
    public static @NotNull Map<String, Patch> extractCode(Path path) {
        Map<String, Patch> res = new HashMap<>();
        StringBuilder buf = new StringBuilder();
        try (var reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(CODE_MARKER)) {
                    // FullReplacePatch
                    var filePath = readFilePath(line, reader);
                    if (filePath == null) {
                        continue;
                    }
                    readUntilCodeMarker(reader, buf);
                    res.put(filePath, new FullReplacePatch(buf.toString()));
                } else if (line.startsWith(PATCH_BEGIN_PREFIX)) {
                    // SearchReplacePatch
                    String filePath = extractPatchFilePath(line);
                    if (filePath == null) {
                        continue;
                    }
                    readSearchReplacePatch(
                            reader,
                            (SearchReplacePatch) res.computeIfAbsent(filePath, unused -> new SearchReplacePatch())
                    );
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    public Path cwd() {
        return Path.of(System.getProperty("user.dir"));
    }

    /**
     * Строит текстовое дерево проекта в формате команды {@code tree}.
     * Включает только файлы с расширениями {@code .java}, {@code .xml}, {@code .properties}.
     * Игнорирует директории {@code target} и {@code .git}.
     *
     * @param root корневая директория проекта
     * @return строковое представление дерева проекта
     */
    public static String buildProjectTree(Path root) {
        TreeNode treeRoot = new TreeNode(root.getFileName().toString(), true);
        try (var files = Files.walk(root)) {
            files
                    .filter(Files::isRegularFile)
                    .filter(FileUtil::hasProjectMapExtension)
                    .filter(path -> isNotExcluded(path, root))
                    .forEach(path -> insertIntoTree(treeRoot, root.relativize(path)));
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось построить дерево проекта", e);
        }
        return renderTree(treeRoot);
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

    /**
     * Извлечь путь к файлу из строки вида {@code [PATCH_BEGIN: путь/к/файлу]}.
     */
    private static String extractPatchFilePath(String line) {
        int start = PATCH_BEGIN_PREFIX.length();
        int end = line.lastIndexOf(']');
        if (end <= start) {
            return null;
        }
        String path = line.substring(start, end).strip();
        return path.isEmpty() ? null : path;
    }

    /**
     * Читает одну пару search/replace из потока и добавляет в {@code patch}.
     * Ожидает секции {@code --- SEARCH ---} и {@code --- REPLACE ---}, завершается по {@code [PATCH_END]}.
     */
    private static void readSearchReplacePatch(BufferedReader reader, SearchReplacePatch patch) throws IOException {
        StringBuilder search = new StringBuilder();
        StringBuilder replace = new StringBuilder();
        boolean inSearch = false;
        boolean inReplace = false;

        String line;
        while ((line = reader.readLine()) != null && !line.equals(PATCH_END)) {
            if (line.equals(SEARCH_MARKER)) {
                inSearch = true;
                inReplace = false;
                continue;
            }
            if (line.equals(REPLACE_MARKER)) {
                inReplace = true;
                inSearch = false;
                continue;
            }
            if (inSearch) {
                search.append(line).append(System.lineSeparator());
            } else if (inReplace) {
                replace.append(line).append(System.lineSeparator());
            }
        }

        if (!search.isEmpty() || !replace.isEmpty()) {
            patch.addEntry(search.toString(), replace.toString());
        }
    }

    /**
     * Проверяет, что путь не содержит исключаемых директорий.
     */
    private static boolean isNotExcluded(Path path, Path root) {
        Path relative = root.relativize(path);
        for (Path component : relative) {
            if (PROJECT_MAP_EXCLUDED_DIRS.contains(component.toString())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Проверяет, что имя файла заканчивается на одно из допустимых расширений.
     */
    private static boolean hasProjectMapExtension(Path path) {
        String name = path.getFileName().toString();
        for (String PROJECT_MAP_EXTENSION : PROJECT_MAP_EXTENSIONS) {
            if (name.endsWith(PROJECT_MAP_EXTENSION)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Вставляет относительный путь в дерево, создавая промежуточные узлы-директории.
     */
    private static void insertIntoTree(TreeNode root, Path relativePath) {
        TreeNode current = root;
        int nameCount = relativePath.getNameCount();
        for (int i = 0; i < nameCount; i++) {
            String name = relativePath.getName(i).toString();
            boolean isDir = i < nameCount - 1;
            TreeNode child = current.findChild(name);
            if (child == null) {
                child = new TreeNode(name, isDir);
                current.children.add(child);
            }
            current = child;
        }
    }

    /**
     * Рендерит дерево в строку с символами псевдографики.
     */
    private static String renderTree(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        sb.append(root.name).append('\n');
        renderChildren(root, "", sb);
        return sb.toString();
    }

    /**
     * Рекурсивно рендерит дочерние узлы.
     *
     * @param parent родительский узел
     * @param prefix префикс отступов для текущего уровня
     * @param buf    буфер для результата
     */
    private static void renderChildren(TreeNode parent, String prefix, StringBuilder buf) {
        var children = parent.children;
        children.sort(Comparator
                .comparing(TreeNode::isDir)
                .reversed()
                .thenComparing(TreeNode::getName)
        );
        for (int i = 0; i < children.size(); i++) {
            TreeNode child = children.get(i);
            boolean isLast = i == children.size() - 1;
            String connector = isLast ? "└── " : "├── ";
            buf.append(prefix).append(connector).append(child.name).append('\n');
            if (child.isDir) {
                String childPrefix = prefix + (isLast ? "    " : "│   ");
                renderChildren(child, childPrefix, buf);
            }
        }
    }

    /**
     * Узел дерева проекта.
     */
    @Getter
    private static class TreeNode {
        final String name;
        final boolean isDir;
        final List<TreeNode> children = new ArrayList<>();

        TreeNode(String name, boolean isDir) {
            this.name = name;
            this.isDir = isDir;
        }

        TreeNode findChild(String name) {
            for (TreeNode child : children) {
                if (child.name.equals(name)) {
                    return child;
                }
            }
            return null;
        }
    }

    private static void readUntilCodeMarker(BufferedReader reader, StringBuilder buf) throws IOException {
        String line = reader.readLine();
        buf.setLength(0);
        while (line != null && line.isBlank()) {
            line = reader.readLine();
        }
        while (line != null && !line.startsWith(CODE_MARKER)) {
            buf.append(line).append(System.lineSeparator());
            line = reader.readLine();
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
