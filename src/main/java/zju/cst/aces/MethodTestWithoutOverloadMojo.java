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
        // 形如：org.flmelody.core.Windward#get(String, EnhancedFunction<C, ?>)
        String[] parts = input.split("#", 2);
        if (parts.length != 2) {
            // 格式不对，兜底：去掉空白直接返回
            return input.replaceAll("\\s+", "");
        }

        String methodCall = parts[1].trim(); // "get(String, EnhancedFunction<C, ?>)"

        int leftParen = methodCall.indexOf('(');
        int rightParen = methodCall.lastIndexOf(')');
        if (leftParen < 0 || rightParen < leftParen) {
            // 没括号，就返回方法部分
            return methodCall.trim();
        }

        String methodName = methodCall.substring(0, leftParen).trim();          // "get"
        String paramStr   = methodCall.substring(leftParen + 1, rightParen).trim(); // "String, EnhancedFunction<C, ?>"

        if (paramStr.isEmpty()) {
            return methodName + "()";
        }

        // ==== 1. 按泛型层次切参数（避免把泛型里的逗号当作分隔符） ====
        java.util.List<String> rawParams = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0; // 泛型嵌套层级

        for (int i = 0; i < paramStr.length(); i++) {
            char c = paramStr.charAt(i);
            if (c == '<') {
                depth++;
                current.append(c);
            } else if (c == '>') {
                depth = Math.max(0, depth - 1);
                current.append(c);
            } else if (c == ',' && depth == 0) {
                // 只有在不在泛型内部时，逗号才是参数分隔符
                rawParams.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            rawParams.add(current.toString().trim());
        }

        // ==== 2. 对每个参数，提取“简单类型名”，去掉泛型和包名 ====
        java.util.List<String> typeList = new java.util.ArrayList<>();

        for (String raw : rawParams) {
            String p = raw.trim();

            // 去掉参数名：很多情况下类型和变量名之间是最后一个空格
            // 例如： "EnhancedFunction<C, ?> function" -> "EnhancedFunction<C, ?>"
            int lastSpace = p.lastIndexOf(' ');
            if (lastSpace > 0) {
                p = p.substring(0, lastSpace);
            }

            // 去掉泛型： EnhancedFunction<C, ?> -> EnhancedFunction
            int genericIdx = p.indexOf('<');
            if (genericIdx >= 0) {
                p = p.substring(0, genericIdx);
            }

            // 去掉包名： java.lang.String -> String
            int dotIdx = p.lastIndexOf('.');
            if (dotIdx >= 0 && dotIdx < p.length() - 1) {
                p = p.substring(dotIdx + 1);
            }

            typeList.add(p);
        }

        // ==== 3. 拼回 methodSignature 形式 ====
        // 生成： get(String, EnhancedFunction)
        return methodName + "(" + String.join(", ", typeList) + ")";
    }
}
