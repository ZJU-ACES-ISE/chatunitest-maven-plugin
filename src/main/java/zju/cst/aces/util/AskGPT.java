package zju.cst.aces.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import zju.cst.aces.config.Config;
import zju.cst.aces.dto.Message;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AskGPT {
    private static String URL;
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public Config config;

    public AskGPT(Config config) {
        this.config = config;
        this.URL = config.getUrl();
    }

    public Response askChatGPT(List<Message> messages) {
        String apiKey = config.getRandomKey();
        int maxTry = 5;
        while (maxTry > 0) {
            try {
                Map<String, Object> payload = new HashMap<>();

                if(Objects.equals(config.getModel(), "code-llama")){
                    payload.put("max_tokens", 8092);
                }

                payload.put("messages", messages);
                payload.put("model", config.getModel());
                payload.put("temperature", config.getTemperature());
                payload.put("top_p", config.getTopP());
                payload.put("frequency_penalty", config.getFrequencyPenalty());
                payload.put("presence_penalty", config.getPresencePenalty());
                String jsonPayload = GSON.toJson(payload);

                RequestBody body = RequestBody.create(MEDIA_TYPE, jsonPayload);
                Request request = new Request.Builder().url(URL).post(body).addHeader("Content-Type", "application/json").addHeader("Authorization", "Bearer " + apiKey).build();

                Response response = config.getClient().newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                try {
                    Thread.sleep(config.sleepTime);
                } catch (InterruptedException ie) {
                    throw new RuntimeException("In AskGPT.askChatGPT: " + ie);
                }
                return response;

            } catch (IOException e) {
                config.getLog().error("In AskGPT.askChatGPT: " + e);
                maxTry--;
            }
        }
        config.getLog().debug("AskGPT: Failed to get response\n");
        return null;
    }
}