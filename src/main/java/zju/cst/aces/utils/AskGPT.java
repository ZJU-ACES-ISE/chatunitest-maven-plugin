package zju.cst.aces.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import zju.cst.aces.ProjectTestMojo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class AskGPT extends ProjectTestMojo {

    private static  Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Config.hostName, Integer.parseInt(Config.port)));
    private static final String URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json");
    private static  OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .proxy(proxy)//自定义代理
            .build();

    /*
    public static OkHttpClient setProxyForClient() {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Config.hostName, Integer.parseInt(Config.port)));
        return CLIENT.newBuilder().proxy(proxy).build();
    }
     */

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static Response askChatGPT(List<Message> messages) {
        //setProxyForClient();
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

                Response response = CLIENT.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("In AskGPT.askChatGPT: Unexpected code " + response);
                return response;

            } catch (IOException e) {
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
        return null;
    }
}