package ru.asmisloff;

import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Главный класс приложения.
 */
@Log4j2
public class App {

    /**
     * Точка входа в приложение.
     * <ul>
     *   <li>Без аргументов — запускает интерактивный режим с промптом.</li>
     *   <li>{@code -f <паттерн>} — ищет файлы по паттерну в текущем каталоге.</li>
     * </ul>
     *
     * @param args аргументы командной строки
     * @throws IllegalArgumentException если передан флаг {@code -f} без имени паттерна
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            execPrompt();
        } else if ("-f".equals(args[0])) {
            if (args.length != 2) {
                throw new IllegalArgumentException("Некорректный формат командной строки");
            }
            FileUtil.find(Path.of("."), args[1], System.out);
        }
    }

    /**
     * Читает промпт из файла, отправляет запрос к LLM и сохраняет ответ.
     */
    private static void execPrompt() {
        Props props = new Props();
        var prompt = new Prompt(Path.of(props.getPromptFileName()), props);
        log.debug(String.join("\n", prompt.getUserLines()));

        // Модель из тега \m имеет приоритет над настройками
        var model = prompt.getModel() != null ? prompt.getModel() : props.getModel();
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
     * Загрузить системный промпт по умолчанию из classpath-ресурса {@code /default-system-prompt.md}.
     *
     * @return содержимое промпта.
     * @throws UncheckedIOException  если произошла ошибка ввода-вывода.
     * @throws IllegalStateException если ресурс не найден.
     */
    private static synchronized String getDefaultSystemPrompt() {
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

    /**
     * Обрабатывает потоковый ответ от LLM: выводит токены в stdout и пишет в файл.
     *
     * @param completion    потоковый ответ.
     * @param writer        @{link Writer} для записи в файл ответа. Если {@code null}, запись в файл пропускается.
     * @param answerFileName имя файла ответа (используется только в сообщениях об ошибках).
     */
    private static void processCompletions(
            @NotNull StreamResponse<ChatCompletionChunk> completion,
            @Nullable Writer writer,
            String answerFileName
    ) {
        completion.stream()
                .map(chunk -> chunk.choices().get(0).delta().content().orElse(""))
                .forEach(message -> {
                    System.out.print(message);
                    if (writer != null) {
                        try {
                            writer.write(message);
                        } catch (IOException e) {
                            log.error("Ошибка записи в файл {}", answerFileName);
                        }
                    }
                });
    }
}