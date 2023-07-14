package zju.cst.aces.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import zju.cst.aces.ProjectTestMojo;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AskGPT extends ProjectTestMojo {
    private static final String URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public Response askChatGPT(List<Message> messages) {
        String apiKey = Config.getRandomKey();
        int maxTry = 5;
        while (maxTry > 0) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("messages", messages);
                payload.put("model", Config.model);
                payload.put("temperature", Config.temperature);
                payload.put("top_p", Config.topP);
                payload.put("frequency_penalty", Config.frequencyPenalty);
                payload.put("presence_penalty", Config.presencePenalty);
                String jsonPayload = GSON.toJson(payload);

                RequestBody body = RequestBody.create(MEDIA_TYPE, jsonPayload);
                Request request = new Request.Builder()
                        .url(URL)
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .build();

                Response response = Config.client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                return response;

            } catch (IOException e) {
                log.error("In AskGPT.askChatGPT: " + e);
                if (e.getMessage().contains("maximum context length is ")) {
                    break;
                }
                if (e.getMessage().contains("Rate limit reached")) {
                    try {
                        Thread.sleep(new Random().nextInt(60) + 60);
                    } catch (InterruptedException ie) {
                        throw new RuntimeException("In AskGPT.askChatGPT: " + ie);
                    }
                }
                maxTry--;
            }
        }
        log.debug("AskGPT: Failed to get response\n");
        return null;
    }
}