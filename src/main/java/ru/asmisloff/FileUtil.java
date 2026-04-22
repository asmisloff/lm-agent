package ru.asmisloff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public class FileUtil {

    public static Stream<String> prompt() {
        try (var reader = Files.newBufferedReader(Path.of("prompt.md"))) {
            return reader.lines();
        } catch (IOException ex) {
            System.out.println("Не удалось прочитать prompt.md из файловой системы");
        }
        try (var ins = FileUtil.class.getResourceAsStream("prompt.md");
             var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(ins)))
        ) {
            return reader.lines();
        } catch (IOException | NullPointerException ex) {
            System.out.println("Не удалось прочитать промпт из classpath");
            throw new IllegalStateException(ex);
        }
    }
}
