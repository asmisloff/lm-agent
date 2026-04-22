package ru.asmisloff;

import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

import java.io.IOException;

public class App {

    public static void main(String[] args) throws IOException {
        var paramsBuilder = ChatCompletionCreateParams.builder()
            .model("deepseek/deepseek-v3.2")
            .addSystemMessage("""
                                  Ты опытный разработчик на Java.
                                  Ты пишешь надежный, понятный и эффективный код. Комментарии и JavaDoc на русском языке, очень лаконично.
                                  Ты выводишь только код, комментарии и JavaDoc в markdown. Без дополнительных пояснений."""
            );
        FileUtil.prompt().forEach(paramsBuilder::addUserMessage);
        var client = new OpenAIOkHttpClient.Builder()
            .baseUrl("https://routerai.ru/api/v1")
            .apiKey("sk-GpACFUI0S8-dKJOJ1x8XunGq0kG6kdek")
            .build();
        System.out.println("==============");
        try (var completion = client.chat().completions().createStreaming(paramsBuilder.build())) {
            completion.stream()
                .map(chunk -> chunk.choices().get(0).delta().content().orElse(""))
                .forEach(System.out::print);
        }
        System.out.println("==============");
    }
}
