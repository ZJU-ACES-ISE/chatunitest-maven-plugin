package zju.cst.aces;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import zju.cst.aces.config.Config;
import zju.cst.aces.parser.ProjectParser;
import zju.cst.aces.runner.ClassRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author chenyi
 * ChatUniTest maven plugin
 */

@Mojo(name = "project")
public class ProjectTestMojo
        extends AbstractMojo {
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    public MavenSession session;
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    public MavenProject project;
    @Parameter(defaultValue = "chatunitest-tests", property = "testOutput")
    public File testOutput;
    @Parameter(defaultValue = "/tmp/chatunitest-info", property = "tmpOutput")
    public File tmpOutput;
    @Parameter(name = "apiKeys", required = true)
    public String[] apiKeys;
    @Parameter(name = "stopWhenSuccess", property = "stopWhenSuccess", defaultValue = "true")
    public boolean stopWhenSuccess;
    @Parameter(name = "noExecution", property = "noExecution", defaultValue = "false")
    public boolean noExecution;
    @Parameter(alias = "thread", property = "thread", defaultValue = "true")
    public boolean enableMultithreading;
    @Parameter(property = "maxThreads", defaultValue = "0")
    public int maxThreads;
    @Parameter(name = "testNumber", defaultValue = "5")
    public int testNumber;
    @Parameter(name = "maxRounds", defaultValue = "5")
    public int maxRounds;
    @Parameter(name = "minErrorTokens", defaultValue = "500")
    public int minErrorTokens;
    @Parameter(name = "maxPromptTokens", defaultValue = "2600")
    public int maxPromptTokens;
    @Parameter(name = "model", defaultValue = "gpt-3.5-turbo")
    public String model;
    @Parameter(name = "temperature", defaultValue = "0.5")
    public Double temperature;
    @Parameter(name = "topP", defaultValue = "1")
    public  int topP;
    @Parameter(name = "frequencyPenalty", defaultValue = "0")
    public int frequencyPenalty;
    @Parameter(name = "presencePenalty", defaultValue = "0")
    public int presencePenalty;
    @Parameter(name = "proxy",defaultValue = "null:-1")
    public String proxy;
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    @Component(hint = "default")
    public DependencyGraphBuilder dependencyGraphBuilder;
    public static Log log;
    public Config config;


    /**
     * Generate tests for all classes in the project
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException {
        init();
        log.info("\n==========================\n[ChatTester] Generating tests for project " + project.getBasedir().getName() + " ...");
        log.warn("[ChatTester] It may consume a significant number of tokens!");

        Path srcMainJavaPath = Paths.get(project.getBasedir().getAbsolutePath(), "src", "main", "java");
        if (!srcMainJavaPath.toFile().exists()) {
            log.error("\n==========================\n[ChatTester] No compile source found in " + project);
            return;
        }
        ProjectParser parser = new ProjectParser(config);
        if (! config.getParseOutput().toFile().exists()) {
            log.info("\n==========================\n[ChatTester] Parsing class info ...");
            parser.parse();
            log.info("\n==========================\n[ChatTester] Parse finished");
        }

        List<String> classPaths = new ArrayList<>();
        parser.scanSourceDirectory(srcMainJavaPath.toFile(), classPaths);

        if (config.isEnableMultithreading() == true) {
            classJob(classPaths);
        } else {
            for (String classPath : classPaths) {
                String className = classPath.substring(classPath.lastIndexOf(File.separator) + 1, classPath.lastIndexOf("."));
                try {
                    className = getFullClassName(className);
                    log.info("\n==========================\n[ChatTester] Generating tests for class < " + className + " > ...");
                    new ClassRunner(className, config).start();
                } catch (IOException e) {
                    log.error("[ChatTester] Generate tests for class " + className + " failed: " + e);
                }
            }
        }

        log.info("\n==========================\n[ChatTester] Generation finished");
    }

    public void classJob(List<String> classPaths) {
        ExecutorService executor = Executors.newFixedThreadPool(config.getClassThreads());
        List<Future<String>> futures = new ArrayList<>();
        for (String classPath : classPaths) {
            Callable<String> callable = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    String className = classPath.substring(classPath.lastIndexOf(File.separator) + 1, classPath.lastIndexOf("."));
                    try {
                        className = getFullClassName(className);
                        log.info("\n==========================\n[ChatTester] Generating tests for class < " + className + " > ...");
                        new ClassRunner(className, config).start();
                    } catch (IOException e) {
                        log.error("[ChatTester] Generate tests for class " + className + " failed: " + e);
                    }
                    return "Processed " + classPath;
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

    public void init() {
        checkTargetFolder(project);
        log = getLog();
        config = new Config.ConfigBuilder(session, project, dependencyGraphBuilder, log)
                .apiKeys(apiKeys)
                .enableMultithreading(enableMultithreading)
                .tmpOutput(tmpOutput.toPath())
                .testOutput(testOutput.toPath())
                .stopWhenSuccess(stopWhenSuccess)
                .noExecution(noExecution)
                .maxThreads(maxThreads)
                .testNumber(testNumber)
                .maxRounds(maxRounds)
                .minErrorTokens(minErrorTokens)
                .maxPromptTokens(maxPromptTokens)
                .model(model)
                .temperature(temperature)
                .topP(topP)
                .frequencyPenalty(frequencyPenalty)
                .presencePenalty(presencePenalty)
                .proxy(proxy)
                .build();
        log.info("\n========================== Configuration ==========================\n");
        log.info(" Multithreading >>>> " + config.isEnableMultithreading());
        if (config.isEnableMultithreading()) {
            log.info(" - Class threads: " + config.getClassThreads() + ", Method threads: " + config.getMethodThreads());
        }
        log.info(" Stop when success >>>> " + config.isStopWhenSuccess());
        log.info(" No execution >>>> " + config.isNoExecution());
        log.info(" --- ");
        log.info(" TestOutput Path >>> " + config.getTestOutput());
        log.info(" TmpOutput Path >>> " + config.getTmpOutput());
        log.info(" MaxThreads >>> " + config.getMaxThreads());
        log.info(" TestNumber >>> " + config.getTestNumber());
        log.info(" MaxRounds >>> " + config.getMaxRounds());
        log.info(" MinErrorTokens >>> " + config.getMinErrorTokens());
        log.info(" MaxPromptTokens >>> " + config.getMaxPromptTokens());
        log.info("\n===================================================================\n");
    }

    public String getFullClassName(String name) throws IOException {
        if (isFullName(name)) {
            return name;
        }
        Path classMapPath = config.getClassMapPath();
        Map<String, List<String>> classMap = GSON.fromJson(Files.readString(classMapPath, StandardCharsets.UTF_8), Map.class);
        if (classMap.containsKey(name)) {
            if (classMap.get(name).size() > 1) {
                throw new RuntimeException("[ChatTester] Multiple classes Named " + name + ": " + classMap.get(name)
                + " Please use full qualified name!");
            }
            return classMap.get(name).get(0);
        }
        return name;
    }

    public boolean isFullName(String name) {
        if (name.contains(".")) {
            return true;
        }
        return false;
    }


    /**
     * Check if the classes is compiled
     * @param project
     */
    public static void checkTargetFolder(MavenProject project) {
        if (!new File(project.getBuild().getOutputDirectory()).exists()) {
            throw new RuntimeException("In TestCompiler.checkTargetFolder: " +
                    "The project is not compiled to the target directory. " +
                    "Please run 'mvn compile' first.");
        }
    }
}
