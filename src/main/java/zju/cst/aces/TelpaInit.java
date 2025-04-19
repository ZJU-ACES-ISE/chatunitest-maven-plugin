package zju.cst.aces;

import org.apache.maven.project.MavenProject;
import zju.cst.aces.api.Project;
import zju.cst.aces.api.config.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TelpaInit {

    public void initializeProject(MavenProject project, String smartUnitTest_path, Config config) {
        try {
            // 执行 SmartUnit 初始化逻辑
            // Maven commands are now executed in ProjectTestMojo and its subclasses
            generateSmartUnitTest(project, smartUnitTest_path, config);
        } catch (Exception e) {
            System.err.println("Error initializing project: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // This method is no longer used - Maven commands are now executed in ProjectTestMojo
    @Deprecated
    private void executeMavenCommand(File workingDir, String... args) {
        // This method is kept for backward compatibility but is no longer used
        throw new UnsupportedOperationException("This method is deprecated. Maven commands are now executed in ProjectTestMojo.");
    }

    public void generateSmartUnitTest(MavenProject project, String smartUnitTest_path, Config config) {

        File baseDir = project.getBasedir();

        if (baseDir != null) {
            // 检查是否存在名为 "smartut-tests" 的文件夹
            File smartutTestsFolder = new File(baseDir, "smartut-tests");
            if (smartutTestsFolder.exists() && smartutTestsFolder.isDirectory()) {
                // 文件夹存在，直接返回
                return;
            }
        }
        // 获取 target/classes 目录
        String targetClassesDir = project.getBuild().getOutputDirectory();
        // 获取 target/dependency 目录
        String targetDependencyDir = project.getBuild().getDirectory() + File.separator + "dependency";
        // 构建依赖路径字符串，列出所有JAR文件
        String dependencyClasspath = buildDependencyClasspath(targetDependencyDir);
        System.out.println("targetClassesDir: " + targetClassesDir + ", dependencyClasspath: " + dependencyClasspath);
        // 创建 setup 命令和参数
        List<String> setupCommand = new ArrayList<>();
        setupCommand.add("java");
        setupCommand.add("-jar");
        setupCommand.add(smartUnitTest_path);
        setupCommand.add("-setup");
        setupCommand.add(targetClassesDir);
        setupCommand.add(dependencyClasspath);

        // 创建 target 命令和参数
        List<String> targetCommand = new ArrayList<>();
        targetCommand.add("java");
        targetCommand.add("-jar");
        targetCommand.add(smartUnitTest_path);
        targetCommand.add("-target");
        targetCommand.add(targetClassesDir);

        // 运行 setup 命令
        runCommand(setupCommand);

        // 运行 target 命令
        runCommand(targetCommand);

        // 清理 SmartUnit 继承的 scaffolding 相关内容
        cleanSmartUnitTests(config.getCounterExamplePath());
    }

    private void runCommand(List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // 合并标准错误流和标准输出流

        try {
            // 启动进程
            Process process = processBuilder.start();

            // 获取进程的输入流
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // 等待进程结束并获取退出值
            int exitCode = process.waitFor();
            process.destroy();
            System.out.println("Exit Code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void cleanSmartUnitTests(Path testDirectory) {
        if (!Files.exists(testDirectory) || !Files.isDirectory(testDirectory)) {
            System.out.println("The specified directory does not exist or is not a directory.");
            return;
        }

        try {
            // 遍历目录中的所有文件
            Files.walk(testDirectory)
                    .filter(Files::isRegularFile) // 只处理文件
                    .forEach(file -> processFile(file.toFile())); // 处理每个文件
        } catch (IOException e) {
            System.err.println("Error traversing the directory: " + testDirectory);
            e.printStackTrace();
        }
    }

    /**
     * Builds a classpath string from all JAR files in the dependency directory
     * @param dependencyDirPath Path to the dependency directory
     * @return A classpath string with all JAR files
     */
    private String buildDependencyClasspath(String dependencyDirPath) {
        File dependencyDir = new File(dependencyDirPath);
        if (!dependencyDir.exists() || !dependencyDir.isDirectory()) {
            System.err.println("Dependency directory does not exist: " + dependencyDirPath);
            return dependencyDirPath; // Return the original path as fallback
        }

        try {
            // Get all JAR files in the directory
            List<Path> jarFiles = Files.list(Paths.get(dependencyDirPath))
                    .filter(path -> path.toString().toLowerCase().endsWith(".jar"))
                    .collect(Collectors.toList());

            if (jarFiles.isEmpty()) {
                System.out.println("No JAR files found in dependency directory: " + dependencyDirPath);
                return dependencyDirPath; // Return the directory path as fallback
            }

            // Build classpath string with all JAR files
            StringBuilder classpath = new StringBuilder();
            for (int i = 0; i < jarFiles.size(); i++) {
                classpath.append(jarFiles.get(i).toString());
                if (i < jarFiles.size() - 1) {
                    classpath.append(File.pathSeparator); // Use path separator (: on Unix, ; on Windows)
                }
            }

            return classpath.toString();
        } catch (IOException e) {
            System.err.println("Error listing JAR files in dependency directory: " + e.getMessage());
            e.printStackTrace();
            return dependencyDirPath; // Return the original path as fallback
        }
    }

    private void processFile(File file) {
        try {
            // 删除 `_scaffolding.java` 结尾的文件
            if (file.getName().endsWith("_scaffolding.java")) {
                if (file.delete()) {
                    System.out.println("Deleted scaffolding file: " + file.getName());
                } else {
                    System.out.println("Failed to delete scaffolding file: " + file.getName());
                }
                return; // 删除后直接返回
            }

            // 对 `Test.java` 文件进行清理
            if (file.getName().endsWith("Test.java")) {
                List<String> lines = Files.readAllLines(file.toPath());
                List<String> updatedLines = new ArrayList<>();

                boolean skipAnnotations = true; // 跳过顶部注解的标志
                for (String line : lines) {
                    // 跳过顶部注解行（以 `@` 开头的行）
                    if (line.contains("org.smartut")) {
                        continue; // Skip SmartUnit-related imports and annotations
                    }
                    if (line.trim().startsWith("@RunWith(SmartUtRunner.class)")) {
                        continue;
                    }
                    // 去掉 `extends` 继承 `_scaffolding` 的部分
                    if (line.contains("extends") && line.contains("_scaffolding")) {
                        line = line.replaceAll("extends\\s+\\w+_scaffolding", "").trim();
                    }

                    updatedLines.add(line);
                }

                // 将清理后的内容写回文件
                Files.write(file.toPath(), updatedLines);
                System.out.println("Cleaned file: " + file.getName());
            }
        } catch (IOException e) {
            System.err.println("Error processing file: " + file.getName());
            e.printStackTrace();
        }
    }
}
