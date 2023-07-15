package zju.cst.aces.runner;

import zju.cst.aces.dto.ClassInfo;
import zju.cst.aces.dto.MethodInfo;
import zju.cst.aces.util.Config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ClassRunner extends AbstractRunner {
    public ClassInfo classInfo;
    public File infoDir;

    public ClassRunner(String fullClassName, String parsePath, String testPath) throws IOException {
        super(fullClassName, parsePath, testPath);
        infoDir = new File(parseOutputPath + File.separator + fullClassName.replace(".", File.separator));
        if (!infoDir.isDirectory()) {
            log.error("Error: " + fullClassName + " no parsed info found");
        }
        File classInfoFile = new File(infoDir + File.separator + "class.json");
        classInfo = GSON.fromJson(Files.readString(classInfoFile.toPath(), StandardCharsets.UTF_8), ClassInfo.class);
    }

    public void start() throws IOException {
        if (Config.enableMultithreading == true) {
            methodJob();
        } else {
            for (String mSig : classInfo.methodSignatures.keySet()) {
                MethodInfo methodInfo = getMethodInfo(classInfo, mSig);
                if (methodInfo == null) {
                    continue;
                }
                new MethodRunner(fullClassName, parseOutputPath.toString(), testOutputPath.toString(), methodInfo).start();
            }
        }
    }

    public void methodJob() {
        ExecutorService executor = Executors.newFixedThreadPool(methodThreads);
        List<Future<String>> futures = new ArrayList<>();
        for (String mSig : classInfo.methodSignatures.keySet()) {
            Callable<String> callable = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    MethodInfo methodInfo = getMethodInfo(classInfo, mSig);
                    if (methodInfo == null) {
                        return "No parsed info found for " + mSig + " in " + fullClassName;
                    }
                    new MethodRunner(fullClassName, parseOutputPath.toString(), testOutputPath.toString(), methodInfo).start();
                    return "Processed " + mSig;
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
    }
}
