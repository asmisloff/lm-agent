package ru.asmisloff.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

/**
 * Инструмент-заглушка. Не выполняет никаких действий, только логирует вызов.
 * Используется для диагностики, когда LLM пытается вызвать неизвестный инструмент.
 */
@Log4j2
@NoArgsConstructor
@JsonDeserialize(using = NoOpToolDeserializer.class)
@JsonClassDescription("Инструмент-заглушка. Не выполняет действий, используется для диагностики.")
public class NoOpTool implements Tool {

    @Getter @Setter
    private Map<String, Object> args;

    /**
     * Логирует факт вызова заглушки с переданными аргументами.
     */
    @Override
    public void exec() {
        log.warn("Вызван неизвестный инструмент с аргументами: {}", args.toString());
    }
}
