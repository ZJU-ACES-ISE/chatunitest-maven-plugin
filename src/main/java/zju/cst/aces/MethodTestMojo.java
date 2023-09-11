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
import zju.cst.aces.dto.ClassInfo;
import zju.cst.aces.dto.MethodInfo;
import zju.cst.aces.parser.ProjectParser;
import zju.cst.aces.runner.ClassRunner;
import zju.cst.aces.runner.MethodRunner;

import java.io.IOException;

/**
 * @author chenyi
 * ChatUniTest maven plugin
 */

@Mojo(name = "method")
public class MethodTestMojo
        extends ProjectTestMojo {
    @Parameter(property = "selectMethod", required = true)
    public String selectMethod;

    /**
     * Generate test for target method in given class
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
        String className = selectMethod.split("#")[0];
        String methodName = selectMethod.split("#")[1];

        if (! config.getParseOutput().toFile().exists()) {
            log.info("\n==========================\n[ChatTester] Parsing class info ...");
            ProjectParser parser = new ProjectParser(config);
            parser.parse();
            log.info("\n==========================\n[ChatTester] Parse finished");
        }

        log.info("\n==========================\n[ChatTester] Generating tests for class: < " + className
                + "> method: < " + methodName + " > ...");

        try {
            String fullClassName = getFullClassName(config, className);
            ClassRunner classRunner = new ClassRunner(fullClassName, config);
            ClassInfo classInfo = classRunner.classInfo;
            MethodInfo methodInfo = null;
            if (methodName.matches("\\d+")) { // use method id instead of method name
                String methodId = methodName;
                for (String mSig : classInfo.methodSigs.keySet()) {
                    if (classInfo.methodSigs.get(mSig).equals(methodId)) {
                        methodInfo = classRunner.getMethodInfo(config, classInfo, mSig);
                        break;
                    }
                }
                if (methodInfo == null) {
                    throw new IOException("Method " + methodName + " in class " + fullClassName + " not found");
                }
                try {
                    new MethodRunner(fullClassName, config, methodInfo).start();
                } catch (Exception e) {
                    log.error("Error when generating tests for " + methodName + " in " + className + " " + project.getArtifactId());
                }
            } else {
                for (String mSig : classInfo.methodSigs.keySet()) {
                    if (mSig.split("\\(")[0].equals(methodName)) {
                        methodInfo = classRunner.getMethodInfo(config, classInfo, mSig);
                        if (methodInfo == null) {
                            throw new IOException("Method " + methodName + " in class " + fullClassName + " not found");
                        }
                        try {
                            new MethodRunner(fullClassName, config, methodInfo).start(); // generate for all methods with the same name;
                        } catch (Exception e) {
                            log.error("Error when generating tests for " + methodName + " in " + className + " " + project.getArtifactId());
                        }
                    }
                }
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Method not found: " + methodName + " in " + className + " " + project.getArtifactId());
        }

        log.info("\n==========================\n[ChatTester] Generation finished");
    }
}
