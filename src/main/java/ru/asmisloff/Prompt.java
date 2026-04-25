package ru.asmisloff;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Character.isWhitespace;

/**
 * Класс для загрузки и обработки промптов из файлов.
 */
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
    private final Map<String, Function<String, String>> replacementFunctions = Map.of(
        "\\i", this::getFileContent
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
            userLines.add(replace != null ? replace.apply(line) : line);
        }
    }

    /**
     * Определяет функцию замены для строки, если она начинается с тега.
     *
     * @param line строка для анализа.
     * @return функция замены или {@code null}, если тег не найден.
     */
    @Nullable
    private Function<String, String> getReplacementFunction(@NotNull String line) {
        var tagBegin = 0;
        while (tagBegin < line.length() && isWhitespace(line.charAt(tagBegin))) {
            ++tagBegin;
        }
        if (line.charAt(tagBegin) != '\\') {
            return null;
        }
        var tagEnd = tagBegin + 1;
        while (tagEnd < line.length() && !isWhitespace(line.charAt(tagEnd))) {
            ++tagEnd;
        }

        idx = tagEnd;
        while (idx != line.length() && isWhitespace(line.charAt(idx))) {
            ++idx;
        }
        return replacementFunctions.get(line.substring(tagBegin, tagEnd));
    }

    private String getFileContent(String line) { // todo: markdown
        var end = line.length();
        while (end > 0 && line.charAt(end - 1) == '\n') {
            --end;
        }
        var path = Path.of(line.substring(idx, end));
        return FileUtil.readString(path);
    }
}
