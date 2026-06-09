package ru.asmisloff;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilTest {

    /**
     * Временная директория для тестов.
     */
    @TempDir
    Path tempDir;

    // ======== find ========

    /**
     * Поиск существующего файла по полному совпадению (без учета регистра).
     */
    @Test
    void find_existingFileCaseInsensitive_returnsFile() throws IOException {
        Path targetFile = tempDir.resolve("TestFile.txt");
        Files.createFile(targetFile);
        Path otherFile = tempDir.resolve("Other.txt");
        Files.createFile(otherFile);

        var out = new StringBuilder();
        FileUtil.find(tempDir, "testfile.txt", out);
        out.setLength(out.length() - 1);

        assertEquals(targetFile.toString(), out.toString());
    }

    /**
     * Поиск по частичному совпадению имени.
     */
    @Test
    void find_partialPatternMatch_returnsMatchingFiles() throws IOException {
        var expected = Files.createFile(tempDir.resolve("apple-pie.txt"));
        Files.createFile(tempDir.resolve("banana-cake.txt"));
        Files.createFile(tempDir.resolve("cherry-tart.txt"));

        var out = new StringBuilder();
        FileUtil.find(tempDir, "pie", out);
        out.setLength(out.length() - 1);

        assertEquals(expected.toString(), out.toString());
    }

    /**
     * Пустой результат, если совпадений нет.
     */
    @Test
    void find_noMatchingFiles_returnsEmptyList() throws IOException {
        Files.createFile(tempDir.resolve("data.json"));
        Files.createFile(tempDir.resolve("config.yml"));

        var out = new StringBuilder();
        FileUtil.find(tempDir, "xml", out);

        assertTrue(out.isEmpty());
    }

    /**
     * Игнорирует поддиректории, возвращает только файлы.
     */
    @Test
    void find_ignoresDirectories_returnsOnlyFiles() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.createFile(file);
        Path dir = tempDir.resolve("subdir");
        Files.createDirectory(dir);

        var out = new StringBuilder();
        FileUtil.find(tempDir, "subdir", out);

        assertTrue(out.isEmpty());
    }

    /**
     * Ищет во всех поддиректориях (рекурсивно).
     */
    @Test
    void find_searchesRecursively_returnsFileInSubdirectory() throws IOException {
        Path subDir = tempDir.resolve("sub");
        Files.createDirectory(subDir);
        Path targetFile = subDir.resolve("target.dat");
        Files.createFile(targetFile);

        var out = new StringBuilder();
        FileUtil.find(tempDir, "target", out);
        out.setLength(out.length() - 1);

        assertEquals(targetFile.toString(), out.toString());
    }

    /**
     * Корректно обрабатывает пустой паттерн. todo: вообще-то, не должно так быть. Исправить.
     */
    @Test
    void find_emptyPattern_returnsAllFiles() throws IOException {
        Path file1 = tempDir.resolve("a.txt");
        Path file2 = tempDir.resolve("b.txt");
        Files.createFile(file1);
        Files.createFile(file2);

        var out = new StringBuilder();
        FileUtil.find(tempDir, "", out);
        out.setLength(out.length() - 1);

        assertEquals(
                String.join("\n", file1.toString(), file2.toString()),
                out.toString()
        );
    }

    /**
     * Пустой ответ при IOException.
     */
    @Test
    void find_ioException_returnsEmptyListAndPrintsMessage() {
        Path originalDir = Path.of(".");
        try {
            System.setProperty("user.dir", "/nonexistent/path/12345");
            var out = new StringBuilder();
            FileUtil.find(tempDir, "test", out);
            assertTrue(out.isEmpty());
        } finally {
            System.setProperty("user.dir", originalDir.toString());
        }
    }

    // ======== getFileContent ========

    /**
     * Несколько поддерживаемых языков — каждый извлекается с корректным префиксом комментария.
     */
    @Test
    void extractCode_multipleLanguages_extractsBoth() throws IOException {
        String content = """
                ```java
                //A.java
                Java code
                ```
                
                ```sql
                --B.sql
                sql query
                ```
                
                ```kotlin
                // C.kt
                kotlin kode
                ```
                
                ```xml
                <!-- D.xml -->
                xml code
                ```
                
                ```
                Отсутствует md-метка
                ```
                
                ```unknown
                Неизвестный язык. Должен быть пропущен.
                ```
                """;
        Path file = createMarkdownFile(content);
        Map<String, String> result = FileUtil.extractCode(file);

        assertEquals(4, result.size());
        assertEquals("Java code", result.get("A.java"));
        assertEquals("sql query", result.get("B.sql"));
        assertEquals("kotlin kode", result.get("C.kt"));
        assertEquals("xml code", result.get("D.xml"));
    }

    /**
     * Строка комментария без пути после открывающего маркера — блок пропускается.
     */
    @Test
    void extractCode_missingFilePath_skipped() throws IOException {
        String content = "```java\n// \n```\ncode\n```\n";
        Path file = createMarkdownFile(content);
        Map<String, String> result = FileUtil.extractCode(file);
        assertTrue(result.isEmpty(), "Без указания файла блок игнорируется");
    }

    /**
     * Вспомогательный метод для создания markdown-файла с заданным содержимым.
     */
    private Path createMarkdownFile(String content) throws IOException {
        Path file = tempDir.resolve("test.md");
        Files.writeString(file, content);
        return file;
    }

    // ======== saveCode ========

    /**
     * Сохранение кода в файл: файл создается, содержимое совпадает.
     */
    @Test
    void saveCode_createsFileWithContent() throws IOException {
        Path file = tempDir.resolve("output.txt");
        String code = "System.out.println(\"Hello\");";

        FileUtil.saveCode(file.toString(), code);

        assertTrue(Files.exists(file));
        assertEquals(code, Files.readString(file));
    }

    /**
     * Сохранение в несуществующую директорию: родительские директории создаются автоматически.
     */
    @Test
    void saveCode_createsParentDirectories() throws IOException {
        Path file = tempDir.resolve("deep/nested/dir/code.txt");
        String code = "print(42)";

        FileUtil.saveCode(file.toString(), code);

        assertTrue(Files.exists(file));
        assertEquals(code, Files.readString(file));
    }

    /**
     * Перезапись существующего файла: старое содержимое заменяется новым.
     */
    @Test
    void saveCode_overwritesExistingFile() throws IOException {
        Path file = tempDir.resolve("existing.txt");
        Files.writeString(file, "old content");
        String newCode = "new content";

        FileUtil.saveCode(file.toString(), newCode);

        assertEquals(newCode, Files.readString(file));
    }
}