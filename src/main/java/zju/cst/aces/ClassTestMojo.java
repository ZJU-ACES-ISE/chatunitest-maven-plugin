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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import zju.cst.aces.api.Task;
import zju.cst.aces.api.impl.RunnerImpl;

import java.io.File;
import java.io.IOException;

/**
 * @author chenyi
 * ChatUniTest maven plugin
 */

@Mojo(name = "class")
public class ClassTestMojo
        extends ProjectTestMojo {
    @Parameter(property = "selectClass", required = true)
    public String selectClass;

    /**
     * Generate tests for target class
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException {
        init();
        String className = selectClass;

        try {
            // Execute Maven commands only if phaseType is TELPA
            if ("TELPA".equals(phaseType)) {
                File baseDir = project.getBasedir();
                log.info("TELPA mode: Executing Maven commands in the target project: " + baseDir.getAbsolutePath());

                // Execute Maven command sequence
                log.info("Step 1: Cleaning the project");
                executeMavenCommand(baseDir, "clean");

                log.info("Step 2: Compiling the project");
                executeMavenCommand(baseDir, "compile");

                log.info("Step 3: Installing the project (skipping tests)");
                executeMavenCommand(baseDir, "install", "-DskipTests");

                log.info("Step 4: Copying dependencies");
                executeMavenCommand(baseDir, "dependency:copy-dependencies");

                log.info("Maven commands executed successfully.");

                // Execute SmartUnitTest generation
                log.info("Generating SmartUnitTest...");
                TelpaInit telpaInit = new TelpaInit();
                telpaInit.generateSmartUnitTest(project, smartUnitTest_path, config);

                log.info("SmartUnitTest generation completed. Starting test generation for class: " + className);
            }

            // Generate tests for the class
            new Task(config, new RunnerImpl(config)).startClassTask(className);
        } catch (Exception e) {
            log.error("Error during execution: " + e.getMessage(), e);
            throw new MojoExecutionException("Failed to execute Maven commands or generate tests for class: " + className, e);
        }
    }
}
