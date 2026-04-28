package ru.asmisloff;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Параметры конфигурации.
 */
@Log4j2
public class Props {

    private static final String BASE_URL = "base-url";
    private static final String MODEL = "model";
    private static final String API_KEY = "api-key";
    private static final String PROMPT_FILENAME = "prompt-filename";
    private static final String ANSWER_FILENAME = "answer-filename";

    /**
     * URL OpenAI-совместимого API.
     */
    @Getter
    private static String baseUrl;

    /**
     * Наименование модели.
     */
    @Getter
    private static String model;

    /**
     * Ключ API.
     */
    @Getter
    private static String apiKey;

    /**
     * Имя файла с промптом.
     */
    @Getter
    private static String promptFileName = "prompt.md";

    /**
     * Имя файла для записи ответа модели.
     */
    @Getter
    private static String answerFileName = "answer.md";

    static {
        var yaml = new Yaml();
        try (var ins = Files.newInputStream(Path.of("lm-agent.yml"))) {
            Map<String, Object> props = yaml.load(ins);
            baseUrl = props.get(BASE_URL).toString();
            model = props.get(MODEL).toString();
            apiKey = props.get(API_KEY).toString();
            promptFileName = props.get(PROMPT_FILENAME).toString();
            answerFileName = props.get(ANSWER_FILENAME).toString();
            validate();
        } catch (IOException e) {
            log.error("Не удалось загрузить файл конфигурации");
            System.exit(1);
        }
    }

    private static void validate() {
        final String NO_PARAM = "Не задан параметр ";
        if (baseUrl == null) {
            throw new IllegalStateException(NO_PARAM + "base-url");
        }
        if (model == null) {
            throw new IllegalStateException(NO_PARAM + "model");
        }
        if (apiKey == null) {
            throw new IllegalStateException(NO_PARAM + "api-key");
        }
    }
}
