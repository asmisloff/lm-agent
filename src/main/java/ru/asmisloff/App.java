package ru.asmisloff;

import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

import java.io.IOException;

public class App {

    public static void main(String[] args) throws IOException {
        try (var promptMd = App.class.getResourceAsStream("prompt.md")) {
            if (promptMd == null) {
                throw new IllegalStateException("Не найден файл с промптом");
            }
            var params = ChatCompletionCreateParams.builder()
                .model("deepseek/deepseek-v3.2")
                .addSystemMessage("""
                                      Ты опытный разработчик на Java.
                                      Ты пишешь надежный, понятный и эффективный код. Комментарии и JavaDoc на русском языке, очень лаконично.
                                      Ты выводишь только код, комментарии и JavaDoc в markdown. Без дополнительных пояснений."""
                )
                .addUserMessage(new String(promptMd.readAllBytes()))
                .build();
            var client = new OpenAIOkHttpClient.Builder()
                .baseUrl("https://routerai.ru/api/v1")
                .apiKey("sk-GpACFUI0S8-dKJOJ1x8XunGq0kG6kdek")
                .build();
            System.out.println("==============");
            try (var completion = client.chat().completions().createStreaming(params)) {
                completion.stream()
                    .map(chunk -> chunk.choices().get(0).delta().content().orElse(""))
                    .forEach(System.out::print);
            }
            System.out.println("==============");
        }
    }
}
