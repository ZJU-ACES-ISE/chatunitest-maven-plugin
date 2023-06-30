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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import zju.cst.aces.parser.ProjectParser;
import zju.cst.aces.runner.ClassRunner;
import zju.cst.aces.utils.Config;
import zju.cst.aces.utils.TestCompiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author chenyi
 * A demo of ChatUniTest maven plugin
 */

@Mojo(name = "project")
public class ProjectTestMojo
        extends AbstractMojo {
    @Parameter(name = "project", defaultValue = "${basedir}")
    public String project;
    @Parameter(name = "testOutput", defaultValue = "chatunitest-tests")
    public String testOutput;
    @Parameter(name = "tmpOutput", defaultValue = "/tmp/chatunitest-info")//TODO: Use system file separator (for windows)
    public String tmpOutput;
    @Parameter(name = "apiKeys", required = true)
    public String[] apiKeys;
    @Parameter(name = "maxRounds", defaultValue = "6")
    public int maxRounds;
    @Parameter(name = "minErrorTokens", defaultValue = "500")
    public int minErrorTokens;
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

    /**
     * Generate tests for all classes in the project
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException {
        init();
        getLog().info("\n==========================\n[ChatTester] Generating tests for project " + project + " ...");
        getLog().warn("\n==========================\n[ChatTester] It may consume a significant number of tokens!");
        tmpOutput = Paths.get(tmpOutput, Paths.get(project).getFileName().toString()).toString();
        String parseOutput = tmpOutput + File.separator + "class-info";
        parseOutput = parseOutput.replace("/", File.separator);
        Path projectPath = Paths.get(project).resolve("src" + File.separator + "main" + File.separator + "java");

        String jarDeps = tmpOutput + File.separator + "jar-deps";
        ProjectParser parser = new ProjectParser(projectPath.toString(), parseOutput, jarDeps);
        if (! (new File(parseOutput).exists())) {
            getLog().info("\n==========================\n[ChatTester] Parsing class info ...");
            parser.parse();
            getLog().info("\n==========================\n[ChatTester] Parse finished");
        }

        List<String> classPaths = new ArrayList<>();
        parser.scanSourceDirectory(projectPath.toFile(), classPaths);

        TestCompiler.backupTestFolder();
        for (String classPath : classPaths) {
            String className = classPath.substring(classPath.lastIndexOf(File.separator) + 1, classPath.lastIndexOf("."));
            getLog().info("\n==========================\n[ChatTester] Generating tests for class " + className + " ...");
            try {
                new ClassRunner(className, parseOutput, testOutput).start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        TestCompiler.restoreTestFolder();

        getLog().info("\n==========================\n[ChatTester] Generation finished");
    }

    public void init() {
        Config.setApiKeys(apiKeys);
        Config.setModel(model);
        Config.setMaxRounds(maxRounds);
        Config.setMinErrorTokens(minErrorTokens);
        Config.setTemperature(temperature);
        Config.setTopP(topP);
        Config.setFrequencyPenalty(frequencyPenalty);
        Config.setPresencePenalty(presencePenalty);
    }
}
