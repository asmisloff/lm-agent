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

    /**
     * Модель, выбранная через тег \m. Если не задана — null.
     */
    @Getter
    private String model;

    /**
     * Системный промпт, выбранный через тег \s. Если не задан — null.
     */
    @Getter
    private String systemPrompt;

    /**
     * Элементы пользовательского промпта.
     */
    @Getter
    private final List<String> userLines = new ArrayList<>();

    /**
     * Функции замены. Ключ - тег, значение - функция, принимающая исходную строку и возвращающая замененную.
     */
    private final Map<String, Consumer<String>> replacementFunctions = Map.of(
            "\\i", this::addFileContent,
            "\\m", this::setModel,
            "\\s", this::setSystemPrompt
    );

    /**
     * Поддерживаемые расширения файлов для обрамления в markdown-блок.
     */
    private static final List<ExtToLang> codeFileExtToMdTag = List.of(
            new ExtToLang(".java", "```Java"),
            new ExtToLang(".kt", "```Kotlin")
    );

    private final Props props;

    private int idx = 0;

    /**
     * Создает промпт, загружая шаблон из файла и применяя замены.
     *
     * @param promptFilePath путь к файлу с шаблоном промпта.
     * @param props          конфигурация приложения.
     * @throws IllegalStateException если файл не может быть прочитан или путь некорректен.
     */
    public Prompt(@NotNull Path promptFilePath, @NotNull Props props) {
        this.props = props;
        var template = FileUtil.readLines(promptFilePath);
        for (var line : template) {
            var replace = getReplacementFunction(line);
            if (replace != null) {
                replace.accept(line);
            } else {
                userLines.add(line);
            }
        }
        if (systemPrompt == null) {
            systemPrompt = props.getSystemPrompts().get("code");
            if (systemPrompt != null) {
                log.debug("Системный промпт не задан явно. По умолчанию выбран code.");
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
        if (idx == line.length()) {
            throw new IllegalStateException("Некорректная управляющая строка: %s".formatted(line));
        }
        return replacementFunctions.get(line.substring(tagBegin, tagEnd));
    }

    /**
     * Устанавливает модель из строки тега \m. Если указан псевдоним, заменяет его на реальное имя модели.
     *
     * @param line строка с тегом \m и именем модели или псевдонимом.
     */
    private void setModel(String line) {
        var modelName = extractTagArgument(line);
        model = props.getModelAliases().getOrDefault(modelName, modelName);
    }

    /**
     * Устанавливает системный промпт из строки тега \s.
     * Ищет промпт в словаре system-prompts из конфигурации.
     *
     * @param line строка с тегом \s и ключом системного промпта
     * @throws IllegalStateException если промпт не найден в словаре
     */
    private void setSystemPrompt(String line) {
        var key = extractTagArgument(line);
        var systemPrompts = props.getSystemPrompts();

        if (!systemPrompts.containsKey(key)) {
            throw new IllegalStateException("Системный промпт с ключом '%s' не найден в конфигурации".formatted(key));
        }

        systemPrompt = systemPrompts.get(key);
        log.debug("Системный промпт из промпта: {}", key);
    }

    /**
     * Извлекает аргумент из теговой строки.
     *
     * @param line теговая строка
     * @return аргумент без начальных и завершающих пробелов
     */
    private String extractTagArgument(String line) {
        var end = line.length();
        while (end > idx && isWhitespace(line.charAt(end - 1))) {
            --end;
        }
        return line.substring(idx, end);
    }

    private void addFileContent(String line) {
        var path = Path.of(extractTagArgument(line));
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

    private record ExtToLang(String ext, String lang) {}
}