package zju.cst.aces.dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import zju.cst.aces.config.Config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ExampleUsage {
    public String className;
    public Map<String, List<String>> methodUsages;
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public ExampleUsage(Config config, String className) {
        this.className = className;
        Path examplePath = config.getExamplePath();
        this.methodUsages  = loadUsages(examplePath, className);
    }

    public Map<String, List<String>> loadUsages(Path path, String name) {
        // read examplePath and load methodUsages
        Map<String, List<String>> usages = null;
        if (!path.toFile().exists()) {
            return null;
        }
        try {
            usages = (Map<String, List<String>>) GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Map.class).get(name);
        } catch (Exception e) {
            throw new RuntimeException("In ExampleUsage.loadUsages: " + e);
        }
        return usages;
    }

    public String getShortestUsage(String methodSig) {
        if (methodUsages == null) {
            return null;
        }
        // sort the list and return the shortest usage of the list
        if (methodUsages.containsKey(methodSig)) {
            List<String> usages = methodUsages.get(methodSig);
            usages.sort((a, b) -> a.length() - b.length());
            return usages.get(0);
        } else {
            return null;
        }
    }
}
