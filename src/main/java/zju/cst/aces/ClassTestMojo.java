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
import zju.cst.aces.utils.TestCompiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        Path srcMainJavaPath = Paths.get(project.getBasedir().getAbsolutePath(), "src", "main", "java");
        if (!srcMainJavaPath.toFile().exists()) {
            log.error("\n==========================\n[ChatTester] No compile source found in " + project);
            return;
        }

        ProjectParser parser = new ProjectParser(srcMainJavaPath.toString(), parseOutput);
        if (! (new File(parseOutput).exists())) {
            log.info("\n==========================\n[ChatTester] Parsing class info ...");
            parser.parse();
            log.info("\n==========================\n[ChatTester] Parse finished");
        }

        log.info("\n==========================\n[ChatTester] Generating tests for class < " + className + " > ...");
        TestCompiler.backupTestFolder();
        try {
            new ClassRunner(getFullClassName(className), parseOutput, testOutput).start();
        } catch (IOException e) {
            throw new RuntimeException("In ClassTestMojo.execute: " + e);
        }
//        TestCompiler.restoreTestFolder();

        log.info("\n==========================\n[ChatTester] Generation finished");
    }
}
