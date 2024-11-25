package zju.cst.aces;

import org.apache.maven.project.MavenProject;
import zju.cst.aces.api.Project;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TelpaInit {
    public void generateSmartUnitTest(MavenProject project,String smartUnitTest_path) {
        // 获取项目根目录
        File baseDir = project.getBasedir();

        if (baseDir != null) {
            // 检查是否存在名为 "smartut-tests" 的文件夹
            File smartutTestsFolder = new File(baseDir, "smartut-tests");
            if (smartutTestsFolder.exists() && smartutTestsFolder.isDirectory()) {
                // 文件夹存在，直接返回mvn 
                return;
            }
        }
        // 获取 target/classes 目录
        String targetClassesDir = project.getBuild().getOutputDirectory();
        // 获取 target/dependency/*.jar 目录
        String targetDependencyDir = project.getBuild().getDirectory() + "\\dependency\\*.jar";

        // 创建 setup 命令和参数
        List<String> setupCommand = new ArrayList<>();
        setupCommand.add("java");
        setupCommand.add("-jar");
        setupCommand.add(smartUnitTest_path);
        setupCommand.add("-setup");
        setupCommand.add(targetClassesDir);
        setupCommand.add(targetDependencyDir);

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
}
