package ru.asmisloff;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для {@link Prompt}.
 */
class PromptTest {

    @TempDir
    Path tempDir;

    private Path promptFile;

    @BeforeEach
    void setUp() {
        promptFile = tempDir.resolve("test_prompt.txt");
    }

    @Test
    @DisplayName("Построение промпта без тегов замены")
    void buildPromptWithoutTags() throws IOException {
        String content = "Line 1\nLine 2\nLine 3";
        Files.write(promptFile, content.getBytes());

        Prompt builder = new Prompt(promptFile);
        List<String> result = builder.getUserLines();

        assertEquals(List.of("Line 1", "Line 2", "Line 3"), result);
    }

    @Test
    @DisplayName("Обработка тега \\i для вставки содержимого файла")
    void handleFileContentTag() throws IOException {
        Path externalFile = tempDir.resolve("external.txt");
        Files.write(externalFile, "External file content".getBytes());
        String promptContent = "\\i " + externalFile;
        Files.write(promptFile, promptContent.getBytes());

        Prompt builder = new Prompt(promptFile);
        List<String> result = builder.getUserLines();

        assertEquals(1, result.size());
        assertEquals("External file content", result.get(0));
    }

    @Test
    @DisplayName("Обработка тега \\i с Java-файлом: обрамление в markdown-блок")
    void handleJavaFileWithTag() throws IOException {
        Path javaFile = tempDir.resolve("Example.java");
        String javaContent = "public class Example { public static void main(String[] args) {} }";
        Files.write(javaFile, javaContent.getBytes());

        String promptContent = "\\i " + javaFile;
        Files.write(promptFile, promptContent.getBytes());

        Prompt prompt = new Prompt(promptFile);
        List<String> result = prompt.getUserLines();

        assertEquals("""
                         ```Java
                         public class Example { public static void main(String[] args) {} }
                         ```""",
                     String.join("\n", result)
        );
    }

    @Test
    @DisplayName("Обработка пустого файла промпта")
    void handleEmptyPromptFile() throws IOException {
        Files.write(promptFile, new byte[0]);

        Prompt builder = new Prompt(promptFile);
        List<String> result = builder.getUserLines();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Обработка неизвестного тега замены")
    void handleUnknownTag() throws IOException {
        String promptContent = "Text \\unknownTag some/path More text";
        Files.write(promptFile, promptContent.getBytes());

        Prompt builder = new Prompt(promptFile);
        List<String> result = builder.getUserLines();

        assertEquals(1, result.size());
        assertEquals("Text \\unknownTag some/path More text", result.get(0));
    }

    @Test
    @DisplayName("Обработка тега c переводом строки в конце и пробелами вначале")
    void handleTagAtEndOfLine() throws IOException {
        Path externalFile = tempDir.resolve("end.txt");
        Files.write(externalFile, "External file content".getBytes());

        String promptContent = String.format("  \\i %s\n", externalFile);
        Files.write(promptFile, promptContent.getBytes());

        Prompt builder = new Prompt(promptFile);
        List<String> result = builder.getUserLines();

        assertEquals(1, result.size());
        assertEquals("External file content", result.get(0));
    }

    @Test
    @DisplayName("Обработка несуществующего файла для тега \\i")
    void handleNonExistentFileForTag() throws IOException {
        String promptContent = "\\i /non/existent/path.txt More text";
        Files.write(promptFile, promptContent.getBytes());

        assertThrows(Exception.class, () -> new Prompt(promptFile));
    }
}