package zju.cst.aces.utils;

import org.codehaus.plexus.util.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestCompiler {
    public static File srcTestFolder = new File("src" + File.separator + "test" + File.separator + "java");
    public static File backupFolder = new File("src" + File.separator + "backup");

    public static boolean compileAndExport(File testFile, Path outputPath, PromptInfo promptInfo) {
        if (!outputPath.toAbsolutePath().getParent().toFile().exists()) {
            outputPath.toAbsolutePath().getParent().toFile().mkdirs();
        }
        String testFileName = testFile.getName().split("\\.")[0];
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(Arrays.asList("mvn", "test", "-Dtest=" + getPackage(testFile) + testFileName));

        List<String> errorMsg = new ArrayList<>();
        StringBuilder output = new StringBuilder();

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                // TODO: handle other conditions e.g. Assertion error
//                if (line.contains("COMPILATION ERROR :")) {

                output.append(line).append("\n");
                if (line.contains("BUILD SUCCESS")){
                    return true;
                }
                if (line.contains("T E S T S") || line.contains("BUILD FAILURE")){
                    errorMsg.add(line);
                    break;
                }
            }
            while ((line = reader.readLine()) != null) {
                if (line.contains("BUILD FAILURE") || line.contains("[Help")){
                    break;
                }
                errorMsg.add(line);
                output.append(line).append("\n");
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()));
            writer.write(output.toString()); // store the original output
            writer.close();

            ErrorProcesser errorProcesser = new ErrorProcesser();
            //TODO: Cannot parse runtime error like Assertion failure.
            String processedOutput = errorProcesser.processErrorMessage(errorMsg, Config.minErrorTokens);

            System.out.println(processedOutput);

            promptInfo.setErrorMsg(processedOutput);

        } catch (Exception e) {
            throw(new RuntimeException(e));
        }
        return false;
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
            throw new RuntimeException(e);
        }
        return "";
    }

    /**
     * Copy test file to src/test/java folder with the same directory structure
     */
    public static File copyFileToTest(File file) {
        Path sourceFile = file.toPath();
        //TODO: change the split string
        String pathWithParent = sourceFile.toAbsolutePath().toString().split("chatunitest-tests" + File.separator)[1];
        Path targetPath = srcTestFolder.toPath().resolve(pathWithParent);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(sourceFile, targetPath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            throw new RuntimeException(e);
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
                throw new RuntimeException(e);
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
                throw new RuntimeException(e);
            }
        }
    }
}
