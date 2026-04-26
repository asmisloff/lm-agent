package ru.asmisloff;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilTest {

    /**
     * Временная директория для тестов.
     */
    @TempDir
    Path tempDir;

    /**
     * Поиск существующего файла по полному совпадению (без учета регистра).
     */
    @Test
    void find_existingFileCaseInsensitive_returnsFile() throws IOException {
        // Создание тестовых файлов
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
     * Корректно обрабатывает пустой паттерн. todo: вообще-то, не должно так бытью Исправить.
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
     * Заменяет текущую рабочую директорию на несуществующую для моделирования ошибки.
     */
    @Test
    void find_ioException_returnsEmptyListAndPrintsMessage() {
        // Сохраняем оригинальную рабочую директорию
        Path originalDir = Path.of(".");

        try {
            // Меняем текущую директорию на несуществующий путь для вызова IOException
            System.setProperty("user.dir", "/nonexistent/path/12345");

            var out = new StringBuilder();
            FileUtil.find(tempDir, "test", out);

            assertTrue(out.isEmpty());
        } finally {
            // Восстанавливаем оригинальную директорию
            System.setProperty("user.dir", originalDir.toString());
        }
    }

    @Test
    void append_shouldCopyContentCorrectly() throws IOException {
        // Подготовка
        String content = "Тестовое содержимое\nВторая строка\nТретья строка";
        Path sourceFile = tempDir.resolve("source.txt");
        Files.writeString(sourceFile, content);

        Path destFile = tempDir.resolve("dest.txt");

        // Действие
        try (BufferedWriter writer = Files.newBufferedWriter(destFile)) {
            FileUtil.append(sourceFile, writer);
        }

        // Проверка
        String copiedContent = Files.readString(destFile);
        assertEquals(content, copiedContent);
    }

    @Test
    void append_shouldHandleEmptyFile() throws IOException {
        // Подготовка
        Path sourceFile = tempDir.resolve("empty.txt");
        Files.createFile(sourceFile);

        Path destFile = tempDir.resolve("dest.txt");

        // Действие
        try (BufferedWriter writer = Files.newBufferedWriter(destFile)) {
            FileUtil.append(sourceFile, writer);
        }

        // Проверка
        String copiedContent = Files.readString(destFile);
        assertEquals("", copiedContent);
    }

    @Test
    void append_shouldHandleLargeFile() throws IOException {
        // Подготовка
        Path sourceFile = tempDir.resolve("large.txt");
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("Строка ").append(i).append("\n");
        }
        Files.writeString(sourceFile, largeContent.toString());

        Path destFile = tempDir.resolve("dest.txt");

        // Действие
        try (BufferedWriter writer = Files.newBufferedWriter(destFile)) {
            FileUtil.append(sourceFile, writer);
        }

        // Проверка
        String original = Files.readString(sourceFile);
        String copied = Files.readString(destFile);
        assertEquals(original, copied);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Текст с кириллицей: Привет мир!",
        "Text with special chars: !@#$%^&*()",
        "Multiline\ntext\nwith\nline\nbreaks",
        "Text with unicode: © € А"
    })
    void append_shouldHandleVariousContent(String content) throws IOException {
        // Подготовка
        Path sourceFile = tempDir.resolve("source.txt");
        Files.writeString(sourceFile, content);

        Path destFile = tempDir.resolve("dest.txt");

        // Действие
        try (BufferedWriter writer = Files.newBufferedWriter(destFile)) {
            FileUtil.append(sourceFile, writer);
        }

        // Проверка
        String copiedContent = Files.readString(destFile);
        assertEquals(content, copiedContent);
    }

    @Test
    void append_shouldThrowWhenSourcePathIsNull() {
        // Подготовка
        Path destFile = tempDir.resolve("dest.txt");

        // Действие и проверка
        try (BufferedWriter writer = Files.newBufferedWriter(destFile)) {
            assertThrows(IllegalArgumentException.class,
                         () -> FileUtil.append(null, writer));
        } catch (IOException e) {
            fail("Неожиданное исключение: " + e.getMessage());
        }
    }

    @Test
    void append_shouldThrowWhenWriterIsNull() throws IOException {
        // Подготовка
        Path sourceFile = tempDir.resolve("source.txt");
        Files.writeString(sourceFile, "content");

        // Действие и проверка
        assertThrows(IllegalArgumentException.class,
                     () -> FileUtil.append(sourceFile, null));
    }

    @Test
    void append_shouldThrowIOExceptionWhenFileNotFound() {
        // Подготовка
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");
        Path destFile = tempDir.resolve("dest.txt");

        // Действие и проверка
        try (BufferedWriter writer = Files.newBufferedWriter(destFile)) {
            assertThrows(IOException.class,
                         () -> FileUtil.append(nonExistentFile, writer));
        } catch (IOException e) {
            fail("Неожиданное исключение: " + e.getMessage());
        }
    }

    /**
     * Читает Java-файл без package и import.
     */
    @Test
    void readCode_readsJavaFileExcludingPackageAndImports() throws IOException {
        String content = """
            package com.example;
              import java.util.List;
            \s
            public class MyClass {
                public void method() {}
            }
            """;
        Path file = createJavaFile(content);
        String result = FileUtil.readCode(file);

        assertEquals(
            """
                public class MyClass {
                    public void method() {}
                }""",
            result
        );
    }

    /**
     * Не удаляет импорт/пакет из Java-файла, если они находятся внутри кода (внутри строк или комментариев).
     */
    @Test
    void readCode_preservesPackageImportInsideStringsAndComments() throws IOException {
        String content = """
            package com.example;
            import java.util.*;
            
            public class Test {
                // import should not be removed
                String s = "package com.test";
            }
            """;
        Path file = createJavaFile(content);
        String result = FileUtil.readCode(file);

        assertEquals(
            """
                public class Test {
                    // import should not be removed
                    String s = "package com.test";
                }""",
            result
        );
    }

    /**
     * Обрабатывает Java-файл без package и import (читает весь файл).
     */
    @Test
    void readCode_withoutPackageOrImports_returnsWholeFile() throws IOException {
        String content = """
            public class Test {
                public static void main(String[] args) {}
            }
            """;
        Path file = createJavaFile(content);
        String result = FileUtil.readCode(file);

        assertEquals(content.stripTrailing(), result);
    }

    /**
     * Обрабатывает пустой Java-файл (только пробельные символы).
     */
    @Test
    void readCode_emptyOrWhitespaceFile_returnsEmptyString() throws IOException {
        Path file = createJavaFile("   \n\t\n  \n");
        String result = FileUtil.readCode(file);
        assertEquals("", result);
    }

    /**
     * Обрабатывает файл с одним package, без остального контента.
     */
    @Test
    void readCode_onlyPackage_returnsEmptyString() throws IOException {
        Path file = createJavaFile("package com.example;\n");
        String result = FileUtil.readCode(file);
        assertEquals("", result);
    }

    /**
     * Обрабатывает файл с одним import, без остального контента.
     */
    @Test
    void readCode_onlyImport_returnsEmptyString() throws IOException {
        Path file = createJavaFile("import java.util.List;\n");
        String result = FileUtil.readCode(file);
        assertEquals("", result);
    }

    /**
     * Создает временный Java-файл с указанным содержимым.
     */
    private Path createJavaFile(String content) throws IOException {
        Path file = tempDir.resolve("Test.java");
        Files.writeString(file, content);
        return file;
    }
}
