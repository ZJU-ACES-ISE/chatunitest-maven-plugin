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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
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
 * ChatUniTest maven plugin
 */

@Mojo(name = "project")
public class ProjectTestMojo
        extends AbstractMojo {
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    public MavenProject project;
    @Parameter(defaultValue = "chatunitest-tests", property = "testOutput")
    public String testOutput;
    @Parameter(defaultValue = "/tmp/chatunitest-info", property = "tmpOutput")
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

    @Component(hint = "default")
    private DependencyGraphBuilder dependencyGraphBuilder;

    public String parseOutput;


    /**
     * Generate tests for all classes in the project
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException {
        init();
        getLog().info("\n==========================\n[ChatTester] Generating tests for project " + project.getBasedir().getName() + " ...");
        getLog().warn("[ChatTester] It may consume a significant number of tokens!");

        Path srcMainJavaPath = Paths.get(project.getBasedir().getAbsolutePath(), "src", "main", "java");
        if (!srcMainJavaPath.toFile().exists()) {
            getLog().info("\n==========================\n[ChatTester] No compile source found in " + project);
            return;
        }
        ProjectParser parser = new ProjectParser(srcMainJavaPath.toString(), parseOutput);
        if (! (new File(parseOutput).exists())) {
            getLog().info("\n==========================\n[ChatTester] Parsing class info ...");
            parser.parse();
            getLog().info("\n==========================\n[ChatTester] Parse finished");
        }

        List<String> classPaths = new ArrayList<>();
        parser.scanSourceDirectory(srcMainJavaPath.toFile(), classPaths);

        TestCompiler.backupTestFolder();
        for (String classPath : classPaths) {
            String className = classPath.substring(classPath.lastIndexOf(File.separator) + 1, classPath.lastIndexOf("."));
            getLog().info("\n==========================\n[ChatTester] Generating tests for class " + className + " ...");
            try {
                new ClassRunner(className, parseOutput, testOutput).start();
            } catch (IOException e) {
                getLog().error("[ChatTester] Generate tests for class " + className + " failed: " + e);
            }
        }
        TestCompiler.restoreTestFolder();

        getLog().info("\n==========================\n[ChatTester] Generation finished");
    }

    public void init() {
        Config.setSession(session);
        Config.setProject(project);
        Config.setDependencyGraphBuilder(dependencyGraphBuilder);
        Config.setApiKeys(apiKeys);
        Config.setModel(model);
        Config.setMaxRounds(maxRounds);
        Config.setMinErrorTokens(minErrorTokens);
        Config.setTemperature(temperature);
        Config.setTopP(topP);
        Config.setFrequencyPenalty(frequencyPenalty);
        Config.setPresencePenalty(presencePenalty);
        tmpOutput = String.valueOf(Paths.get(tmpOutput, project.getArtifactId()));
        parseOutput = tmpOutput + File.separator + "class-info";
        parseOutput = parseOutput.replace("/", File.separator);
    }
}
