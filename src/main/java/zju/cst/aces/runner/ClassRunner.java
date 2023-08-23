package zju.cst.aces.runner;

import zju.cst.aces.dto.ClassInfo;
import zju.cst.aces.dto.MethodInfo;
import zju.cst.aces.config.Config;

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
    public int index;

    public ClassRunner(String fullClassName, Config config) throws IOException {
        super(fullClassName, config);
        infoDir = new File(parseOutputPath + File.separator + fullClassName.replace(".", File.separator));
        if (!infoDir.isDirectory()) {
            config.getLog().error("Error: " + fullClassName + " no parsed info found");
        }
        File classInfoFile = new File(infoDir + File.separator + "class.json");
        classInfo = GSON.fromJson(Files.readString(classInfoFile.toPath(), StandardCharsets.UTF_8), ClassInfo.class);
    }

    @Override
    public void start() throws IOException {
        if (config.isEnableMultithreading() == true) {
            methodJob();
        } else {
            for (String mSig : classInfo.methodSigs.keySet()) {
                MethodInfo methodInfo = getMethodInfo(config, classInfo, mSig);
                if (!filter(methodInfo)) {
                    config.getLog().info("Skip method: " + mSig + " in class: " + fullClassName);
                    continue;
                }
                new MethodRunner(fullClassName, config, methodInfo).start();
            }
        }
    }

    public void methodJob() {
        ExecutorService executor = Executors.newFixedThreadPool(config.getMethodThreads());
        List<Future<String>> futures = new ArrayList<>();
        for (String mSig : classInfo.methodSigs.keySet()) {
            Callable<String> callable = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    MethodInfo methodInfo = getMethodInfo(config, classInfo, mSig);
                    if (methodInfo == null) {
                        return "No parsed info found for " + mSig + " in " + fullClassName;
                    }
                    if (!filter(methodInfo)) {
                        return "Skip method: " + mSig + " in class: " + fullClassName;
                    }
                    new MethodRunner(fullClassName, config, methodInfo).start();
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

    private boolean filter(MethodInfo methodInfo) {
        if (methodInfo == null || methodInfo.isConstructor || methodInfo.isGetSet || !methodInfo.isPublic) {
            return false;
        }
        return true;
    }
}
