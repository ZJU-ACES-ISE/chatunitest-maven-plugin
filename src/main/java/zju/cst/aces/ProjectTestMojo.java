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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    @Parameter(property = "url", defaultValue = "https://api.gptsapi.net/v1/chat/completions")
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
    @Parameter(property = "phaseType",defaultValue = "COVERUP")
    public String phaseType;
    @Parameter(property = "smartUnitTest_jar_path",defaultValue = "D:\\APP\\IdeaProjects\\chatunitest-maven-plugin-corporation\\src\\main\\resources\\smartut-master-1.1.0.jar")
    public String smartUnitTest_path;
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
            new Task(config, new RunnerImpl(config)).startProjectTask();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
                .build();
        if(phaseType.equals("TELPA")){
            TelpaInit telpaInit=new TelpaInit();
            telpaInit.generateSmartUnitTest(project,smartUnitTest_path,config);
        }
        config.setPluginSign(phaseType);
        config.print();
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
