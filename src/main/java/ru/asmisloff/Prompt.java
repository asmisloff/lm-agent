package ru.asmisloff;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.asmisloff.tool.AskUserTool;

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
     * Блоки разговора в порядке следования.
     */
    @Getter
    private final List<MessageBlock> conversation = new ArrayList<>();

    /**
     * Функции замены. Ключ - тег, значение - функция, принимающая исходную строку и возвращающая замененную.
     */
    private final Map<String, Consumer<String>> replacementFunctions = Map.of(
            "\\i", this::addFileContent,
            "\\m", this::setModel,
            "\\s", this::setSystemPrompt
    );

    /**
     * Поддерживаемые расширения файлов c информацией о markdown-блоке и стиле комментария для пути.
     */
    @Getter
    private static final List<FileTypeAttributes> fileTypeAttributes = List.of(
            new FileTypeAttributes(".java", "```java", "//", ""),
            new FileTypeAttributes(".kt", "```kotlin", "//", ""),
            new FileTypeAttributes(".sql", "```sql", "--", ""),
            new FileTypeAttributes(".xml", "```xml", "<!--", "-->")
    );

    private final Props props;

    private int idx = 0;

    /**
     * Временный список для раскрытия тегов перед разбором ASK_USER-блоков.
     */
    private final List<String> expandedLines;

    /**
     * Создает промпт, загружая шаблон из файла, раскрывая теги и разбирая блоки ASK_USER.
     *
     * @param promptFilePath путь к файлу с шаблоном промпта.
     * @param props          конфигурация приложения.
     * @throws IllegalStateException если файл не может быть прочитан или путь некорректен.
     */
    public Prompt(@NotNull Path promptFilePath, @NotNull Props props) {
        this.props = props;
        var template = FileUtil.readLines(promptFilePath);

        expandedLines = new ArrayList<>();
        for (var line : template) {
            var replace = getReplacementFunction(line);
            if (replace != null) {
                replace.accept(line);
            } else {
                expandedLines.add(line);
            }
        }

        var currentRole = Role.USER;
        var currentBlock = new ArrayList<String>();
        for (var line : expandedLines) {
            if (line.contains(AskUserTool.MARKER_OPEN)) {
                flushBlock(currentBlock, currentRole);
                currentRole = Role.ASSISTANT;
            } else if (line.contains(AskUserTool.MARKER_CLOSE)) {
                flushBlock(currentBlock, currentRole);
                currentRole = Role.USER;
            } else {
                currentBlock.add(line);
            }
        }
        flushBlock(currentBlock, currentRole);

        if (systemPrompt == null) {
            systemPrompt = props.getSystemPrompts().get("code");
            if (systemPrompt != null) {
                log.debug("Системный промпт не задан явно. По умолчанию выбран code.");
            }
        }
    }

    private void flushBlock(List<String> block, Role role) {
        if (!block.isEmpty()) {
            conversation.add(new MessageBlock(role, List.copyOf(block)));
            block.clear();
        }
    }

    /**
     * Возвращает строковое представление промпта для предварительного просмотра.
     * Строки пользователя выводятся как есть, строки ассистента обрамляются
     * в маркеры {@link AskUserTool#MARKER_OPEN} и {@link AskUserTool#MARKER_CLOSE}.
     *
     * @return промпт для предварительного просмотра.
     */
    public String preview() {
        var sb = new StringBuilder();
        for (var block : conversation) {
            if (block.role() == Role.ASSISTANT) {
                sb.append(AskUserTool.MARKER_OPEN).append('\n');
            }
            for (var line : block.lines()) {
                sb.append(line).append('\n');
            }
            if (block.role() == Role.ASSISTANT) {
                sb.append(AskUserTool.MARKER_CLOSE).append('\n');
            }
        }
        return sb.toString();
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

    /**
     * Добавляет содержимое файла в expandedLines.
     * Для файлов с кодом оборачивает содержимое в markdown-блок, а путь к файлу вставляется
     * комментарием в первой строке блока (стиль комментария зависит от типа файла).
     *
     * @param line строка с тегом \i и путём к файлу
     */
    private void addFileContent(String line) {
        var path = Path.of(extractTagArgument(line));
        var fileName = path.toAbsolutePath().toString();
        fileTypeAttributes.stream()
                .filter(att -> fileName.endsWith(att.ext()))
                .findFirst()
                .ifPresentOrElse(
                        attributes -> {
                            var commentLine = "%s%s%s".formatted(attributes.commentPrefix, fileName, attributes.commentSuffix);
                            expandedLines.add(attributes.langMark);
                            expandedLines.add(commentLine);
                            expandedLines.add(FileUtil.getFileContent(path));
                            expandedLines.add("```");
                        },
                        () -> expandedLines.add(FileUtil.getFileContent(path))
                );
    }

    /**
     * Хранит атрибуты, специфичные для файла заданного типа.
     *
     * @param ext           расширение файла.
     * @param langMark      метка языка программирования для кодового блока в Markdown.
     * @param commentPrefix последовательность символов, открывающая комментарий в коде.
     * @param commentSuffix последовательность символов, закрывающая комментарий в коде.
     */
    public record FileTypeAttributes(String ext, String langMark, String commentPrefix, String commentSuffix) {
    }

    /**
     * Роль сообщения в разговоре.
     */
    public enum Role {
        USER, ASSISTANT
    }

    /**
     * Блок сообщений одной роли.
     */
    public record MessageBlock(Role role, List<String> lines) {
    }
}
