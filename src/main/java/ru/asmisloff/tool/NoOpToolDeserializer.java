package ru.asmisloff.tool;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Map;

/**
 * Кастомный десериализатор для {@link NoOpTool}.
 * Сохраняет все поля входящего JSON в {@link NoOpTool#getArgs()}.
 */
public class NoOpToolDeserializer extends StdDeserializer<NoOpTool> {

    public NoOpToolDeserializer() {
        super(NoOpTool.class);
    }

    @Override
    public NoOpTool deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Map<String, Object> args = p.readValueAs(new TypeReference<Map<String, Object>>() {});
        NoOpTool tool = new NoOpTool();
        tool.setArgs(args);
        return tool;
    }
}
