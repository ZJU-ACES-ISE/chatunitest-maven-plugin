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
import zju.cst.aces.parser.ProjectParser;
import zju.cst.aces.runner.ClassRunner;

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
        checkTargetFolder(project);
        init();
        if (project.getPackaging().equals("pom")) {
            log.info("\n==========================\n[ChatTester] Skip pom-packaging ...");
            return;
        }
        printConfiguration();
        String className = selectClass;
        if (! config.getParseOutput().toFile().exists()) {
            log.info("\n==========================\n[ChatTester] Parsing class info ...");
            ProjectParser parser = new ProjectParser(config);
            parser.parse();
            log.info("\n==========================\n[ChatTester] Parse finished");
        }

        log.info("\n==========================\n[ChatTester] Generating tests for class < " + className + " > ...");
        try {
            new ClassRunner(getFullClassName(config, className), config).start();
        } catch (IOException e) {
            log.warn("Class not found: " + className + " in " + project.getArtifactId());
        }

        log.info("\n==========================\n[ChatTester] Generation finished");
    }
}
