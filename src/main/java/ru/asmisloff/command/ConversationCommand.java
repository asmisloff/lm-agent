package ru.asmisloff.command;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.*;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import ru.asmisloff.Prompt;
import ru.asmisloff.Props;
import ru.asmisloff.tool.ToolRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Реализует диалог с LLM-моделью через OpenAI-совместимое API.
 * <p>
 * Команда загружает историю диалога из JSON-файла, пополняет её содержимым из файла промпта и отправляет запрос к модели.
 * Если модель отвечает вызовами инструментов ({@code tool_calls}), команда исполняет их и повторяет цикл, пока модель не вернёт
 * финальный текстовый ответ.
 * <p>
 * Результат диалога (полная история сообщений) сохраняется обратно в JSON-файл. Файл истории задаётся опциональным аргументом;
 * если аргумент опущен, используется {@code history-file} из {@code lm-agent.yml} (по умолчанию {@code hist.json}).
 * Если расширение файла не указано, автоматически подставляется {@code .json}.
 */
@Log4j2
public class ConversationCommand implements Command {

    private final ObjectMapper objectMapper;
    private final Prompt prompt;
    private final String model;
    private final OpenAIClient client;
    private final String historyFileName;

    public ConversationCommand(ObjectMapper objectMapper, Props props) {
        this.objectMapper = objectMapper;
        this.prompt = new Prompt(Path.of(props.getPromptFileName()), props);

        this.model = prompt.getModel() != null
                ? prompt.getModel()
                : props.getModel();

        this.client = new OpenAIOkHttpClient.Builder()
                .baseUrl(props.getBaseUrl())
                .apiKey(props.getApiKey())
                .build();

        this.historyFileName = props.getHistoryFileName();
    }

    @Override
    @SneakyThrows
    public void exec(String... args) {
        var historyFilename = resolveHistoryFilename(args);

        var history = new History(historyFilename);
        boolean toolCallsExist = send(history);
        while (toolCallsExist) {
            toolCallsExist = send(history);
        }

        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(history.file, history.paramsBuilder.build().messages());
    }

    /**
     * Определяет имя файла истории на основе аргументов командной строки и конфигурации.
     *
     * @param args аргументы команды (не более одного)
     * @return имя файла с расширением {@code .json}. Если расширение не было указано явно, оно будет добавлено методом.
     * @throws IllegalArgumentException если передано более одного аргумента.
     */
    private String resolveHistoryFilename(String... args) {
        if (args.length > 1) {
            throw new IllegalArgumentException("Ожидается не более одного аргумента: [файл истории]");
        }

        String filename;
        if (args.length == 1 && args[0] != null && !args[0].isBlank()) {
            filename = args[0];
        } else {
            filename = historyFileName;
        }

        if (!filename.endsWith(".json")) {
            filename += ".json";
        }

        return filename;
    }

    private boolean send(History history) {
        log.info("Отправка запроса к {}", model);

        var modelResponseMessage = client.chat().completions().create(history.paramsBuilder.build())
                .choices()
                .get(0)
                .message();
        var toolCalls = modelResponseMessage.toolCalls().orElse(null);
        if (toolCalls != null) {
            var assistantMessageBuilder = ChatCompletionAssistantMessageParam.builder();
            assistantMessageBuilder.toolCalls(toolCalls);
            modelResponseMessage.content().ifPresent(content -> {
                System.out.println(content);
                assistantMessageBuilder.content(content);
            });
            history.paramsBuilder.addMessage(ChatCompletionMessageParam.ofAssistant(assistantMessageBuilder.build()));

            for (var toolCall : toolCalls) {
                toolCall.function()
                        .ifPresent(func -> {
                            var result = ToolRegistry.execTool(func.function());
                            if (result != null) {
                                history.paramsBuilder.addMessage(ChatCompletionMessageParam.ofTool(
                                        ChatCompletionToolMessageParam.builder()
                                                .toolCallId(func.id())
                                                .content(result)
                                                .build()
                                ));
                            }
                        });
            }
        } else {
            modelResponseMessage.content().ifPresent(content -> {
                System.out.println(content);
                history.paramsBuilder.addAssistantMessage(content);
            });
        }
        return toolCalls != null;
    }


    /**
     * История диалога с моделью
     */
    private class History {

        private final File file;
        private final ChatCompletionCreateParams.Builder paramsBuilder;

        private History(String filename) {
            file = getOrCreateFile(Path.of(filename));

            paramsBuilder = ChatCompletionCreateParams.builder().model(model);
            paramsBuilder.addSystemMessage(prompt.getSystemPrompt());
            for (var toolClass : ToolRegistry.getKnownToolClasses()) {
                paramsBuilder.addTool(toolClass);
            }

            try {
                objectMapper
                        .readValue(file, new TypeReference<List<ChatCompletionMessageParam>>() {})
                        .forEach(paramsBuilder::addMessage);
            } catch (IOException ex) {
                log.error("Некорректная структура файла истории", ex);
                throw new IllegalStateException(ex);
            }

            for (var block : prompt.getConversation()) {
                var lines = new StringBuilder();
                if (block.role() == Prompt.Role.USER) {
                    block.lines().forEach(lines::append);
                }
                paramsBuilder.addUserMessage(lines.toString());
            }
        }

        private static File getOrCreateFile(Path path) {
            if (!Files.exists(path)) {
                try {
                    Files.writeString(path, "[]", StandardOpenOption.CREATE_NEW);
                } catch (IOException e) {
                    log.error("Не удалось создать файл {}", path);
                    throw new RuntimeException(e);
                }
            }
            return path.toFile();
        }
    }
}
