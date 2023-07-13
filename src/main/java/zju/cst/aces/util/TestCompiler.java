package zju.cst.aces.util;

import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.util.FileUtils;
import zju.cst.aces.ProjectTestMojo;
import zju.cst.aces.parser.ClassParser;
import zju.cst.aces.runner.MethodRunner;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class TestCompiler extends ProjectTestMojo {
    public static File srcTestFolder = new File("src" + File.separator + "test" + File.separator + "java");
    public static File backupFolder = new File("src" + File.separator + "backup");

    //TODO: Delete backup folder and restore folder process after remove mvn test command.
    public boolean runTest(File file, Path outputPath, PromptInfo promptInfo) {
        File testFile = null;
        try {
            testFile = copyFileToTest(file);
            log.debug("Running test " + testFile.getName() + "...");
            if (!testFile.exists()) {
                log.error("Test file < " + testFile.getName() + " > not exists");
                return false; // next round
            }
            if (!outputPath.toAbsolutePath().getParent().toFile().exists()) {
                outputPath.toAbsolutePath().getParent().toFile().mkdirs();
            }
            String testFileName = testFile.getName().split("\\.")[0];
            ProcessBuilder processBuilder = new ProcessBuilder();
            String mvn = Config.OS.contains("win") ? "mvn.cmd" : "mvn";
            processBuilder.command(Arrays.asList(mvn, "test", "-Dtest=" + getPackage(testFile) + testFileName));

            log.debug("Running command: `"
                    + mvn + "test -Dtest=" + getPackage(testFile) + testFileName + "`");
            // full output text
            StringBuilder output = new StringBuilder();
            List<String> errorMessage = new ArrayList<>();

            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                log.debug(line);
                output.append(line).append("\n");
                errorMessage.add(line);
                if (line.contains("BUILD SUCCESS")){
                    return true;
                }
                if (line.contains("[Help")){
                    break;
                }
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()));
            writer.write(output.toString()); // store the original output
            writer.close();

            TestMessage testMessage = new TestMessage();
            testMessage.setErrorType(TestMessage.ErrorType.RUNTIME_ERROR);
            testMessage.setErrorMessage(errorMessage);
            promptInfo.setErrorMsg(testMessage);

        } catch (Exception e) {
            throw new RuntimeException("In TestCompiler.compileAndExport: " + e);
        }
        MethodRunner.removeTestFile(testFile);
        return false;
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
                    "-d", outputPath.getParent().toString());

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);

            result = task.call();
            if (!result) {
                TestMessage testMessage = new TestMessage();
                List<String> errors = new ArrayList<>();
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    errors.add("Error on line " + diagnostic.getLineNumber() +
                            " : " + diagnostic.getMessage(null));
                }
                testMessage.setErrorType(TestMessage.ErrorType.COMPILE_ERROR);
                testMessage.setErrorMessage(errors);
                promptInfo.setErrorMsg(testMessage);

                BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()));
                writer.write(errors.toString()); // store the full output
                writer.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("In TestCompiler.compile: " + e);
        }
        return result;
    }

    public static List<String> listClassPaths() {
        List<String> classPaths = new ArrayList<>();
        classPaths.add(Paths.get(Config.project.getBasedir().getAbsolutePath(),"target", "classes").toString());
        try {
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
     * Read the first line of the test file to get the package declaration
     */
    public static String getPackage(File testFile) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(testFile));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("package")) {
                    return line.split("package")[1].split(";")[0].trim() + ".";
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("In TestCompiler.getPackage: " + e);
        }
        return "";
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
    public static void backupTestFolder() {
        restoreTestFolder();
        if (srcTestFolder.exists()) {
            try {
                FileUtils.copyDirectoryStructure(srcTestFolder, backupFolder);
                FileUtils.deleteDirectory(srcTestFolder);
            } catch (IOException e) {
                throw new RuntimeException("In TestCompiler.backupTestFolder: " + e);
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
