package zju.cst.aces.util;

import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
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
import zju.cst.aces.ProjectTestMojo;
import zju.cst.aces.parser.ClassParser;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class TestCompiler extends ProjectTestMojo {
    public static File srcTestFolder = new File("src" + File.separator + "test" + File.separator + "java");
    public static File backupFolder = new File("src" + File.separator + "backup");

    public boolean executeTest(String fullTestName, Path outputPath, PromptInfo promptInfo) {
        File file = outputPath.toAbsolutePath().getParent().toFile();
        try {
            List<String> classpathElements = new ArrayList<>();
            classpathElements.addAll(Config.classPaths);
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
            if (summary.getTestsFailedCount() > 0) {
                TestMessage testMessage = new TestMessage();
                List<String> errors = new ArrayList<>();
                summary.getFailures().forEach(failure -> {
                    for (StackTraceElement st : failure.getException().getStackTrace()) {
                        if (st.getClassName().equals(fullTestName)) {
                            errors.add(failure.getTestIdentifier().getDisplayName() + ": "
                                    + " line: "  + st.getLineNumber() + " "
                                    + failure.getException().toString());
                        }
                    }
                });
                testMessage.setErrorType(TestMessage.ErrorType.RUNTIME_ERROR);
                testMessage.setErrorMessage(errors);
                promptInfo.setErrorMsg(testMessage);

                BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()));
                writer.write(errors.toString()); // store the full output
                writer.close();
            }
            summary.printTo(new PrintWriter(System.out));
            return summary.getTestsFailedCount() == 0;
        } catch (Exception e) {
            throw new RuntimeException("In TestCompiler.executeTest: " + e);
        }
    }

    /**
     * Compile test file
     */
    public boolean compileTest(String className, String code, Path outputPath, PromptInfo promptInfo) {
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
            Iterable<String> options = Arrays.asList("-classpath", String.join(Config.OS.contains("win") ? ";" : ":", Config.classPaths),
                    "-d", outputPath.toAbsolutePath().getParent().toString());

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);

            result = task.call();
            if (!result) {
                TestMessage testMessage = new TestMessage();
                List<String> errors = new ArrayList<>();
                diagnostics.getDiagnostics().forEach(diagnostic -> {
                    errors.add("Error on line " + diagnostic.getLineNumber() +
                            " : " + diagnostic.getMessage(null));
                });
                testMessage.setErrorType(TestMessage.ErrorType.COMPILE_ERROR);
                testMessage.setErrorMessage(errors);
                promptInfo.setErrorMsg(testMessage);

                BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()));
                writer.write(code);
                writer.write(errors.toString()); // store the full output
                writer.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("In TestCompiler.compileTest: " + e);
        }
        return result;
    }

    public static List<String> listClassPaths() {
        List<String> classPaths = new ArrayList<>();
        try {
            classPaths.addAll(Config.project.getCompileClasspathElements());
            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(Config.session.getProjectBuildingRequest() );
            buildingRequest.setProject(Config.project);
            DependencyNode root = Config.dependencyGraphBuilder.buildDependencyGraph(buildingRequest, null);
            Set<DependencyNode> depSet = new HashSet<>();
            ClassParser.walkDep(root, depSet);
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
     * Copy test file to src/test/java folder with the same directory structure
     */
    public File copyFileToTest(File file) {
        Path sourceFile = file.toPath();
        String splitString = Config.OS.contains("win") ? Config.testOutput + "\\\\" : Config.testOutput + "/";
        String pathWithParent = sourceFile.toAbsolutePath().toString().split(splitString)[1];
        Path targetPath = srcTestFolder.toPath().resolve(pathWithParent);
        log.debug("In TestCompiler.copyFileToTest: file " + file.getName() + " target path" + targetPath);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(sourceFile, targetPath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            log.error("In TestCompiler.copyFileToTest: " + e);
        }
        return targetPath.toFile();
    }

    /**
     * Move the src/test/java folder to a backup folder
     */
    public static void copyAndBackupTestFolder() {
        restoreTestFolder();
        if (srcTestFolder.exists()) {
            try {
                FileUtils.copyDirectoryStructure(srcTestFolder, backupFolder);
                FileUtils.deleteDirectory(srcTestFolder);
                FileUtils.copyDirectoryStructure(new File(Config.testOutput), srcTestFolder);
            } catch (IOException e) {
                throw new RuntimeException("In TestCompiler.copyAndBackupTestFolder: " + e);
            }
        }
    }

    /**
     * Restore the backup folder to src/test/java
     */
    public static void restoreTestFolder() {
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
