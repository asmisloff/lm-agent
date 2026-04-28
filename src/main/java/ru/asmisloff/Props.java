package ru.asmisloff;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Параметры конфигурации.
 */
@Log4j2
public class Props { // todo: убрать статику

    private static final String BASE_URL = "base-url";
    private static final String MODEL = "model";
    private static final String API_KEY = "api-key";
    private static final String PROMPT_FILENAME = "prompt-filename";
    private static final String ANSWER_FILENAME = "answer-filename";
    private static final String MODEL_ALIASES = "model-aliases";
    private static final String SYSTEM_PROMPTS = "system-prompts";

    /**
     * URL OpenAI-совместимого API.
     */
    @Getter
    private static final String baseUrl;

    /**
     * Наименование модели.
     */
    @Getter
    private static final String model;

    /**
     * Ключ API.
     */
    @Getter
    private static final String apiKey;

    /**
     * Имя файла с промптом.
     */
    @Getter
    private static final String promptFileName;

    /**
     * Имя файла для записи ответа модели.
     */
    @Getter
    private static final String answerFileName;

    /**
     * Псевдонимы моделей.
     */
    @Getter
    private static final Map<String, String> modelAliases;

    /**
     * Системные промпты.
     */
    @Getter
    private static final Map<String, String> systemPrompts;

    static {
        var props = loadConfig();
        baseUrl = getStringOrElse(props, BASE_URL, null);
        model = getStringOrElse(props, MODEL, null);
        apiKey = getStringOrElse(props, API_KEY, null);
        promptFileName = getStringOrElse(props, PROMPT_FILENAME, "prompt.md");
        answerFileName = getStringOrElse(props, ANSWER_FILENAME, "answer.md");
        modelAliases = getDict(props, MODEL_ALIASES);
        systemPrompts = getDict(props, SYSTEM_PROMPTS);
        validate();
    }

    @Nullable
    private static String getStringOrElse(Map<String, Object> props, String key, String defaultValue) {
        return Optional.ofNullable(props.get(key))
                .map(value -> {
                    if (value instanceof String strValue) {
                        return strValue;
                    }
                    throw new IllegalStateException("Некорректное значение параметра %s: %s".formatted(key, value));
                })
                .orElse(defaultValue);
    }

    private static Map<String, Object> loadConfig() {
        try (var ins = Files.newInputStream(Path.of("lm-agent.yml"))) {
            return new Yaml().load(ins);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить файл конфигурации", e);
        }
    }

    private static Map<String, String> getDict(Map<String, Object> props, String configKey) {
        Object rawMap = props.get(configKey);
        if (rawMap instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String && entry.getValue() instanceof String)) {
                    throw new IllegalStateException(MessageFormat.format("Некорректная структура раздела {0}: {1}", configKey, entry));
                }
            }
            //noinspection unchecked (фактические типы проверены выше)
            return Collections.unmodifiableMap((Map<String, String>) map);
        }
        log.warn("В конфигурации отсутствует раздел {}", configKey);
        return Collections.emptyMap();
    }

    private static void validate() {
        final String NO_PARAM = "Не задан параметр ";
        if (baseUrl == null) {
            throw new IllegalStateException(NO_PARAM + BASE_URL);
        }
        if (model == null) {
            throw new IllegalStateException(NO_PARAM + MODEL);
        }
        if (apiKey == null) {
            throw new IllegalStateException(NO_PARAM + API_KEY);
        }
    }
}
