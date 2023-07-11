package zju.cst.aces.parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import zju.cst.aces.util.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectParser {

    private String srcFolderPath;
    private String outputPath;
    public Map<String, List<String>> classMap = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public ProjectParser(String src, String output) {
        setSrcFolderPath(src);
        setOutputPath(output);
    }

    /**
     * Parse the project.
     */
    public void parse() {
        List<String> classPaths = new ArrayList<>();
        scanSourceDirectory(new File(srcFolderPath), classPaths);
        if (classPaths.isEmpty()) {
            throw new RuntimeException("No java file found in " + srcFolderPath);
        }
        for (String classPath : classPaths) {
            try {
                addClassMap(classPath);
                String packagePath = classPath.substring(srcFolderPath.length() + 1);
                Path output = Paths.get(outputPath, packagePath).getParent();
                ClassParser classParser = new ClassParser(output);
                classParser.extractClass(classPath);
            } catch (Exception e) {
                throw new RuntimeException("In ProjectParser.parse: " + e);
            }
        }
        exportClassMap();
    }

    public void addClassMap(String classPath) {
        String fullClassName = classPath.substring(srcFolderPath.length() + 1)
                .replace(".java", "")
                .replace(File.separator, ".");

        String className = Paths.get(classPath).getFileName().toString().replace(".java", "");
        if (classMap.containsKey(className)) {
            classMap.get(className).add(fullClassName);
        } else {
            List<String> fullClassNames = new ArrayList<>();
            fullClassNames.add(fullClassName);
            classMap.put(className, fullClassNames);
        }
    }

    public void exportClassMap() {
        Path classMapPath = Config.classMapPath;
        if (!Files.exists(classMapPath.getParent())) {
            try {
                Files.createDirectories(classMapPath.getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(classMapPath.toFile()), StandardCharsets.UTF_8)){
            writer.write(GSON.toJson(classMap));
        } catch (Exception e) {
            throw new RuntimeException("In ProjectParser.exportNameMap: " + e);
        }
    }

    public static void scanSourceDirectory(File directory, List<String> classPaths) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        scanSourceDirectory(file, classPaths);
                    } else if (file.getName().endsWith(".java")) {
                        String classPath = file.getPath();
                        classPaths.add(classPath);
                    }
                }
            }
        }
    }

    public void setSrcFolderPath(String src) {
        this.srcFolderPath = src;
    }

    public void setOutputPath(String output) {
        this.outputPath = output;
    }
}
