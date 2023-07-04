package zju.cst.aces.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectParser {

    private String srcFolderPath;
    private String outputPath;
    private ClassParser classParser = null;

    public ProjectParser(String src, String output) {
        setSrcFolderPath(src);
        setOutputPath(output);
        classParser = new ClassParser(outputPath);
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
                throw new RuntimeException("In ProjectParser.parse: " + e);
            }
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
