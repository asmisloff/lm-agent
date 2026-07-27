package ru.asmisloff.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import java.util.Collection;
import java.util.Map;

/**
 * Реестр доступных инструментов.
 * Хранит соответствие между именем инструмента и его классом,
 * а также выполняет десериализацию и запуск инструмента по накопленным данным.
 */
@Log4j2
@UtilityClass
public class ToolRegistry {

    private static final ObjectMapper objectMapper = new ObjectMapper(); // todo: снаружи

    /**
     * Сопоставление простого имени класса → класс инструмента
     */
    private static final Map<String, Class<? extends Tool>> TOOLS = Map.of(
            SaveToFileTool.class.getSimpleName(), SaveToFileTool.class,
            AskUserTool.class.getSimpleName(), AskUserTool.class,
            ReadFileTool.class.getSimpleName(), ReadFileTool.class
    );

    /**
     * Возвращает все зарегистрированные классы инструментов.
     *
     * @return коллекция классов, реализующих {@link Tool}
     */
    public static Collection<Class<? extends Tool>> getKnownToolClasses() {
        return TOOLS.values();
    }

    /**
     * Возвращает класс инструмента по его имени.
     *
     * @param name имя инструмента (простое имя класса)
     * @return класс инструмента; {@link NoOpTool}, если инструмент не найден
     */
    public static Class<? extends Tool> getToolClass(String name) {
        var toolClass = TOOLS.get(name);
        if (toolClass == null) {
            log.error("Неизвестный инструмент: {}", name);
            return NoOpTool.class;
        }
        return toolClass;
    }

    /**
     * Десериализует аргументы из аккумулятора и выполняет инструмент.
     *
     * @param acc аккумулятор с именем инструмента и JSON-аргументами
     */
    public static void execTool(ToolCallAccumulator acc) {
        try {
            objectMapper
                    .readValue(acc.getArguments(), getToolClass(acc.getName()))
                    .exec();
        } catch (JsonProcessingException ex) {
            log.error("Некорректный вызов инструмента {}: {}", acc.getName(), acc.getArguments(), ex);
        }
    }

    public static String execTool(ChatCompletionMessageFunctionToolCall.Function func) {
        try {
            return objectMapper
                    .readValue(func.arguments(), getToolClass(func.name()))
                    .exec();
        } catch (JsonProcessingException ex) {
            log.error("Некорректный вызов инструмента {}: {}", func.name(), func.arguments(), ex);
            return ex.getMessage();
        }
    }
}
