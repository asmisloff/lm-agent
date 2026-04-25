package ru.asmisloff;

import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Log4j2
public class App {

    public static void main(String[] args) {
        if (args.length == 0) {
            execPrompt();
        } else if (args[0].equals("-f")) {
            if (args.length != 2) {
                throw new IllegalArgumentException("Некорректный формат командной строки");
            }
            FileUtil.find(Path.of("."), args[1], System.out);
        }
    }

    private static void execPrompt() {
        var prompt = new Prompt(Path.of(Props.getPromptFileName()));
        log.debug(String.join("\n", prompt.getUserLines()));

        var paramsBuilder = ChatCompletionCreateParams.builder()
            .model(Props.getModel())
            .addSystemMessage("""
                                  Ты опытный разработчик на Java.
                                  Ты пишешь надежный, понятный и эффективный код. Комментарии и JavaDoc на русском языке, очень лаконично.
                                  Ты выводишь только код, комментарии и JavaDoc в markdown. Без дополнительных пояснений."""
            );
        prompt.getUserLines().forEach(paramsBuilder::addUserMessage);

        var client = new OpenAIOkHttpClient.Builder()
            .baseUrl(Props.getBaseUrl())
            .apiKey(Props.getApiKey())
            .build();
        log.info("Отправка запроса к {}", Props.getModel());
        try (var completion = client.chat().completions().createStreaming(paramsBuilder.build())) {
            try (var writer = Files.newBufferedWriter(Path.of(Props.getAnswerFileName()))) {
                processCompletions(completion, writer);
            } catch (IOException ex) {
                log.error("Ошибка доступа к файлу {}", Props.getAnswerFileName());
                processCompletions(completion, null);
            }
        }
        log.info("Завершено");
    }

    private static void processCompletions(
        @NotNull StreamResponse<ChatCompletionChunk> completion,
        @Nullable Writer writer
    ) {
        completion.stream()
            .map(chunk -> chunk.choices().get(0).delta().content().orElse(""))
            .forEach(message -> {
                System.out.print(message);
                if (writer != null) {
                    try {
                        writer.write(message);
                    } catch (IOException e) {
                        log.error("Ошибка записи в файл {}", Props.getAnswerFileName());
                    }
                }
            });
    }
}
