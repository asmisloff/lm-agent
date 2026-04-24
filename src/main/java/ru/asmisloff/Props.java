package ru.asmisloff;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Параметры конфигурации.
 */
@Log4j2
public class Props {

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
    private static String promptFileName; // todo: по умолчанию?

    /**
     * Имя файла для записи ответа модели.
     */
    @Getter
    private static String answerFileName; // todo: по умолчанию?

    static {
        var properties = new Properties();
        try {
            properties.load(Files.newInputStream(Path.of("lm-agent.properties")));
            baseUrl = properties.getProperty("base.url");
            model = properties.getProperty("model");
            apiKey = properties.getProperty("api.key");
            promptFileName = properties.getProperty("prompt.filename");
            answerFileName = properties.getProperty("answer.filename");
            validate();
        } catch (IOException e) {
            log.error("Не удалось загрузить файл конфигурации");
            System.exit(1);
        } catch (IllegalStateException ex) {
            log.error(ex.getMessage());
            System.exit(1);
        }
    }

    private static void validate() {
        final String NO_PARAM = "Не задан параметр ";
        if (baseUrl == null) {
            throw new IllegalStateException(NO_PARAM + "base.url");
        }
        if (model == null) {
            throw new IllegalStateException(NO_PARAM + "model");
        }
        if (apiKey == null) {
            throw new IllegalStateException(NO_PARAM + "api.key");
        }
        if (promptFileName == null) {
            throw new IllegalStateException(NO_PARAM + "prompt.file.name");
        }
        if (answerFileName == null) {
            throw new IllegalStateException(NO_PARAM + "answer.file.name");
        }
    }
}
