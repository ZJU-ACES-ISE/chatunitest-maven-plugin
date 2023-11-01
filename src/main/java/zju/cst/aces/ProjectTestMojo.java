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
import zju.cst.aces.api.Task;
import zju.cst.aces.api.config.Config;

import java.io.File;

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
    @Parameter(alias = "merge", property = "merge", defaultValue = "true")
    public boolean enableMerge;
    @Parameter(property = "maxThreads", defaultValue = "0")
    public int maxThreads;
    @Parameter(property = "testNumber", defaultValue = "5")
    public int testNumber;
    @Parameter(property = "maxRounds", defaultValue = "5")
    public int maxRounds;
    @Parameter(property = "maxPromptTokens", defaultValue = "2600")
    public int maxPromptTokens;
    @Parameter(property = "minErrorTokens", defaultValue = "500")
    public int minErrorTokens;
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
        log.info("\n==========================\n[ChatUniTest] Generating tests for project " + project.getBasedir().getName() + " ...");
        log.warn("[ChatUniTest] It may consume a significant number of tokens!");
        new Task(config).startProjectTask();
    }

    public void init() {
        log = getLog();
        config = new Config.ConfigBuilder(project, dependencyGraphBuilder)
                .promptPath(promptPath)
                .examplePath(examplePath.toPath())
                .apiKeys(apiKeys)
                .enableMultithreading(enableMultithreading)
                .enableRuleRepair(enableRuleRepair)
                .tmpOutput(tmpOutput.toPath())
                .testOutput(testOutput == null? null : testOutput.toPath())
                .stopWhenSuccess(stopWhenSuccess)
                .noExecution(noExecution)
                .enableMerge(enableMerge)
                .maxThreads(maxThreads)
                .testNumber(testNumber)
                .maxRounds(maxRounds)
                .maxPromptTokens(maxPromptTokens)
                .minErrorTokens(minErrorTokens)
                .sleepTime(sleepTime)
                .dependencyDepth(dependencyDepth)
                .model(model)
                .url(url)
                .temperature(temperature)
                .topP(topP)
                .frequencyPenalty(frequencyPenalty)
                .presencePenalty(presencePenalty)
                .proxy(proxy)
                .build();
        config.print();
    }
}
