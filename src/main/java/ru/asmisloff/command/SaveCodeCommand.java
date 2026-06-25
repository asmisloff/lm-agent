package ru.asmisloff.command;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import ru.asmisloff.FileUtil;
import ru.asmisloff.Props;

import java.nio.file.Path;
import java.util.Map;

/**
 * Команда сохранить код.
 * Сохраняет код из md-файла с ответом модели в файлы на диске.
 * Единственный аргумент — путь к md-файлу (опциональный). Если не указан, используется значение
 * {@link Props#getAnswerFileName()} из настроек.
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
     * @param args единственный опциональный аргумент — путь к md-файлу.
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
        var patchMap = FileUtil.extractCode(mdPath);
        applyPatches(patchMap);
    }

    /**
     * Применить патчи
     *
     * @param patchMap патчи.
     */
    private static void applyPatches(Map<String, Patch> patchMap) {
        for (var entry : patchMap.entrySet()) {
            var path = Path.of(entry.getKey());
            var patch = entry.getValue();
            if (patch instanceof FullReplacePatch fullReplacePatch) {
                FileUtil.writeString(path, fullReplacePatch.getReplace());
                log.info("FullReplace сохранён в файл: {}", path.toAbsolutePath());
            } else if (patch instanceof SearchReplacePatch searchReplacePatch) {
                applySearchReplacePatches(path, searchReplacePatch);
                log.info("SearchReplace ({} пар) сохранён в файл: {}", searchReplacePatch.size(), path.toAbsolutePath());
            }
        }
    }

    private static void applySearchReplacePatches(Path path, SearchReplacePatch patch) {
        var content = FileUtil.getFileContent(path);
        for (int i = 0; i < patch.size(); i++) {
            String search = patch.getSearch(i);
            String replace = patch.getReplace(i);
            if (content.contains(search)) {
                content = content.replace(search, replace);
            } else {
                log.warn("В содержимом файла {} не удалось найти фрагмент кода: {}", path.getFileName(), search);
            }
        }
        FileUtil.writeString(path, content);
    }

}
