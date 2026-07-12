package ru.asmisloff;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для {@link Prompt}.
 */
class PromptTest {

    @TempDir
    Path tempDir;

    private Path promptFile;
    private Props props;

    @BeforeEach
    void setUp() {
        promptFile = tempDir.resolve("test_prompt.txt");
        props = Mockito.mock(Props.class);
        Mockito.when(props.getSystemPrompts()).thenReturn(Collections.emptyMap());
        Mockito.when(props.getModelAliases()).thenReturn(Collections.emptyMap());
    }

    @Test
    @DisplayName("Построение промпта без тегов замены")
    void buildPromptWithoutTags() throws IOException {
        var content = "Line 1\nLine 2\nLine 3";
        Files.write(promptFile, content.getBytes());

        var prompt = new Prompt(promptFile, props);

        assertEquals("Line 1\nLine 2\nLine 3\n", prompt.preview());
    }

    @Test
    @DisplayName("Обработка тега \\i для вставки содержимого файла")
    void handleFileContentTag() throws IOException {
        var externalFile = tempDir.resolve("external.txt");
        Files.write(externalFile, "External file content".getBytes());
        var promptContent = "\\i " + externalFile;
        Files.write(promptFile, promptContent.getBytes());

        var prompt = new Prompt(promptFile, props);

        assertEquals("External file content\n", prompt.preview());
    }

    @Test
    @DisplayName("Обработка тега \\i с Java-файлом: обрамление в markdown-блок")
    void handleJavaFileWithTag() throws IOException {
        var javaFile = tempDir.resolve("Example.java");
        var javaContent = "public class Example { public static void main(String[] args) {} }";
        Files.write(javaFile, javaContent.getBytes());

        var promptContent = "\\i " + javaFile;
        Files.write(promptFile, promptContent.getBytes());

        var prompt = new Prompt(promptFile, props);

        var expected = "```java\n" +
                       "//" + javaFile.toAbsolutePath() + "\n" +
                       javaContent + "\n```\n";
        assertEquals(expected, prompt.preview());
    }

    @Test
    @DisplayName("Обработка тега \\i с SQL-файлом: обрамление в markdown-блок")
    void handleSqlFileWithTag() throws IOException {
        var sqlFile = tempDir.resolve("query.sql");
        var sqlContent = "SELECT * FROM users WHERE id = 1;";
        Files.write(sqlFile, sqlContent.getBytes());

        var promptContent = "\\i " + sqlFile;
        Files.write(promptFile, promptContent.getBytes());

        var prompt = new Prompt(promptFile, props);

        var expected = "```sql\n" +
                       "--" + sqlFile.toAbsolutePath() + "\n" +
                       sqlContent + "\n```\n";
        assertEquals(expected, prompt.preview());
    }

    @Test
    @DisplayName("Обработка тега \\i с XML-файлом: обрамление в markdown-блок")
    void handleXmlFileWithTag() throws IOException {
        var xmlFile = tempDir.resolve("config.xml");
        var xmlContent = "<?xml version=\"1.0\"?>\n<root><element>value</element></root>";
        Files.write(xmlFile, xmlContent.getBytes());

        var promptContent = "\\i " + xmlFile;
        Files.write(promptFile, promptContent.getBytes());

        var prompt = new Prompt(promptFile, props);

        var expected = "```xml\n" +
                       "<!--" + xmlFile.toAbsolutePath() + "-->\n" +
                       xmlContent + "\n```\n";
        assertEquals(expected, prompt.preview());
    }

    @Test
    @DisplayName("Обработка пустого файла промпта")
    void handleEmptyPromptFile() throws IOException {
        Files.write(promptFile, new byte[0]);

        var prompt = new Prompt(promptFile, props);

        assertEquals("", prompt.preview());
    }

    @Test
    @DisplayName("Обработка неизвестного тега замены")
    void handleUnknownTag() throws IOException {
        var promptContent = "Text \\unknownTag some/path More text";
        Files.write(promptFile, promptContent.getBytes());

        var prompt = new Prompt(promptFile, props);

        assertEquals("Text \\unknownTag some/path More text\n", prompt.preview());
    }

    @Test
    @DisplayName("Обработка тега c переводом строки в конце и пробелами вначале")
    void handleTagAtEndOfLine() throws IOException {
        var externalFile = tempDir.resolve("end.txt");
        Files.write(externalFile, "External file content".getBytes());

        var promptContent = String.format("  \\i %s\n", externalFile);
        Files.write(promptFile, promptContent.getBytes());

        var prompt = new Prompt(promptFile, props);

        assertEquals("External file content\n", prompt.preview());
    }

    @Test
    @DisplayName("Обработка несуществующего файла для тега \\i")
    void handleNonExistentFileForTag() throws IOException {
        var promptContent = "\\i /non/existent/path.txt More text";
        Files.write(promptFile, promptContent.getBytes());

        assertThrows(Exception.class, () -> new Prompt(promptFile, props));
    }
}
