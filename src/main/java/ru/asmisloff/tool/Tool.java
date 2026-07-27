package ru.asmisloff.tool;

import org.jetbrains.annotations.Nullable;

/**
 * Контракт инструмента, вызываемого LLM-агентом.
 * Реализации выполняют конкретное действие при вызове {@link #exec()}.
 */
public interface Tool {

    /**
     * Выполняет действие инструмента с переданными параметрами.
     */
    @Nullable
    String exec();
}
