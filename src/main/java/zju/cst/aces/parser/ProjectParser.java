package zju.cst.aces.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
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
    public Map<String, Set<String>> classMap = new HashMap<>();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static Config config;
    public int classCount = 0;
    public int methodCount = 0;

    public ProjectParser(Config config) {
        this.srcFolderPath = Paths.get(config.getProject().getBasedir().getAbsolutePath(), "src", "main", "java");
        this.config = config;
        this.outputPath = config.getParseOutput();
        JavaSymbolSolver symbolSolver = getSymbolSolver();
        parser.getParserConfiguration().setSymbolResolver(symbolSolver);
        if (config.parser == null) {
            config.setParser(parser);
        }
    }

    /**
     * Parse the project.
     */
    public void parse() {
        List<String> classPaths = new ArrayList<>();
        scanSourceDirectory(config.getProject(), classPaths);
        if (classPaths.isEmpty()) {
            config.getLog().warn("No java file found in " + srcFolderPath);
            return;
        }
        for (String classPath : classPaths) {
            try {
                String packagePath = classPath.substring(srcFolderPath.toString().length() + 1);
                Path output = outputPath.resolve(packagePath).getParent();
                ClassParser classParser = new ClassParser(config, output);
                classParser.extractClass(classPath);

                addClassMap(outputPath, packagePath);
                classCount++;
                methodCount += classParser.methodCount;
            } catch (Exception e) {
                throw new RuntimeException("In ProjectParser.parse: " + e);
            }
        }
        exportClassMapping();
        exportJson(config.getClassMapPath(), classMap);
        config.getLog().info("\nParsed classes: " + classCount + "\nParsed methods: " + methodCount);
    }

    public void addClassMap(Path outputPath, String packagePath) {
        Path path = outputPath.resolve(packagePath).getParent();
        String packageDeclaration = path.toString().substring(outputPath.toString().length() + 1).replace(File.separator, ".");
        // list the directories in the path
        File[] files = path.toFile().listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                String className = file.getName();
                String fullClassName = packageDeclaration + "." + className;
                if (classMap.containsKey(className)) {
                    classMap.get(className).add(fullClassName);
                } else {
                    Set<String> fullClassNames = new HashSet<>();
                    fullClassNames.add(fullClassName);
                    classMap.put(className, fullClassNames);
                }
            }
        }
    }

    public void exportJson(Path path, Object obj) {
        if (!Files.exists(path.getParent())) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(path.toFile()), StandardCharsets.UTF_8)){
            writer.write(GSON.toJson(obj));
        } catch (Exception e) {
            throw new RuntimeException("In ProjectParser.exportJson: " + e);
        }
    }

    public static void scanSourceDirectory(MavenProject project, List<String> classPaths) {
        File[] files = Paths.get(project.getCompileSourceRoots().get(0)).toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    Files.walk(file.toPath()).forEach(path -> {
                        if (path.toString().endsWith(".java")) {
                            classPaths.add(path.toString());
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private JavaSymbolSolver getSymbolSolver() {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        try {
            combinedTypeSolver.add(new JarTypeSolver(config.session.getLocalRepository().find(config.project.getArtifact()).getFile()));
            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(config.getSession().getProjectBuildingRequest() );
            buildingRequest.setProject(config.getProject());
            DependencyNode root = config.getDependencyGraphBuilder().buildDependencyGraph(buildingRequest, null);
            Set<DependencyNode> depSet = new HashSet<>();
            walkDep(root, depSet);

            for (DependencyNode dep : depSet) {
                try {
                    if (dep.getArtifact().getFile() == null || dep.getArtifact().getType().equals("pom")) {
                        continue;
                    }
                    combinedTypeSolver.add(new JarTypeSolver(dep.getArtifact().getFile()));
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
        config.setParserFacade(JavaParserFacade.get(combinedTypeSolver));
        return symbolSolver;
    }

    public static void walkDep(DependencyNode node, Set<DependencyNode> depSet) {
        depSet.add(node);
        for (DependencyNode dep : node.getChildren()) {
            walkDep(dep, depSet);
        }
    }

    public void exportClassMapping() {
        Path savePath = config.tmpOutput.resolve("classMapping.json");
        exportJson(savePath, config.classMapping);
    }
}
