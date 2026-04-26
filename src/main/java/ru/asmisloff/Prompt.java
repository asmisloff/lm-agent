package ru.asmisloff;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.Character.isWhitespace;

/**
 * Класс для загрузки и обработки промптов из файлов.
 */
@Log4j2
public class Prompt {

    private String system; // todo: системный. \review \test

    private List<String> dialog; // todo: диалог "пользователь-ассистент"

    /**
     * Элементы пользовательского промпта.
     */
    @Getter
    private final List<String> userLines = new ArrayList<>();

    /**
     * Функции замены. Ключ - тег, значение - функция, принимающая строку исходную строку и возвращающая замененную.
     */
    private final Map<String, Consumer<String>> replacementFunctions = Map.of(
        "\\i", this::addFileContent
        // todo: \m модель
        // \is include-search
    );

    /**
     * Поддерживаемые расширения файлов для обрамления в markdown-блок.
     */
    private static final List<ExtToLang> codeFileExtToMdTag = List.of(
        new ExtToLang(".java", "```Java"),
        new ExtToLang(".kt", "```Kotlin")
    );

    private int idx = 0;

    /**
     * Создает промпт, загружая шаблон из файла и применяя замены.
     *
     * @param promptFilePath путь к файлу с шаблоном промпта.
     * @throws IllegalStateException если файл не может быть прочитан или путь некорректен.
     */
    public Prompt(@NotNull Path promptFilePath) {
        var template = FileUtil.readLines(promptFilePath);
        for (var line : template) {
            var replace = getReplacementFunction(line);
            if (replace != null) {
                replace.accept(line);
            } else {
                userLines.add(line);
            }
        }
    }

    /**
     * Определяет функцию замены для строки, если она начинается с тега.
     *
     * @param line строка для анализа.
     * @return функция замены или {@code null}, если тег не найден.
     */
    @Nullable
    private Consumer<String> getReplacementFunction(@NotNull String line) {
        var tagBegin = 0;
        char c = '\0';
        while (tagBegin < line.length() && isWhitespace(c = line.charAt(tagBegin))) {
            ++tagBegin;
        }
        if (c != '\\') {
            return null;
        }
        var tagEnd = tagBegin;
        while (tagEnd < line.length() && !isWhitespace(line.charAt(tagEnd))) {
            ++tagEnd;
        }

        idx = tagEnd;
        while (idx < line.length() && isWhitespace(line.charAt(idx))) {
            ++idx;
        }
        return replacementFunctions.get(line.substring(tagBegin, tagEnd));
    }

    private void addFileContent(String line) {
        var end = line.length();
        while (end > 0 && line.charAt(end - 1) == '\n') {
            --end;
        }
        var path = Path.of(line.substring(idx, end));
        var mdTag = getMdTag(path.getFileName().toString());
        if (mdTag == null) {
            userLines.add(FileUtil.readString(path));
            return;
        }

        userLines.add(mdTag);
        userLines.add(FileUtil.readCode(path));
        userLines.add("```");
    }

    /**
     * Возвращает начальный тег markdown для фрагмента программного кода. Тег выбирается по расширению файла.
     *
     * @param fileName имя файла.
     * @return Тег или null, если расширение не поддерживается.
     */
    private @Nullable String getMdTag(@NotNull String fileName) {
        for (var entry : codeFileExtToMdTag) {
            if (fileName.endsWith(entry.ext)) {
                return entry.lang;
            }
        }
        return null;
    }

    private record ExtToLang(String ext, String lang) { }
}