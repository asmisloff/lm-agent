package ru.asmisloff;

import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Log4j2
public class App {

    public static void main(String[] args) {
        var props = getProperties();
        var prompt = FileUtil.readString(Path.of(props.getProperty("prompt.file.name")));
        log.debug(prompt);
        var model = props.getProperty("model");

        var paramsBuilder = ChatCompletionCreateParams.builder()
                .model(model)
                .addSystemMessage("""
                        Ты опытный разработчик на Java.
                        Ты пишешь надежный, понятный и эффективный код. Комментарии и JavaDoc на русском языке, очень лаконично.
                        Ты выводишь только код, комментарии и JavaDoc в markdown. Без дополнительных пояснений."""
                )
                .addUserMessage(prompt);

        var client = new OpenAIOkHttpClient.Builder()
                .baseUrl(props.getProperty("base.url"))
                .apiKey(props.getProperty("api.key"))
                .build();
        log.info("Отправка запроса к {}", model);
        var answerFileName = props.getProperty("answer.file.name");
        try (var completion = client.chat().completions().createStreaming(paramsBuilder.build())) {
            try (var writer = Files.newBufferedWriter(Path.of(answerFileName))) { // todo: вынести в FileUtil
                processCompletions(completion, writer, answerFileName);
            } catch (IOException ex) {
                log.error("Ошибка доступа к файлу {}", answerFileName);
                processCompletions(completion, null, answerFileName);
            }
        }
        log.info("Завершено");
    }

    private static void processCompletions(
            @NotNull StreamResponse<ChatCompletionChunk> completion,
            @Nullable Writer writer,
            String answerFileName // todo: читать свойства при старте, заполнять глобальный singleton
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

    private static Properties getProperties() {
        try (var ins = Files.newInputStream(Path.of("./lm-agent.properties"))) {
            var props = new Properties();
            props.load(ins);
            return props;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать файл lm-agent.properties");
        }
    }
}
