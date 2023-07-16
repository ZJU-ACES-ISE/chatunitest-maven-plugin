package zju.cst.aces.runner;

import okhttp3.Response;
import zju.cst.aces.config.Config;
import zju.cst.aces.dto.Message;
import zju.cst.aces.dto.MethodInfo;
import zju.cst.aces.dto.PromptInfo;
import zju.cst.aces.util.*;

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
            for (int num = 1; num <= config.getTestNumber(); num++) {
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
            for (int num = 1; num <= config.getTestNumber(); num++) {
                if (startRounds(num)) {
                    break;
                }
            }
        }
    }

    public boolean startRounds(final int num) throws IOException {
        PromptInfo promptInfo = null;
        String testName = className + separator + methodInfo.methodName + separator
                + classInfo.methodSignatures.get(methodInfo.methodSignature) + separator + num + separator + "Test";
        String fullTestName = fullClassName + separator + methodInfo.methodName + separator
                + classInfo.methodSignatures.get(methodInfo.methodSignature) + separator + num + separator + "Test";
        config.getLog().info("\n==========================\n[ChatTester] Generating test for method < "
                + methodInfo.methodName + " > number " + num + "...\n");
        for (int rounds = 1; rounds <= config.getMaxRounds(); rounds++) {
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
            List<Message> prompt = generateMessages(promptInfo);
            config.getLog().debug("[Prompt]:\n" + prompt.toString());

            AskGPT askGPT = new AskGPT(config);
            Response response = askGPT.askChatGPT(prompt);
            Path savePath = testOutputPath.resolve(fullTestName.replace(".", File.separator) + ".java");

            String code = parseResponse(response);
            if (code.isEmpty()) {
                config.getLog().info("Test for method < " + methodInfo.methodName + " > extract code failed");
                continue;
            }
            code = changeTestName(code, className, testName);
            code = repairPackage(code, classInfo.packageDeclaration);
            code = addTimeout(code, testTimeOut);
            promptInfo.setUnitTest(code); // Before repair imports
            code = repairImports(code, classInfo.imports);

            TestCompiler compiler = new TestCompiler(config);
            boolean compileResult = compiler.compileTest(testName, code,
                    errorOutputPath.resolve(testName + "_CompilationError_" + rounds + ".txt"), promptInfo);
            if (!compileResult) {
                config.getLog().info("Test for method < " + methodInfo.methodName + " > compilation failed");
                continue;
            }
            if (compiler.executeTest(fullTestName, errorOutputPath.resolve(testName + "_ExecutionError_" + rounds + ".txt"), promptInfo)) {
                exportTest(code, savePath);
                config.getLog().info("Test for method < " + methodInfo.methodName + " > generated successfully");
                return true;
            } else {
                config.getLog().info("Test for method < " + methodInfo.methodName + " > execution failed");
            }
        }
        return false;
    }
}