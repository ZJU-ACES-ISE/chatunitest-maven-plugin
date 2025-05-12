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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author chenyi
 * ChatUniTest maven plugin for testing methods without overloads
 */

@Mojo(name = "methodWithoutOverload")
public class MethodTestWithoutOverloadMojo
        extends ProjectTestMojo {
    @Parameter(property = "selectMethod", required = true)
    public String selectMethod;

    /**
     * Generate test for target method with specific signature in given class
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException {
        init();
        String className = selectMethod.split("#")[0];
        String methodName = selectMethod.split("#")[1];
        String signature = simplifyMethodCall(selectMethod);


        try {
            // Execute Maven commands only if phaseType is TELPA and they haven't been executed yet
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

                log.info("SmartUnitTest generation completed. Starting test generation for method: " + className + "#" + methodName + " with signature: " + signature);

                // Set the flag to indicate that initialization has been done
            }
            // Generate tests for the method with specific signature
            new Task(config, new RunnerImpl(config)).startMethodWithoutOverloadTask(className, methodName, signature);
        } catch (Exception e) {
            log.error("Error during execution: " + e.getMessage(), e);
            throw new MojoExecutionException("Failed to execute Maven commands or generate tests for method: " + className + "#" + methodName + " with signature: " + signature, e);
        }
    }
    public static String simplifyMethodCall(String input) {
        // 匹配如 org.zju.edu.res(int num, String name)
        String regex = "(?:[a-zA-Z_][\\w\\.]*)\\.([a-zA-Z_][\\w]*)\\s*\\(([^)]*)\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String methodName = matcher.group(1);
            String params = matcher.group(2);

            // 只保留参数类型
            String simplifiedParams = "";
            if (!params.trim().isEmpty()) {
                simplifiedParams = String.join(", ",
                        // 去掉每个参数中的变量名，只保留类型
                        Pattern.compile(",\\s*")
                                .splitAsStream(params)
                                .map(param -> param.trim().split("\\s+")[0]) // 取类型
                                .reduce((a, b) -> a + ", " + b).orElse("")
                );
            }

            String replacement = methodName + "(" + simplifiedParams + ")";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
