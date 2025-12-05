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
        if (shouldSkip()) return;
        // warn if aggregator and no module
        if ("pom".equals(project.getPackaging()) && (module == null || module.isEmpty())) {
            log.warn("[ChatUniTest] You are running on an aggregator POM. " +
                    "Specify -Dmodule=<submodule> or use -pl/-f to target a submodule.");
            return;
        }

        String className = selectMethod.split("#")[0];
        String methodPart = selectMethod.split("#")[1]; // "getFen()" 或 "findEnPassantTarget(Square, Side)"

        // methodName 只要方法名，不要括号和参数
        String methodName;
        int idx = methodPart.indexOf('(');
        if (idx >= 0) {
            methodName = methodPart.substring(0, idx).trim();   // "getFen"
        } else {
            methodName = methodPart.trim();                     // 兜底情况
        }
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
        // 形如：com.xxx.Board#getFen() 或 com.xxx.Board#findEnPassant(Square target, Side side)
        String[] parts = input.split("#", 2);
        if (parts.length != 2) {
            return input; // 格式不对就直接原样返回，避免 NPE
        }

        String methodCall = parts[1];    // getFen() / findEnPassant(Square target, Side side)

        int leftParen = methodCall.indexOf('(');
        int rightParen = methodCall.lastIndexOf(')');

        if (leftParen < 0 || rightParen < leftParen) {
            return methodCall.trim();    // 没有括号就直接返回方法名
        }

        String methodName = methodCall.substring(0, leftParen).trim();
        String params = methodCall.substring(leftParen + 1, rightParen).trim();

        // 只保留参数类型，并规范化为 "Type1, Type2"（逗号后一个空格）
        String simplifiedParams = "";
        if (!params.isEmpty()) {
            simplifiedParams = Pattern.compile(",\\s*")
                    .splitAsStream(params)
                    .map(p -> p.trim().split("\\s+")[0]) // 取类型部分
                    .collect(java.util.stream.Collectors.joining(", "));
        }

        return methodName + "(" + simplifiedParams + ")";
    }
}
