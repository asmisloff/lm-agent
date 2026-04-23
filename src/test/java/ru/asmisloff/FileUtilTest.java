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

    @TempDir
    Path tempDir;

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
