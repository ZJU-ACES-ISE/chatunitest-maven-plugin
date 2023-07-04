package zju.cst.aces.runner;

import okhttp3.Response;
import zju.cst.aces.utils.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class MethodRunner extends ClassRunner {

    public MethodInfo methodInfo;
    public String testName;
    private static Config config = new Config();

    public MethodRunner(String classname, String parsePath, String testOutputPath, MethodInfo methodInfo) throws IOException {
        super(classname, parsePath, testOutputPath);
        this.methodInfo = methodInfo;
        testName = className + separator + methodInfo.methodName + separator + "Test";
    }

    @Override
    public void start() throws IOException {
        PromptInfo promptInfo = null;
        for (int rounds = 1; rounds <= config.maxRounds; rounds++) {
            if (promptInfo == null) {
                if (methodInfo.dependentMethods.size() > 0) {
                    promptInfo = generatePromptInfoWithDep(classInfo, methodInfo);
                } else {
                    promptInfo = generatePromptInfoWithoutDep(classInfo, methodInfo);
                }
            }
            List<Message> prompt = generateMessages(promptInfo);

            getLog().info("Generating test for method < " + methodInfo.methodName + " > round " + rounds + " ...");
            Response response = AskGPT.askChatGPT(prompt);
            Path savePath = testOutputPath.resolve(classInfo.packageDeclaration
                    .replace(".", File.separator)
                    .replace("package ", "")
                    .replace(";", "")
                    + File.separator + testName + ".java");

            String code = parseResponse(response);
            if (code.isEmpty()) {
                getLog().info("Test for method < " + methodInfo.methodName + " > extract code failed");
                continue;
            }
            code = changeTestName(code, className, testName);
            code = repairPackage(code, classInfo.packageDeclaration);
            code = addTimeout(code, testTimeOut);

            promptInfo.setUnitTest(code);

            code = repairImports(code, classInfo.imports);
            exportTest(code, savePath);

            File testFile = TestCompiler.copyFileToTest(savePath.toFile());

            int promptTokens = prompt.stream().mapToInt(message -> TokenCounter.countToken(message.getContent())).sum();
            if (TestCompiler.compileAndExport(testFile,
                    errorOutputPath.resolve(testName + "CompilationError_" + rounds + ".txt"), promptInfo, promptTokens)) {
                getLog().info("Test for method < " + methodInfo.methodName + " > generated successfully");
                break;
            } else {
                removeTestFile(testFile);
                removeTestFile(savePath.toFile());
                getLog().info("Test for method < " + methodInfo.methodName + " > generated failed");
            }
        }
    }

    /**
     * Remove the failed test file
     */
    private void removeTestFile(File testFile) {
        if (testFile.exists()) {
            testFile.delete();
        }
    }
}