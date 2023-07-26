package zju.cst.aces.util;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.util.FileUtils;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import zju.cst.aces.config.Config;
import zju.cst.aces.dto.PromptInfo;
import zju.cst.aces.dto.TestMessage;
import zju.cst.aces.parser.ProjectParser;

import javax.tools.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class TestCompiler {
    public static File srcTestFolder = new File("src" + File.separator + "test" + File.separator + "java");
    public static File backupFolder = new File("src" + File.separator + "backup");
    public static Config config;
    public String testName;
    public String fullTestName;
    public String code;

    public TestCompiler(Config config) {
        this.config = config;
        this.code = "";
    }
    public TestCompiler(Config config, String code) {
        this.config = config;
        this.code = code;
    }

    public TestExecutionSummary executeTest(String fullTestName, Path outputPath) {
        File file = outputPath.toAbsolutePath().getParent().toFile();
        this.fullTestName = fullTestName;
        try {
            List<String> classpathElements = new ArrayList<>();
            classpathElements.addAll(config.getClassPaths());
            List<URL> urls = new ArrayList<>();
            for (String classpath : classpathElements) {
                URL url = new File(classpath).toURI().toURL();
                urls.add(url);
            }
            urls.add(file.toURI().toURL());
            ClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());

            // Use the ServiceLoader API to load TestEngine implementations
            ServiceLoader<TestEngine> testEngineServiceLoader = ServiceLoader.load(TestEngine.class, classLoader);

            // Create a LauncherConfig with the TestEngines from the ServiceLoader
            LauncherConfig launcherConfig = LauncherConfig.builder()
                    .enableTestEngineAutoRegistration(false)
                    .enableTestExecutionListenerAutoRegistration(false)
                    .addTestEngines(testEngineServiceLoader.findFirst().orElseThrow())
                    .build();

            Launcher launcher = LauncherFactory.create(launcherConfig);

            // Register a listener to collect test execution results.
            SummaryGeneratingListener listener = new SummaryGeneratingListener();
            launcher.registerTestExecutionListeners(listener);

            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectClass(classLoader.loadClass(fullTestName)))
                    .build();
            launcher.execute(request);

            TestExecutionSummary summary = listener.getSummary();
            return summary;
        } catch (Exception e) {
            throw new RuntimeException("In TestCompiler.executeTest: " + e);
        }
    }

    /**
     * Compile test file
     */
    public boolean compileTest(String className, Path outputPath, PromptInfo promptInfo) {
        if (this.code == "") {
            throw new RuntimeException("In TestCompiler.compileTest: code is empty");
        }
        this.testName = className;
        boolean result;
        try {
            if (!outputPath.toAbsolutePath().getParent().toFile().exists()) {
                outputPath.toAbsolutePath().getParent().toFile().mkdirs();
            }
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

            SimpleJavaFileObject sourceJavaFileObject = new SimpleJavaFileObject(URI.create(className + ".java"),
                    JavaFileObject.Kind.SOURCE){
                public CharBuffer getCharContent(boolean b) {
                    return CharBuffer.wrap(code);
                }
            };

            Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(sourceJavaFileObject);
            Iterable<String> options = Arrays.asList("-classpath", String.join(config.getOS().contains("win") ? ";" : ":", config.getClassPaths()),
                    "-d", outputPath.toAbsolutePath().getParent().toString());

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);

            result = task.call();
            if (!result && promptInfo != null) {
                TestMessage testMessage = new TestMessage();
                List<String> errors = new ArrayList<>();
                diagnostics.getDiagnostics().forEach(diagnostic -> {
                    errors.add("Error in " + testName +
                            ": line " + diagnostic.getLineNumber() + " : "
                            + diagnostic.getMessage(null));
                });
                testMessage.setErrorType(TestMessage.ErrorType.COMPILE_ERROR);
                testMessage.setErrorMessage(errors);
                promptInfo.setErrorMsg(testMessage);

                exportError(errors, outputPath);
            }
        } catch (Exception e) {
            throw new RuntimeException("In TestCompiler.compileTest: " + e);
        }
        return result;
    }

    public void exportError(List<String> errors, Path outputPath) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()));
            writer.write(code);
            writer.write("\n--------------------------------------------\n");
            writer.write(errors.stream().collect(Collectors.joining("\n")));
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("In TestCompiler.exportError: " + e);
        }
    }

    public static List<String> listClassPaths(MavenSession session, MavenProject project, DependencyGraphBuilder dependencyGraphBuilder) {
        List<String> classPaths = new ArrayList<>();
        try {
            classPaths.addAll(project.getCompileClasspathElements());
            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest() );
            buildingRequest.setProject(project);
            DependencyNode root = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, null);
            Set<DependencyNode> depSet = new HashSet<>();
            ProjectParser.walkDep(root, depSet);
            for (DependencyNode dep : depSet) {
                if (dep.getArtifact().getFile() != null) {
                    classPaths.add(dep.getArtifact().getFile().getAbsolutePath());
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return classPaths;
    }


    /**
     * Move the src/test/java folder to a backup folder
     */
    public void copyAndBackupTestFolder() {
        restoreTestFolder();
        if (srcTestFolder.exists()) {
            try {
                FileUtils.copyDirectoryStructure(srcTestFolder, backupFolder);
                FileUtils.deleteDirectory(srcTestFolder);
                FileUtils.copyDirectoryStructure(config.getTestOutput().toFile(), srcTestFolder);
            } catch (IOException e) {
                throw new RuntimeException("In TestCompiler.copyAndBackupTestFolder: " + e);
            }
        }
    }

    /**
     * Restore the backup folder to src/test/java
     */
    public void restoreTestFolder() {
        if (backupFolder.exists()) {
            try {
                if (srcTestFolder.exists()) {
                    FileUtils.deleteDirectory(srcTestFolder);
                }
                FileUtils.copyDirectoryStructure(backupFolder, srcTestFolder);
                FileUtils.deleteDirectory(backupFolder);
            } catch (IOException e) {
                throw new RuntimeException("In TestCompiler.restoreTestFolder: " + e);
            }
        }
    }
}
