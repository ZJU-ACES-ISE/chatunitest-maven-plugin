package zju.cst.aces.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProjectParser {

    //    private static final String projectRootPath = System.getProperty("user.dir");
    private String srcFolderPath;
    private String outputPath;
    private ClassParser classParser = null;

    public ProjectParser(String src, String output, String jarDepPath) {
        setSrcFolderPath(src);
        setOutputPath(output);
        copyJarDeps(jarDepPath);
        classParser = new ClassParser(outputPath, jarDepPath);
    }

    /**
     * Parse the project.
     */
    public void parse() {
        List<String> classPaths = new ArrayList<>();
        scanSourceDirectory(new File(srcFolderPath), classPaths);
        for (String classPath : classPaths) {
            try {
                this.classParser.extractClass(classPath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Run command `mvn dependency:copy-dependencies -DoutputDirectory=jarDepPath` to get jar dependencies.
     * TODO: 使用maven接口获取jar依赖
     */
    private static void copyJarDeps(String jarDepPath) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(Arrays.asList("mvn", "dependency:copy-dependencies", "-DoutputDirectory=" + jarDepPath));
        try {
            Process process = processBuilder.start();
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("get jar dependencies error:" + e);
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

    public String getOutputPath() {
        return this.outputPath;
    }

    public String getSrcFolderPath() {
        return this.srcFolderPath;
    }

}
