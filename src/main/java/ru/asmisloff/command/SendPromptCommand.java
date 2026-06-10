package ru.asmisloff.command;

import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.asmisloff.App;
import ru.asmisloff.Prompt;
import ru.asmisloff.Props;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Команда на отправку промпта модели.
 */
@Log4j2
public class SendPromptCommand implements Command {

    @Getter
    private final Props props;

    private final Prompt prompt;
    private final String model;

    /**
     * Создаёт команду, загружая промпт из файла и определяя модель.
     *
     * @param props настройки приложения
     */
    SendPromptCommand(@NotNull Props props) {
        this.props = props;
        prompt = new Prompt(Path.of(props.getPromptFileName()), props);
        model = prompt.getModel() != null ? prompt.getModel() : props.getModel();
    }

    /**
     * Отправляет промпт модели, используя потоковую передачу.
     * <p>Ответ одновременно выводится в stdout и сохраняется в файл, заданный в свойствах.</p>
     *
     * @param args аргументы команды (не используются)
     */
    @Override
    public void exec(String... args) {
        log.debug(String.join("\n", prompt.getUserLines()));
        log.info("Используемая модель: {}", model);

        var paramsBuilder = ChatCompletionCreateParams.builder()
                .model(model)
                .addSystemMessage(getSystemPrompt(prompt));

        prompt.getUserLines().forEach(paramsBuilder::addUserMessage);

        var client = new OpenAIOkHttpClient.Builder()
                .baseUrl(props.getBaseUrl())
                .apiKey(props.getApiKey())
                .build();
        log.info("Отправка запроса к {}", model);
        try (var completion = client.chat().completions().createStreaming(paramsBuilder.build())) {
            String answerFile = props.getAnswerFileName();
            try (var writer = Files.newBufferedWriter(Path.of(answerFile))) {
                processCompletions(completion, writer, answerFile);
            } catch (IOException ex) {
                log.error("Ошибка доступа к файлу {}", answerFile);
                processCompletions(completion, null, answerFile);
            }
        }
        log.info("Завершено");
    }

    /**
     * Возвращает системный промпт для использования в запросе.
     * Приоритеты:
     * <ol>
     *   <li>Промпт из тега \s в файле промпта</li>
     *   <li>Промпт по умолчанию, загружаемый из ресурса {@code /default-system-prompt.md}</li>
     * </ol>
     *
     * @param prompt загруженный промпт
     * @return системный промпт
     * @throws UncheckedIOException  если ресурс не удалось прочитать
     * @throws IllegalStateException если ресурс отсутствует в classpath
     */
    private static String getSystemPrompt(Prompt prompt) {
        String systemPrompt = prompt.getSystemPrompt();
        if (systemPrompt != null) {
            log.debug("Системный промпт: {}", systemPrompt);
            return systemPrompt;
        }
        log.debug("Системный промпт по умолчанию из ресурса");
        return getDefaultSystemPrompt();
    }

    /**
     * Обрабатывает потоковый ответ от LLM: выводит токены в stdout и пишет в файл.
     *
     * @param completion     потоковый ответ.
     * @param writer         {@link Writer} для записи в файл ответа. Если {@code null}, запись в файл пропускается.
     * @param answerFileName имя файла ответа (используется только в сообщениях об ошибках).
     */
    private static void processCompletions(
            @NotNull StreamResponse<ChatCompletionChunk> completion,
            @Nullable Writer writer,
            String answerFileName
    ) {
        completion.stream()
                .map(chunk -> chunk.choices().isEmpty()
                        ? ""
                        : chunk.choices().get(0).delta().content().orElse("")
                )
                .filter(choice -> !choice.isEmpty())
                .forEach(choice -> {
                    System.out.print(choice);
                    if (writer != null) {
                        try {
                            writer.write(choice);
                        } catch (IOException e) {
                            log.error("Ошибка записи в файл {}", answerFileName);
                        }
                    }
                });
    }

    /**
     * Загрузить системный промпт по умолчанию из classpath-ресурса {@code /default-system-prompt.md}.
     *
     * @return содержимое промпта.
     * @throws UncheckedIOException  если произошла ошибка ввода-вывода.
     * @throws IllegalStateException если ресурс не найден.
     */
    // todo: не нужно, убрать
    private static String getDefaultSystemPrompt() {
        try (var inputStream = App.class.getResourceAsStream("/default-system-prompt.md")) {
            if (inputStream == null) {
                throw new IllegalStateException("Ресурс /default-system-prompt.md не найден в classpath");
            }
            var prompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).strip();
            log.debug("Загружен системный промпт по умолчанию");
            return prompt;
        } catch (IOException e) {
            throw new UncheckedIOException("Ошибка чтения ресурса /default-system-prompt.md", e);
        }
    }
}