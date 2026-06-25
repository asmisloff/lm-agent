package ru.asmisloff.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Патч поиска и замены фрагментов в файле.
 * Может содержать несколько пар search/replace для одного файла.
 */
public class SearchReplacePatch implements Patch {
    /**
     * Фрагменты для поиска.
     */
    private final List<String> search = new ArrayList<>();
    /**
     * Фрагменты для замены (по индексу соответствуют {@link #search}).
     */
    private final List<String> replace = new ArrayList<>();

    public int size() {
        return search.size();
    }

    /**
     * Добавить пару search/replace.
     */
    public void addEntry(String searchText, String replaceText) {
        search.add(searchText);
        replace.add(replaceText);
    }

    /**
     * Текст замены по индексу.
     */
    public String getReplace(int index) {
        return replace.get(index);
    }

    /**
     * Текст поиска по индексу.
     */
    public String getSearch(int index) {
        return search.get(index);
    }
}
