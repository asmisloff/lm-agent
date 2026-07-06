package ru.asmisloff.tool;

import lombok.Getter;
import lombok.Setter;

/**
 * Аккумулятор данных вызова инструмента.
 * Накапливает имя инструмента и его аргументы в процессе потокового парсинга.
 */
public class ToolCallAccumulator {

    private final StringBuilder arguments = new StringBuilder();

    /**
     * Имя вызываемого инструмента
     */
    @Getter
    @Setter
    private String name;

    /**
     * Добавляет фрагмент аргументов к накопленной строке.
     *
     * @param argsFragment фрагмент JSON-аргументов
     */
    public void appendArguments(String argsFragment) {
        this.arguments.append(argsFragment);
    }

    /**
     * Возвращает полную строку аргументов в формате JSON.
     *
     * @return накопленные аргументы
     */
    public String getArguments() {
        return arguments.toString();
    }
}
