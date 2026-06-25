package ru.asmisloff.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Патч полной замены содержимого файла.
 */
@Getter
@RequiredArgsConstructor
public class FullReplacePatch implements Patch {
    /**
     * Новое содержимое файла.
     */
    private final String replace;
}
