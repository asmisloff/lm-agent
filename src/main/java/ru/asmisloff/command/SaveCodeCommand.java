package ru.asmisloff.command;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import ru.asmisloff.FileUtil;
import ru.asmisloff.Props;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Команда сохранить код.
 * Сохраняет код из md-файла с ответом модели в файлы на диске.
 * Первый аргумент — путь к md-файлу (опциональный). Если не указан, используется значение
 * {@link Props#getAnswerFileName()} из настроек.
 * После пути к md-файлу можно указать произвольное количество фильтров:
 * <ul>
 *   <li>Число — сохраняется кодовый блок с таким порядковым номером (нумерация с 1).</li>
 *   <li>Строка — сохраняется блок, ключ (путь) которого содержит эту подстроку без учёта регистра.
 *       Совпадение должно быть однозначным, иначе генерируется исключение.</li>
 * </ul>
 * Если фильтры не заданы, сохраняются все кодовые блоки.
 */
@Log4j2
public class SaveCodeCommand implements Command {

    private final Props props;

    /**
     * Создаёт команду сохранения кода.
     *
     * @param props настройки приложения, используются для получения пути к md-файлу по умолчанию.
     */
    public SaveCodeCommand(@NotNull Props props) {
        this.props = props;
    }

    /**
     * Выполнить команду.
     *
     * @param args первый аргумент — путь к md-файлу (опциональный),
     *             остальные аргументы — опциональные фильтры (числа или строки)
     * @throws IllegalArgumentException если номер блока не положителен,
     *                                  строковый фильтр даёт 0 или более одного совпадения
     * @throws RuntimeException         при ошибках сохранения
     */
    @Override
    public void exec(String... args) {
        String mdFilePath;
        if (args.length >= 1) {
            mdFilePath = args[0];
        } else {
            mdFilePath = props.getAnswerFileName();
            log.info("Путь к md-файлу не указан, используется значение из props: {}", mdFilePath);
        }

        Path mdPath = Path.of(mdFilePath);
        Map<String, String> codeMap = FileUtil.extractCode(mdPath);

        if (args.length <= 1) {
            saveAllCodeBlocks(codeMap);
            return;
        }

        var entries = codeMap.entrySet().stream().toList();
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            var n = tryParseInt(arg);
            if (n != null) {
                saveNthCodeBlock(n, entries);
            } else {
                saveMatchingCodeBlock(entries, arg);
            }
        }
    }

    /**
     * Сохраняет первый кодовый блок, ключ которого содержит заданную подстроку без учёта регистра.
     * Если найдено ноль или больше одного совпадения, генерируется исключение.
     *
     * @param entries список пар (путь к файлу, код).
     * @param arg     подстрока для поиска в ключе.
     * @throws IllegalArgumentException если совпадений 0 или больше 1
     */
    private static void saveMatchingCodeBlock(List<Map.Entry<String, String>> entries, String arg) {
        var matchingEntries = entries.stream()
                .filter(e -> FileUtil.containsIgnoreCase(e.getKey(), arg))
                .toList();
        if (matchingEntries.isEmpty()) {
            throw new IllegalArgumentException("Ключ не найден: " + arg);
        }
        if (matchingEntries.size() > 1) {
            throw new IllegalArgumentException("По ключу %s найдено несколько соответствий: %s".formatted(arg, matchingEntries));
        }
        var e = matchingEntries.get(0);
        FileUtil.saveCode(e.getKey(), e.getValue());
    }

    /**
     * Сохраняет кодовый блок по порядковому номеру (начиная с 1).
     *
     * @param n       номер блока (от единицы).
     * @param entries список пар (путь к файлу, код).
     * @throws IllegalArgumentException если номер меньше 1 или превышает количество блоков
     */
    private static void saveNthCodeBlock(Integer n, List<Map.Entry<String, String>> entries) {
        Map.Entry<String, String> entry;
        int idx = n - 1;
        if (idx < 0 || idx >= entries.size()) {
            throw new IllegalArgumentException("Неправильный номер кодового блока: " + n);
        }
        entry = entries.get(idx);
        FileUtil.saveCode(entry.getKey(), entry.getValue());
    }

    /**
     * Сохранить все кодовые блоки.
     *
     * @param codeMap кодовые блоки.
     */
    private static void saveAllCodeBlocks(Map<String, String> codeMap) {
        codeMap.forEach((filePath, code) -> {
            FileUtil.saveCode(filePath, code);
            log.info("Код сохранён в файл: {}", filePath);
        });
    }

    /**
     * Преобразовать {@code s} в {@link Integer}.
     *
     * @param s строка.
     * @return {@link Integer}, записанный в {@code s} или {@code null}, если преобразование невозможно.
     */
    private static Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}