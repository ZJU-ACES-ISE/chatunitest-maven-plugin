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
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import zju.cst.aces.api.Project;
import zju.cst.aces.api.Task;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.api.impl.ProjectImpl;
import zju.cst.aces.api.impl.RunnerImpl;
import zju.cst.aces.logger.MavenLogger;
import zju.cst.aces.parser.ProjectParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

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
    @Parameter(property = "testOutput")
    public File testOutput;
    @Parameter(defaultValue = "/tmp/chatunitest-info", property = "tmpOutput")
    public File tmpOutput;
    @Parameter(property = "promptPath")
    public File promptPath;
    @Parameter(property = "examplePath", defaultValue = "${project.basedir}/exampleUsage.json")
    public File examplePath;
    @Parameter(property = "url", defaultValue = "https://api.openai.com/v1/chat/completions")
    public String url;
    @Parameter(property = "model", defaultValue = "gpt-3.5-turbo")
    public String model;
    @Parameter(property = "apiKeys", required = true)
    public String[] apiKeys;
    @Parameter(property = "stopWhenSuccess", defaultValue = "true")
    public boolean stopWhenSuccess;
    @Parameter(property = "noExecution", defaultValue = "false")
    public boolean noExecution;
    @Parameter(alias = "thread", property = "thread", defaultValue = "true")
    public boolean enableMultithreading;
    @Parameter(alias = "ruleRepair", property = "ruleRepair", defaultValue = "true")
    public boolean enableRuleRepair;
    @Parameter(alias = "obfuscate", property = "obfuscate", defaultValue = "false")
    public boolean enableObfuscate;
    @Parameter(alias = "merge", property = "merge", defaultValue = "true")
    public boolean enableMerge;
    @Parameter(property = "obfuscateGroupIds")
    public String[] obfuscateGroupIds;
    @Parameter(property = "maxThreads", defaultValue = "0")
    public int maxThreads;
    @Parameter(property = "testNumber", defaultValue = "5")
    public int testNumber;
    @Parameter(property = "maxRounds", defaultValue = "5")
    public int maxRounds;
    @Parameter(property = "maxPromptTokens", defaultValue = "-1")
    public int maxPromptTokens;
    @Parameter(property = "minErrorTokens", defaultValue = "500")
    public int minErrorTokens;
    @Parameter(property = "maxResponseTokens", defaultValue = "1024")
    public int maxResponseTokens;
    @Parameter(property = "sleepTime", defaultValue = "0")
    public int sleepTime;
    @Parameter(property = "dependencyDepth", defaultValue = "1")
    public int dependencyDepth;
    @Parameter(property = "temperature", defaultValue = "0.5")
    public Double temperature;
    @Parameter(property = "topP", defaultValue = "1")
    public  int topP;
    @Parameter(property = "frequencyPenalty", defaultValue = "0")
    public int frequencyPenalty;
    @Parameter(property = "presencePenalty", defaultValue = "0")
    public int presencePenalty;
    @Parameter(property = "proxy",defaultValue = "null:-1")
    public String proxy;
    @Parameter(property = "phaseType",defaultValue = "CHATUNITEST")
    public String phaseType;
    @Parameter(property = "smartUnitTest_jar_path",defaultValue = "")
    public String smartUnitTest_path;
    @Parameter(property = "mavenHome", defaultValue = "${maven.home}")
    public String mavenHome;
    @Parameter(property = "sampleSize", defaultValue = "10")
    public int sampleSize;
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
        log.info(String.format("\n==========================\n[%s] Generating tests for project %s ...", phaseType, project.getBasedir().getName()));
        log.warn(String.format("[%s] It may consume a significant number of tokens!", phaseType));

        try {
//            // Execute Maven commands only if phaseType is TELPA
//            if ("TELPA".equals(phaseType)) {
//                File baseDir = project.getBasedir();
//                log.info("TELPA mode: Executing Maven commands in the target project: " + baseDir.getAbsolutePath());
//
//                // Execute Maven command sequence
//                log.info("Step 1: Cleaning the project");
//                executeMavenCommand(baseDir, "clean");
//
//                log.info("Step 2: Compiling the project");
//                executeMavenCommand(baseDir, "compile");
//
//                log.info("Step 3: Installing the project (skipping tests)");
//                executeMavenCommand(baseDir, "install", "-DskipTests");
//
//                log.info("Step 4: Copying dependencies");
//                executeMavenCommand(baseDir, "dependency:copy-dependencies");
//
//                log.info("Maven commands executed successfully.");
//
//                // Execute SmartUnitTest generation
//                log.info("Generating SmartUnitTest...");
//                TelpaInit telpaInit = new TelpaInit();
//                telpaInit.generateSmartUnitTest(project, smartUnitTest_path, config);
//
//                log.info("SmartUnitTest generation completed. Starting test generation...");
//            }

            // Generate tests
            new Task(config, new RunnerImpl(config)).startProjectTask();
        } catch (Exception e) {
            log.error("Error during execution: " + e.getMessage(), e);
            throw new MojoExecutionException("Failed to execute Maven commands or generate tests", e);
        }
    }

    public void init() {
        log = getLog();
        MavenLogger mLogger = new MavenLogger(log);
        Project myProject = new ProjectImpl(project, listClassPaths(project, dependencyGraphBuilder));
        config = new Config.ConfigBuilder(myProject)
                .logger(mLogger)
                .promptPath(promptPath)
                .examplePath(examplePath.toPath())
                .apiKeys(apiKeys)
                .enableMultithreading(enableMultithreading)
                .enableRuleRepair(enableRuleRepair)
                .tmpOutput(tmpOutput.toPath())
                .testOutput(testOutput == null? null : testOutput.toPath())
                .stopWhenSuccess(stopWhenSuccess)
                .noExecution(noExecution)
                .enableObfuscate(enableObfuscate)
                .enableMerge(enableMerge)
                .obfuscateGroupIds(obfuscateGroupIds)
                .maxThreads(maxThreads)
                .testNumber(testNumber)
                .maxRounds(maxRounds)
                .sleepTime(sleepTime)
                .dependencyDepth(dependencyDepth)
                .model(model)
                .maxResponseTokens(maxResponseTokens)
                .maxPromptTokens(maxPromptTokens)
                .minErrorTokens(minErrorTokens)
                .url(url)
                .temperature(temperature)
                .topP(topP)
                .frequencyPenalty(frequencyPenalty)
                .presencePenalty(presencePenalty)
                .proxy(proxy)
                .phaseType(phaseType)
                .sampleSize(sampleSize)
                .build();
        // SmartUnitTest generation is now handled in the execute method when phaseType is TELPA
        config.setPluginSign(phaseType);
        config.print();
    }

    /**
     * Execute Maven command in the specified directory
     * @param workingDir The directory to execute the command in
     * @param args Maven command arguments
     */
    /**
     * Check if the current Maven process is running in debug mode (mvnDebug)
     * @return true if running in debug mode, false otherwise
     * @deprecated This method is no longer used as we always use 'mvn' command
     */
    @Deprecated
    protected boolean isRunningInDebugMode() {
        try {
            // Check for debug arguments in the JVM
            String jvmArgs = System.getProperty("sun.java.command", "");
            return jvmArgs.contains("-Xdebug") || jvmArgs.contains("-agentlib:jdwp");
        } catch (Exception e) {
            log.debug("Error checking debug mode: " + e.getMessage());
            return false;
        }
    }

    /**
     * Execute Maven command in the specified directory
     * Always uses 'mvn' command regardless of whether the parent process is running with 'mvn' or 'mvnDebug'
     * @param workingDir The directory to execute the command in
     * @param args Maven command arguments
     */
    protected void executeMavenCommand(File workingDir, String... args) {
        try {
            // Set the necessary system property for Maven
            System.setProperty("maven.multiModuleProjectDirectory", workingDir.getAbsolutePath());

            log.info("Executing Maven command: mvn " + String.join(" ", args));

            // Create InvocationRequest
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(new File(workingDir, "pom.xml"));

            // Handle goals and options separately
            List<String> goals = new ArrayList<>();
            if (args.length > 0) {
                // First argument is the goal
                goals.add(args[0]);

                // Add any additional options
                if (args.length > 1) {
                    for (int i = 1; i < args.length; i++) {
                        if (args[i].startsWith("-D")) {
                            // This is a system property
                            String property = args[i].substring(2);
                            int equalsIndex = property.indexOf('=');
                            if (equalsIndex > 0) {
                                String key = property.substring(0, equalsIndex);
                                String value = property.substring(equalsIndex + 1);
                                request.getProperties().setProperty(key, value);
                            }
                        } else {
                            // This is another goal or option
                            goals.add(args[i]);
                        }
                    }
                }
            }
            request.setGoals(goals);

            // Set properties to skip unnecessary plugins
            Properties properties = request.getProperties();
            if (properties == null) {
                properties = new Properties();
                request.setProperties(properties);
            }
            properties.setProperty("cobertura.skip", "true");
            properties.setProperty("rat.skip", "true");
            properties.setProperty("dependencyVersionsCheck.skip", "true");



            // Create and configure Invoker
            Invoker invoker = new DefaultInvoker();

            // Explicitly set Maven executable to "mvn" regardless of parent process
            invoker.setMavenExecutable(new File("mvn"));

            // Set Maven home if available
            if (mavenHome != null && !mavenHome.isEmpty()) {
                log.info("Using Maven home: " + mavenHome);
                invoker.setMavenHome(new File(mavenHome));
            } else {
                log.warn("Maven home not specified. Using system Maven installation.");
            }

            // Execute Maven command
            try {
                log.info("Executing Maven goal(s): " + String.join(", ", request.getGoals()));
                org.apache.maven.shared.invoker.InvocationResult result = invoker.execute(request);

                if (result.getExitCode() != 0) {
                    if (result.getExecutionException() != null) {
                        log.error("Maven command failed with exception: " + result.getExecutionException().getMessage());
                        throw new RuntimeException("Failed to execute Maven command: " + Arrays.toString(args), result.getExecutionException());
                    } else {
                        log.error("Maven command failed with exit code: " + result.getExitCode());
                        throw new RuntimeException("Failed to execute Maven command: " + Arrays.toString(args) + ", exit code: " + result.getExitCode());
                    }
                }

                log.info("Maven command completed successfully");
            } catch (MavenInvocationException e) {
                log.error("Maven command failed: " + e.getMessage());
                throw new RuntimeException("Failed to execute Maven command: " + Arrays.toString(args), e);
            }
        } catch (Exception e) {
            log.error("Failed to execute Maven command: " + Arrays.toString(args), e);
        }
    }

    public static List<String> listClassPaths(MavenProject project, DependencyGraphBuilder dependencyGraphBuilder) {
        List<String> classPaths = new ArrayList<>();
        if (project.getPackaging().equals("jar")) {
            Path artifactPath = Paths.get(project.getBuild().getDirectory()).resolve(project.getBuild().getFinalName() + ".jar");
            if (!artifactPath.toFile().exists()) {
                throw new RuntimeException("In TestCompiler.listClassPaths: " + artifactPath + " does not exist. Run mvn install first.");
            }
            classPaths.add(artifactPath.toString());
        }
        try {
            classPaths.addAll(project.getCompileClasspathElements());
            Class<?> clazz = project.getClass();
            Field privateField = clazz.getDeclaredField("projectBuilderConfiguration");
            privateField.setAccessible(true);
            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest((DefaultProjectBuildingRequest) privateField.get(project));
            buildingRequest.setProject(project);
            DependencyNode root = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, null);
            Set<DependencyNode> depSet = new HashSet<>();
            ProjectParser.walkDep(root, depSet);
            for (DependencyNode dep : depSet) {
                if (dep.getArtifact().getFile() != null) {
                    classPaths.add(dep.getArtifact().getFile().getAbsolutePath());
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return classPaths;
    }

}
