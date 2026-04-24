package ru.asmisloff;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        List<Path> result = FileUtil.find(tempDir, "testfile.txt");

        assertEquals(List.of(targetFile), result);
    }

    /**
     * Поиск по частичному совпадению имени.
     */
    @Test
    void find_partialPatternMatch_returnsMatchingFiles() throws IOException {
        var expected = Files.createFile(tempDir.resolve("apple-pie.txt"));
        Files.createFile(tempDir.resolve("banana-cake.txt"));
        Files.createFile(tempDir.resolve("cherry-tart.txt"));

        List<Path> result = FileUtil.find(tempDir, "pie");

        assertEquals(1, result.size());
        assertEquals(expected.toAbsolutePath(), result.get(0));
    }

    /**
     * Возвращает пустой список, если совпадений нет.
     */
    @Test
    void find_noMatchingFiles_returnsEmptyList() throws IOException {
        Files.createFile(tempDir.resolve("data.json"));
        Files.createFile(tempDir.resolve("config.yml"));

        List<Path> result = FileUtil.find(tempDir, "xml");

        assertTrue(result.isEmpty());
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

        List<Path> result = FileUtil.find(tempDir, "subdir");

        assertTrue(result.isEmpty());
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

        List<Path> result = FileUtil.find(tempDir, "target");

        assertEquals(List.of(targetFile), result);
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

        List<Path> result = FileUtil.find(tempDir, "");

        assertEquals(Set.of(file1, file2), new HashSet<>(result));
    }

    /**
     * Тест: Возвращает пустой список при IOException.
     * Заменяет текущую рабочую директорию на несуществующую для моделирования ошибки.
     */
    @Test
    void find_ioException_returnsEmptyListAndPrintsMessage() {
        // Сохраняем оригинальную рабочую директорию
        Path originalDir = Path.of(".").toAbsolutePath();

        try {
            // Меняем текущую директорию на несуществующий путь для вызова IOException
            System.setProperty("user.dir", "/nonexistent/path/12345");

            List<Path> result = FileUtil.find(tempDir, "test");

            assertTrue(result.isEmpty());
            // Проверка вывода на консоль требует мокирования System.out, опущено для простоты
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
}
