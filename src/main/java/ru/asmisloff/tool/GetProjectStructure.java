package ru.asmisloff.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Nullable;
import ru.asmisloff.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Инструмент для получения структуры файлов проекта.
 * Вызывается без аргументов, возвращает дерево проекта в форме псевдографики в стиле утилиты `tree`.
 */
@Data
@Log4j2
@NoArgsConstructor
@JsonClassDescription("Получить структуру файлов проекта: плоскую или древовидную")
public class GetProjectStructure implements Tool {

    @JsonPropertyDescription("Флаг, в какой форме вернуть ответ. Если true, то в виде дерева, изображенного псевдографикой " +
            "(как в утилите tree). Если false, то плоский список путей ко всем файлам."
    )
    private boolean tree;

    @Nullable
    @Override
    public String exec() {
        Path cwd = FileUtil.cwd();
        if (tree) {
            return FileUtil.buildProjectTree(cwd);
        }
        try (var files = Files.walk(cwd)) {
            return files
                    .map(Path::toString)
                    .collect(Collectors.joining("\n"));
        } catch (IOException ex) {
            log.error("Ошибка при получении структуры проекта: ", ex);
            throw new IllegalStateException(ex);
        }
    }
}
