package zju.cst.aces.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import zju.cst.aces.config.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ProjectParser {

    public static final JavaParser parser = new JavaParser();
    public Path srcFolderPath;
    public Path outputPath;
    public Map<String, List<String>> classMap = new HashMap<>();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static Config config;

    public ProjectParser(Config config) {
        this.srcFolderPath = Paths.get(config.getProject().getBasedir().getAbsolutePath(), "src", "main", "java");
        this.config = config;
        this.outputPath = config.getParseOutput();
        JavaSymbolSolver symbolSolver = getSymbolSolver();
        parser.getParserConfiguration().setSymbolResolver(symbolSolver);
    }

    /**
     * Parse the project.
     */
    public void parse() {
        List<String> classPaths = new ArrayList<>();
        scanSourceDirectory(srcFolderPath.toFile(), classPaths);
        if (classPaths.isEmpty()) {
            throw new RuntimeException("No java file found in " + srcFolderPath);
        }
        for (String classPath : classPaths) {
            try {
                addClassMap(classPath);
                String packagePath = classPath.substring(srcFolderPath.toString().length() + 1);
                Path output = outputPath.resolve(packagePath).getParent();
                ClassParser classParser = new ClassParser(parser, output);
                classParser.extractClass(classPath);
            } catch (Exception e) {
                throw new RuntimeException("In ProjectParser.parse: " + e);
            }
        }
        exportClassMap();
    }

    public void addClassMap(String classPath) {
        String fullClassName = classPath.substring(srcFolderPath.toString().length() + 1)
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
        Path classMapPath = config.getClassMapPath();
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

    private JavaSymbolSolver getSymbolSolver() {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        try {
            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(config.getSession().getProjectBuildingRequest() );
            buildingRequest.setProject(config.getProject());
            DependencyNode root = config.getDependencyGraphBuilder().buildDependencyGraph(buildingRequest, null);
            Set<DependencyNode> depSet = new HashSet<>();
            walkDep(root, depSet);
            for (DependencyNode dep : depSet) {
                try {
                    if (dep.getArtifact().getFile() != null) {
                        combinedTypeSolver.add(new JarTypeSolver(dep.getArtifact().getFile()));
                    }
                } catch (Exception e) {
                    config.getLog().warn(e.getMessage());
                    config.getLog().debug(e);
                }
            }
        } catch (Exception e) {
            config.getLog().warn(e.getMessage());
            config.getLog().debug(e);
        }
        for (String src : config.getProject().getCompileSourceRoots()) {
            if (new File(src).exists()) {
                combinedTypeSolver.add(new JavaParserTypeSolver(src));
            }
        }
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        return symbolSolver;
    }

    public static void walkDep(DependencyNode node, Set<DependencyNode> depSet) {
        depSet.add(node);
        for (DependencyNode dep : node.getChildren()) {
            walkDep(dep, depSet);
        }
    }
}
