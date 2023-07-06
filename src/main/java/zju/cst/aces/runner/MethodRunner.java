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

    public MethodRunner(String fullClassName, String parsePath, String testOutputPath, MethodInfo methodInfo) throws IOException {
        super(fullClassName, parsePath, testOutputPath);
        this.methodInfo = methodInfo;
        testName = className + separator + methodInfo.methodName + separator + "Test";
    }

    @Override
    public void start() throws IOException {
        log.info("\n==========================\n[ChatTester] Generating test for method < " + methodInfo.methodName + " > ...\n");
        PromptInfo promptInfo = null;
        for (int rounds = 1; rounds <= Config.maxRounds; rounds++) {
            if (promptInfo == null) {
                log.info("Generating test for method < " + methodInfo.methodName + " > round " + rounds + " ...");
                if (methodInfo.dependentMethods.size() > 0) {
                    promptInfo = generatePromptInfoWithDep(classInfo, methodInfo);
                } else {
                    promptInfo = generatePromptInfoWithoutDep(classInfo, methodInfo);
                }
            } else {
                log.info("Fixing test for method < " + methodInfo.methodName + " > round " + rounds + " ...");
            }
            List<Message> prompt = generateMessages(promptInfo);
            log.debug("[Prompt]:\n" + prompt.toString());

            AskGPT askGPT = new AskGPT();
            Response response = askGPT.askChatGPT(prompt);
            Path savePath = testOutputPath.resolve(classInfo.packageDeclaration
                    .replace(".", File.separator)
                    .replace("package ", "")
                    .replace(";", ""))
                    .resolve(testName + ".java");

            String code = parseResponse(response);
            if (code.isEmpty()) {
                log.info("Test for method < " + methodInfo.methodName + " > extract code failed");
                continue;
            }
            code = changeTestName(code, className, testName);
            code = repairPackage(code, classInfo.packageDeclaration);
            code = addTimeout(code, testTimeOut);

            promptInfo.setUnitTest(code);

            code = repairImports(code, classInfo.imports);
            exportTest(code, savePath);

            TestCompiler compiler = new TestCompiler();
            File testFile = compiler.copyFileToTest(savePath.toFile());

            if (compiler.compileAndExport(testFile,
                    errorOutputPath.resolve(testName + "CompilationError_" + rounds + ".txt"), promptInfo)) {
                log.info("Test for method < " + methodInfo.methodName + " > generated successfully");
                break;
            } else {
                removeTestFile(testFile);
                removeTestFile(savePath.toFile());
                log.info("Test for method < " + methodInfo.methodName + " > generated failed");
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