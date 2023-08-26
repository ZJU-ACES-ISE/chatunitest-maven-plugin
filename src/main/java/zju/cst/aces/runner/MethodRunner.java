package zju.cst.aces.runner;

import okhttp3.Response;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import zju.cst.aces.config.Config;
import zju.cst.aces.dto.*;
import zju.cst.aces.util.AskGPT;
import zju.cst.aces.util.TestCompiler;
import zju.cst.aces.util.TestProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MethodRunner extends ClassRunner {

    public MethodInfo methodInfo;

    public MethodRunner(String fullClassName, Config config, MethodInfo methodInfo) throws IOException {
        super(fullClassName, config);
        this.methodInfo = methodInfo;
    }

    @Override
    public void start() throws IOException {
        if (!config.isStopWhenSuccess() && config.isEnableMultithreading()) {
            ExecutorService executor = Executors.newFixedThreadPool(config.getTestNumber());
            List<Future<String>> futures = new ArrayList<>();
            for (int num = 0; num < config.getTestNumber(); num++) {
                int finalNum = num;
                Callable<String> callable = new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        startRounds(finalNum);
                        return "";
                    }
                };
                Future<String> future = executor.submit(callable);
                futures.add(future);
            }
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    executor.shutdownNow();
                }
            });

            for (Future<String> future : futures) {
                try {
                    String result = future.get();
                    System.out.println(result);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            executor.shutdown();
        } else {
            for (int num = 0; num < config.getTestNumber(); num++) {
                if (startRounds(num)) {
                    break;
                }
            }
        }
        generateTestSuite();
    }

    public boolean startRounds(final int num) throws IOException {
        PromptInfo promptInfo = null;
        String testName = className + separator + methodInfo.methodName + separator
                + classInfo.methodSigs.get(methodInfo.methodSignature) + separator + num + separator + "Test";
        String fullTestName = fullClassName + separator + methodInfo.methodName + separator
                + classInfo.methodSigs.get(methodInfo.methodSignature) + separator + num + separator + "Test";
        config.getLog().info("\n==========================\n[ChatTester] Generating test for method < "
                + methodInfo.methodName + " > number " + num + "...\n");

        for (int rounds = 0; rounds < config.getMaxRounds(); rounds++) {
            if (promptInfo == null) {
                config.getLog().info("Generating test for method < " + methodInfo.methodName + " > round " + rounds + " ...");
                if (methodInfo.dependentMethods.size() > 0) {
                    promptInfo = generatePromptInfoWithDep(classInfo, methodInfo);
                } else {
                    promptInfo = generatePromptInfoWithoutDep(classInfo, methodInfo);
                }
            } else {
                config.getLog().info("Fixing test for method < " + methodInfo.methodName + " > round " + rounds + " ...");
            }
            promptInfo.setFullTestName(fullTestName);
            promptInfo.addRecord(new RoundRecord(rounds));
            RoundRecord record = promptInfo.getRecords().get(rounds);

//            TestSkeleton skeleton = new TestSkeleton(promptInfo); // test skeleton to wrap a test method

            List<Message> prompt = generateMessages(promptInfo);
            if (isExceedMaxTokens(prompt)) {
                config.getLog().error("Exceed max prompt tokens: " + methodInfo.methodName + " Skipped.");
                break;
            }
            config.getLog().debug("[Prompt]:\n" + prompt.toString());

            AskGPT askGPT = new AskGPT(config);
            Response response = askGPT.askChatGPT(prompt);
            Path savePath = testOutputPath.resolve(fullTestName.replace(".", File.separator) + ".java");
            promptInfo.setTestPath(savePath);

            String content = parseResponse(response);
            String code = extractCode(content);
            code = wrapTestMethod(code);

//            code = skeleton.build(code);

            record.setPrompt(prompt);
            record.setResponse(content);
            if (code.isEmpty()) {
                config.getLog().info("Test for method < " + methodInfo.methodName + " > extract code failed");
                record.setHasCode(false);
                continue;
            }
            record.setHasCode(true);

            code = changeTestName(code, testName);
            code = repairPackage(code, classInfo.packageName);
//            code = addTimeout(code, testTimeOut);
            code = repairImports(code, classInfo.imports, true);
            promptInfo.setUnitTest(code); // Before repair imports

            record.setCode(code);
            if (runTest(testName, fullTestName, savePath, promptInfo, rounds)) {
                record.setHasError(false);
                exportRecord(promptInfo, classInfo, num);
                return true;
            }
            record.setHasError(true);
            record.setErrorMsg(promptInfo.getErrorMsg());
        }

        exportRecord(promptInfo, classInfo, num);
        return false;
    }

    public boolean runTest(String testName, String fullTestName, Path savePath, PromptInfo promptInfo, int rounds) {
        TestProcessor testProcessor = new TestProcessor(fullTestName);
        String code = promptInfo.getUnitTest();
        if (rounds >= 1) {
            code = testProcessor.addCorrectTest(promptInfo);
        }

        // Compilation
        TestCompiler compiler = new TestCompiler(config, code);
        Path compilationErrorPath = errorOutputPath.resolve(testName + "_CompilationError_" + rounds + ".txt");
        Path executionErrorPath = errorOutputPath.resolve(testName + "_ExecutionError_" + rounds + ".txt");
        boolean compileResult = compiler.compileTest(testName, compilationErrorPath, promptInfo);
        if (!compileResult) {
            config.getLog().info("Test for method < " + methodInfo.methodName + " > compilation failed round " + rounds);
            return false;
        }
        if (config.isNoExecution()) {
            exportTest(code, savePath);
            config.getLog().info("Test for method < " + methodInfo.methodName + " > generated successfully round " + rounds);
            return true;
        }

        // Execution
        TestExecutionSummary summary = compiler.executeTest(fullTestName, executionErrorPath);
        if (summary.getTestsFailedCount() > 0) {
            String testProcessed = testProcessor.removeErrorTest(promptInfo, summary);

            // Remove errors successfully, recompile and re-execute test
            if (testProcessed != null) {
                config.getLog().debug("[Original Test]:\n" + code);
                TestCompiler newCompiler = new TestCompiler(config, testProcessed);
                if (newCompiler.compileTest(testName, compilationErrorPath, null)) {
                    TestExecutionSummary newSummary = newCompiler.executeTest(fullTestName, executionErrorPath);
                    if (newSummary.getTestsFailedCount() == 0) {
                        exportTest(testProcessed, savePath);
                        config.getLog().debug("[Processed Test]:\n" + testProcessed);
                        config.getLog().info("Processed test for method < " + methodInfo.methodName + " > generated successfully round " + rounds);
                        return true;
                    }
                }
                testProcessor.removeCorrectTest(promptInfo, summary);
            }

            // Set promptInfo error message
            // TODO: should be a function invoked by each return statement
            TestMessage testMessage = new TestMessage();
            List<String> errors = new ArrayList<>();
            summary.getFailures().forEach(failure -> {
                for (StackTraceElement st : failure.getException().getStackTrace()) {
                    if (st.getClassName().contains(fullTestName)) {
                        errors.add("Error in " + failure.getTestIdentifier().getLegacyReportingName()
                                + ": line " + st.getLineNumber() + " : "
                                + failure.getException().toString());
                    }
                }
            });
            testMessage.setErrorType(TestMessage.ErrorType.RUNTIME_ERROR);
            testMessage.setErrorMessage(errors);
            promptInfo.setErrorMsg(testMessage);
            compiler.exportError(errors, executionErrorPath);
            testProcessor.removeCorrectTest(promptInfo, summary);
            config.getLog().info("Test for method < " + methodInfo.methodName + " > execution failed round " + rounds);
            return false;
        }
//            summary.printTo(new PrintWriter(System.out));
        exportTest(code, savePath);
        config.getLog().info("Test for method < " + methodInfo.methodName + " > generated successfully round " + rounds);
        return true;
    }
}