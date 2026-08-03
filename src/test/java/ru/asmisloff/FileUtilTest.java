package ru.asmisloff;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.asmisloff.command.FullReplacePatch;
import ru.asmisloff.command.SearchReplacePatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FileUtilTest {

    /**
     * Временная директория для тестов.
     */
    @TempDir
    Path tempDir;

    // ======== writeString ========

    @Test
    @DisplayName("Запись в несуществующий файл — файл создаётся и содержит переданную строку")
    void writeString_fileDoesNotExist_createsFileAndWritesContent() throws IOException {
        Path file = tempDir.resolve("newFile.txt");
        String expected = "Hello, writeString!";

        FileUtil.writeString(file, expected);

        assertTrue(Files.exists(file), "Файл должен быть создан");
        assertEquals(expected, Files.readString(file));
    }

    // ======== find ========

    @Test
    @DisplayName("Поиск существующего файла по полному совпадению (без учета регистра)")
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

    @Test
    @DisplayName("Поиск по частичному совпадению имени")
    void find_partialPatternMatch_returnsMatchingFiles() throws IOException {
        var expected = Files.createFile(tempDir.resolve("apple-pie.txt"));
        Files.createFile(tempDir.resolve("banana-cake.txt"));
        Files.createFile(tempDir.resolve("cherry-tart.txt"));

        var out = new StringBuilder();
        FileUtil.find(tempDir, "pie", out);
        out.setLength(out.length() - 1);

        assertEquals(expected.toString(), out.toString());
    }

    @Test
    @DisplayName("Пустой результат, если совпадений нет")
    void find_noMatchingFiles_returnsEmptyList() throws IOException {
        Files.createFile(tempDir.resolve("data.json"));
        Files.createFile(tempDir.resolve("config.yml"));

        var out = new StringBuilder();
        FileUtil.find(tempDir, "xml", out);

        assertTrue(out.isEmpty());
    }

    @Test
    @DisplayName("Игнорирует поддиректории, возвращает только файлы")
    void find_ignoresDirectories_returnsOnlyFiles() throws IOException {
        Files.createFile(tempDir.resolve("file.txt"));
        Files.createDirectory(tempDir.resolve("subdir"));

        var out = new StringBuilder();
        FileUtil.find(tempDir, "subdir", out);

        assertTrue(out.isEmpty());
    }

    @Test
    @DisplayName("Ищет во всех поддиректориях (рекурсивно)")
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

    @Test
    @DisplayName("Корректно обрабатывает пустой паттерн")
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

    // ======== extractCode — FullReplacePatch ========

    @Test
    @DisplayName("Несколько поддерживаемых языков — каждый извлекается с корректным префиксом комментария")
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
        var result = FileUtil.extractCode(file);

        assertEquals(4, result.size());
        assertInstanceOf(FullReplacePatch.class, result.get("A.java"));
        assertEquals("Java code" + System.lineSeparator(), ((FullReplacePatch) result.get("A.java")).getReplace());
        assertEquals("sql query" + System.lineSeparator(), ((FullReplacePatch) result.get("B.sql")).getReplace());
        assertEquals("kotlin kode" + System.lineSeparator(), ((FullReplacePatch) result.get("C.kt")).getReplace());
        assertEquals("xml code" + System.lineSeparator(), ((FullReplacePatch) result.get("D.xml")).getReplace());
    }

    @Test
    @DisplayName("Строка комментария без пути после открывающего маркера — блок пропускается")
    void extractCode_missingFilePath_skipped() throws IOException {
        String content = "```java\n// \n```\ncode\n```\n";
        Path file = createMarkdownFile(content);
        var result = FileUtil.extractCode(file);
        assertTrue(result.isEmpty(), "Без указания файла блок игнорируется");
    }

    // ======== extractCode — SearchReplacePatch ========

    @Test
    @DisplayName("Один SearchReplacePatch для одного файла")
    void extractCode_singleSearchReplace_parsedCorrectly() throws IOException {
        String nl = System.lineSeparator();
        String content = """
                [PATCH_BEGIN: src/main/Foo.java]
                --- SEARCH ---
                    int x = 1;
                --- REPLACE ---
                    int x = 42;
                [PATCH_END]
                """;
        Path file = createMarkdownFile(content);
        var result = FileUtil.extractCode(file);

        assertEquals(1, result.size());
        var patch = result.get("src/main/Foo.java");
        assertNotNull(patch);
        assertInstanceOf(SearchReplacePatch.class, patch);
        var srPatch = (SearchReplacePatch) patch;
        assertEquals(1, srPatch.size());
        assertEquals("    int x = 1;" + nl, srPatch.getSearch(0));
        assertEquals("    int x = 42;" + nl, srPatch.getReplace(0));
    }

    @Test
    @DisplayName("Несколько SearchReplacePatch для одного файла")
    void extractCode_multipleSearchReplaceForSameFile_allParsed() throws IOException {
        String nl = System.lineSeparator();
        String content = """
                [PATCH_BEGIN: com/example/Bar.java]
                --- SEARCH ---
                foo()
                --- REPLACE ---
                bar()
                [PATCH_END]
                [PATCH_BEGIN: com/example/Bar.java]
                --- SEARCH ---
                old text
                --- REPLACE ---
                new text
                [PATCH_END]
                """;
        Path file = createMarkdownFile(content);
        var result = FileUtil.extractCode(file);

        assertEquals(1, result.size());
        var patch = result.get("com/example/Bar.java");
        assertNotNull(patch);
        assertInstanceOf(SearchReplacePatch.class, patch);
        var srPatch = (SearchReplacePatch) patch;
        assertEquals(2, srPatch.size());
        assertEquals("foo()" + nl, srPatch.getSearch(0));
        assertEquals("bar()" + nl, srPatch.getReplace(0));
        assertEquals("old text" + nl, srPatch.getSearch(1));
        assertEquals("new text" + nl, srPatch.getReplace(1));
    }

    @Test
    @DisplayName("SearchReplacePatch для разных файлов — каждый в своём ключе")
    void extractCode_searchReplaceForDifferentFiles_separateKeys() throws IOException {
        String nl = System.lineSeparator();
        String content = """
                [PATCH_BEGIN: a/A.java]
                --- SEARCH ---
                aaa
                --- REPLACE ---
                AAA
                [PATCH_END]
                [PATCH_BEGIN: b/B.java]
                --- SEARCH ---
                bbb
                --- REPLACE ---
                BBB
                [PATCH_END]
                """;
        Path file = createMarkdownFile(content);
        var result = FileUtil.extractCode(file);

        assertEquals(2, result.size());
        assertTrue(result.containsKey("a/A.java"));
        assertTrue(result.containsKey("b/B.java"));
        assertEquals("aaa" + nl, ((SearchReplacePatch) result.get("a/A.java")).getSearch(0));
        assertEquals("bbb" + nl, ((SearchReplacePatch) result.get("b/B.java")).getSearch(0));
    }

    @Test
    @DisplayName("FullReplace и SearchReplace в одном файле — оба типа извлекаются")
    void extractCode_mixedPatchTypes_bothExtracted() throws IOException {
        String content = """
                ```java
                //MyClass.java
                public class MyClass {}
                ```
                
                [PATCH_BEGIN: OtherClass.java]
                --- SEARCH ---
                void old() {}
                --- REPLACE ---
                void new_() {}
                [PATCH_END]
                """;
        var file = createMarkdownFile(content);
        var result = FileUtil.extractCode(file);

        assertEquals(2, result.size());
        assertInstanceOf(FullReplacePatch.class, result.get("MyClass.java"));
        assertInstanceOf(SearchReplacePatch.class, result.get("OtherClass.java"));
    }

    @Test
    @DisplayName("PATCH_BEGIN без пути — патч пропускается")
    void extractCode_patchBeginWithoutPath_skipped() throws IOException {
        String content = """
                [PATCH_BEGIN:]
                --- SEARCH ---
                something
                --- REPLACE ---
                else
                [PATCH_END]
                """;
        var file = createMarkdownFile(content);
        var result = FileUtil.extractCode(file);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Пустой файл — пустой результат")
    void extractCode_emptyFile_returnsEmpty() throws IOException {
        var file = createMarkdownFile("");
        var result = FileUtil.extractCode(file);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("SearchReplacePatch сохраняет оригинальные отступы")
    void extractCode_searchReplace_preservesIndentation() throws IOException {
        var nl = System.lineSeparator();
        var content = """
                [PATCH_BEGIN: Indented.java]
                --- SEARCH ---
                        int x = 0;
                        return x;
                --- REPLACE ---
                        int x = 99;
                        return x;
                [PATCH_END]
                """;
        var file = createMarkdownFile(content);
        var result = FileUtil.extractCode(file);

        SearchReplacePatch patch = (SearchReplacePatch) result.get("Indented.java");
        assertNotNull(patch);
        assertEquals("        int x = 0;" + nl + "        return x;" + nl, patch.getSearch(0));
        assertEquals("        int x = 99;" + nl + "        return x;" + nl, patch.getReplace(0));
    }

    // ======== buildProjectTree ========

    @Test
    @DisplayName("Пустая директория без файлов — только имя корня")
    void buildProjectTree_emptyDir_returnsOnlyRoot() {
        String tree = FileUtil.buildProjectTree(tempDir);

        assertEquals(tempDir.getFileName().toString() + "\n", tree);
    }

    @Test
    @DisplayName("Файлы неподдерживаемых расширений игнорируются")
    void buildProjectTree_unsupportedExtensions_ignored() throws IOException {
        Files.createFile(tempDir.resolve("readme.txt"));
        Files.createFile(tempDir.resolve("script.py"));
        Files.createFile(tempDir.resolve("image.png"));

        String tree = FileUtil.buildProjectTree(tempDir);

        assertEquals(tempDir.getFileName().toString() + "\n", tree);
    }

    @Test
    @DisplayName("Файлы поддерживаемых расширений попадают в дерево")
    void buildProjectTree_supportedExtensions_included() throws IOException {
        Files.createFile(tempDir.resolve("App.java"));
        Files.createFile(tempDir.resolve("config.xml"));
        Files.createFile(tempDir.resolve("messages.properties"));

        String tree = FileUtil.buildProjectTree(tempDir);

        String expected = tempDir.getFileName() + "\n"
                + "├── App.java\n"
                + "├── config.xml\n"
                + "└── messages.properties\n";
        assertEquals(expected, tree);
    }

    @Test
    @DisplayName("Директория target исключается из дерева")
    void buildProjectTree_excludesTargetDir() throws IOException {
        Path targetDir = tempDir.resolve("target");
        Files.createDirectory(targetDir);
        Files.createFile(targetDir.resolve("generated.java"));
        Files.createFile(tempDir.resolve("App.java"));

        String tree = FileUtil.buildProjectTree(tempDir);

        String expected = tempDir.getFileName() + "\n"
                + "└── App.java\n";
        assertEquals(expected, tree);
    }

    @Test
    @DisplayName("Директория .git исключается из дерева")
    void buildProjectTree_excludesGitDir() throws IOException {
        Path gitDir = tempDir.resolve(".git");
        Files.createDirectory(gitDir);
        Files.createFile(gitDir.resolve("config.xml"));
        Files.createFile(tempDir.resolve("App.java"));

        String tree = FileUtil.buildProjectTree(tempDir);

        String expected = tempDir.getFileName() + "\n"
                + "└── App.java\n";
        assertEquals(expected, tree);
    }

    @Test
    @DisplayName("Вложенные директории корректно отображаются")
    void buildProjectTree_nestedDirs_renderedCorrectly() throws IOException {
        Path subDir = tempDir.resolve("sub");
        Files.createDirectory(subDir);
        Files.createFile(subDir.resolve("Child.java"));
        Files.createFile(tempDir.resolve("Root.java"));

        String tree = FileUtil.buildProjectTree(tempDir);

        String expected = tempDir.getFileName() + "\n"
                + "├── sub\n"
                + "│   └── Child.java\n"
                + "└── Root.java\n";
        assertEquals(expected, tree);
    }

    @Test
    @DisplayName("Глубокая вложенность — несколько уровней")
    void buildProjectTree_deepNesting_renderedCorrectly() throws IOException {
        Path l1 = tempDir.resolve("src");
        Path l2 = l1.resolve("main");
        Path l3 = l2.resolve("java");
        Files.createDirectories(l3);
        Files.createFile(l3.resolve("Deep.java"));
        Files.createFile(l2.resolve("resources.xml"));

        String tree = FileUtil.buildProjectTree(tempDir);

        String expected = tempDir.getFileName() + "\n"
                + "└── src\n"
                + "    └── main\n"
                + "        ├── java\n"
                + "        │   └── Deep.java\n"
                + "        └── resources.xml\n";
        assertEquals(expected, tree);
    }

    @Test
    @DisplayName("Директории перед файлами, обе группы по алфавиту")
    void buildProjectTree_sorting_directoriesFirstThenAlphabetical() throws IOException {
        Files.createFile(tempDir.resolve("B.java"));
        Path dirA = tempDir.resolve("a_dir");
        Path dirZ = tempDir.resolve("z_dir");
        Files.createDirectory(dirA);
        Files.createDirectory(dirZ);
        Files.createFile(dirA.resolve("X.xml"));
        Files.createFile(dirZ.resolve("M.properties"));
        Files.createFile(tempDir.resolve("C.java"));

        String tree = FileUtil.buildProjectTree(tempDir);

        String expected = tempDir.getFileName() + "\n"
                + "├── a_dir\n"
                + "│   └── X.xml\n"
                + "├── z_dir\n"
                + "│   └── M.properties\n"
                + "├── B.java\n"
                + "└── C.java\n";
        assertEquals(expected, tree);
    }

    @Test
    @DisplayName("Смешанный сценарий: исключаемые и включаемые файлы/директории")
    void buildProjectTree_mixedScenario_correctOutput() throws IOException {
        // Поддерживаемые
        Files.createFile(tempDir.resolve("pom.xml"));
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.createFile(src.resolve("Main.java"));
        Files.createFile(src.resolve("Messages.properties"));

        // Исключаемые
        Path target = tempDir.resolve("target");
        Files.createDirectory(target);
        Files.createFile(target.resolve("generated.java"));

        Path git = tempDir.resolve(".git");
        Files.createDirectory(git);
        Files.createFile(git.resolve("config.xml"));

        // Неподдерживаемое расширение
        Files.createFile(tempDir.resolve("notes.txt"));

        String tree = FileUtil.buildProjectTree(tempDir);

        String expected = tempDir.getFileName() + "\n"
                + "├── src\n"
                + "│   ├── Main.java\n"
                + "│   └── Messages.properties\n"
                + "└── pom.xml\n";
        assertEquals(expected, tree);
    }

    @Test
    @Disabled
    void buildProjectTree_actual() {
        System.out.println(FileUtil.buildProjectTree(FileUtil.cwd()));
    }

    // ======== вспомогательные методы ========

    private Path createMarkdownFile(String content) throws IOException {
        Path file = tempDir.resolve("test.md");
        Files.writeString(file, content);
        return file;
    }
}
