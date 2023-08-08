package zju.cst.aces.runner;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.Response;
import zju.cst.aces.config.Config;
import zju.cst.aces.dto.*;
import zju.cst.aces.parser.ClassParser;
import zju.cst.aces.util.CodeExtractor;
import zju.cst.aces.util.TokenCounter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class AbstractRunner {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String separator = "_";
    public static int testTimeOut = 8000;
    public Path parseOutputPath;
    public Path testOutputPath;
    public Path errorOutputPath;
    public String className;
    public String fullClassName;
    public Config config;

    // get configuration from Config, and move init() to Config
    public AbstractRunner(String fullClassname, Config config) throws IOException {
        fullClassName = fullClassname;
        className = fullClassname.substring(fullClassname.lastIndexOf(".") + 1);
        this.config = config;
        errorOutputPath = config.getErrorOutput();
        parseOutputPath = config.getParseOutput();
        testOutputPath = config.getTestOutput();
    }

    public List<Message> generateMessages(PromptInfo promptInfo) throws IOException {
        List<Message> messages = new ArrayList<>();
        if (promptInfo.errorMsg == null) { // round 1
            messages.add(Message.ofSystem(generateSystemPrompt(promptInfo)));
        }
        messages.add(Message.of(generateUserPrompt(promptInfo)));
        return messages;
    }

    public String generateUserPrompt(PromptInfo promptInfo) throws IOException {
        String user = null;
        if (promptInfo.errorMsg == null) {
            user = String.format("The focal method is `%s` in the focal class `%s`, and their information is\n```%s```",
                    promptInfo.getMethodSignature(), promptInfo.getClassName(), promptInfo.getInfo());
            if (!promptInfo.getOtherMethods().trim().isEmpty()) {
                user += String.format("\nSignatures of Other methods in the focal class are\n```%s```", promptInfo.getOtherMethods());
            }
            if (promptInfo.hasDep) {
                for (Map<String, String> cDeps : promptInfo.getConstructorDeps()) {
                    for (Map.Entry<String, String> entry : cDeps.entrySet()) {
                        user += String.format("\nThe brief information of dependent class `%s` is\n```%s```", entry.getKey(), entry.getValue());
                    }
                }
                for (Map<String, String> mDeps : promptInfo.getMethodDeps()) {
                    for (Map.Entry<String, String> entry : mDeps.entrySet()) {
                        user += String.format("\nThe brief information of dependent method `%s` is\n```%s```", entry.getKey(), entry.getValue());
                    }
                }
            }
        } else {
            int promptTokens = TokenCounter.countToken(promptInfo.getUnitTest())
                    + TokenCounter.countToken(promptInfo.getMethodSignature())
                    + TokenCounter.countToken(promptInfo.getClassName())
                    + TokenCounter.countToken(promptInfo.getInfo())
                    + TokenCounter.countToken(promptInfo.getOtherMethods());
            int allowedTokens = Math.max(config.getMaxPromptTokens() - promptTokens, config.getMinErrorTokens());
            TestMessage errorMsg = promptInfo.getErrorMsg();
            String processedErrorMsg = "";
            for (String error : errorMsg.getErrorMessage()) {
                if (TokenCounter.countToken(processedErrorMsg + error + "\n") <= allowedTokens) {
                    processedErrorMsg += error + "\n";
                }
            }
            config.getLog().debug("Allowed tokens: " + allowedTokens);
            config.getLog().debug("Processed error message: \n" + processedErrorMsg);

            user = String.format("I need you to fix an error in a unit test, an error occurred while compiling and executing\n" +
                            "The unit test is:\n" +
                            "```\n%s```\n" +
                            "The error message is:\n" +
                            "```\n%s```\n" +
                            "The unit test is testing the method %s in the class %s,\n" +
                            "the source code of the method under test and its class is:\n" +
                            "```\n%s```\n",
                    promptInfo.getUnitTest(), processedErrorMsg, promptInfo.getMethodSignature(), promptInfo.getClassName(), promptInfo.getInfo());
            if (!promptInfo.getOtherMethods().trim().isEmpty()) {
                user += String.format("The signatures of other methods in its class are:\n```\n%s```\n", promptInfo.getOtherMethods());
            }
            user += "Please fix the error and return the whole fixed unit test." +
                    " You can use Junit 5, Mockito 3 and reflection. No explanation is needed.\n";
        }
        return user;
    }

    public String generateSystemPrompt(PromptInfo promptInfo) {
        if (promptInfo.isHasDep()) {
            return config.systemPromptWithDep;
        }
        return config.systemPromptWithoutDep;
    }

    public String joinLines(List<String> lines) {
        return lines.stream().collect(Collectors.joining("\n"));
    }

    public String filterAndJoinLines(List<String> lines, String filter) {
        return lines.stream()
                .filter(line -> !line.equals(filter))
                .collect(Collectors.joining("\n"));
    }

    public String parseResponse(Response response) {
        if (response == null) {
            return "";
        }
        Map<String, Object> body = GSON.fromJson(response.body().charStream(), Map.class);
        String content = ((Map<String, String>) ((Map<String, Object>) ((ArrayList<?>) body.get("choices")).get(0)).get("message")).get("content");
        return content;
    }

    public void exportTest(String code, Path savePath) {
        if (!savePath.toAbsolutePath().getParent().toFile().exists()) {
            savePath.toAbsolutePath().getParent().toFile().mkdirs();
        }
        //set charset utf-8
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(savePath.toFile()), StandardCharsets.UTF_8)) {
            writer.write(code);
        } catch (IOException e) {
            throw new RuntimeException("In AbstractRunner.exportTest: " + e);
        }
    }

    public String extractCode(String content) {
        try {
            return new CodeExtractor(content).getExtractedCode();
        } catch (Exception e) {
            config.getLog().error("In AbstractRunner.extractCode: " + e);
        }
        return "";
    }

    public String repairImports(String code, List<String> imports) {
        String[] codeParts = code.trim().split("\\n", 2);
        String firstLine = codeParts[0];
        String _code = codeParts[1];
        for (int i = imports.size() - 1; i >= 0; i--) {
            String _import = imports.get(i);
            if (!_code.contains(_import)) {
                _code = _import + "\n" + _code;
            }
        }
        return firstLine + "\n" + _code;
    }

    public String repairPackage(String code, String packageInfo) {
        CompilationUnit cu = StaticJavaParser.parse(code).setPackageDeclaration(packageInfo
                .replace("package ", "").replace(";", ""));
        return cu.toString();
    }

    public String addTimeout(String testCase, int timeout) {
        // Check JUnit version
        String junit4 = "import org.junit.Test";
        String junit5 = "import org.junit.jupiter.api.Test";
        if (testCase.contains(junit4)) {  // JUnit 4
            if (testCase.contains("@Test(timeout =")) {
                return testCase;
            }
            testCase = testCase.replace("@Test(", String.format("@Test(timeout = %d, ", timeout));
            return testCase.replace("@Test\n", String.format("@Test(timeout = %d)%n", timeout));
        } else if (testCase.contains(junit5)) {  // JUnit 5
            if (testCase.contains("import org.junit.jupiter.api.Timeout;")) {
                return testCase;
            }
            List<String> timeoutImport = new ArrayList<>();
            timeoutImport.add("import org.junit.jupiter.api.Timeout;");
            testCase = repairImports(testCase, timeoutImport);
            return testCase.replace("@Test\n", String.format("@Test%n    @Timeout(%d)%n", timeout));
        } else {
            config.getLog().warn("Generated with unknown JUnit version, try without adding timeout.");
        }
        return testCase;
    }

    public String changeTestName(String code, String className, String newName) {
        CompilationUnit cu = StaticJavaParser.parse(code);
        cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(c -> c.setName(newName));
        return cu.toString();
    }

    public PromptInfo generatePromptInfoWithoutDep(ClassInfo classInfo, MethodInfo methodInfo) {
        PromptInfo promptInfo = new PromptInfo(
                false,
                classInfo.className,
                methodInfo.methodName,
                methodInfo.methodSignature);
        String fields = joinLines(classInfo.fields);
        String methods = filterAndJoinLines(classInfo.briefMethods, methodInfo.brief);
        String imports = joinLines(classInfo.imports);

        String information = classInfo.packageDeclaration
                + "\n" + imports
                + "\n" + classInfo.classSignature
                + " {"
                + "\n" + fields
                + "\n" + methodInfo.sourceCode
                + "\n}";

        promptInfo.setInfo(information);
        promptInfo.setOtherMethods(methods);

        return promptInfo;
    }

    public PromptInfo generatePromptInfoWithDep(ClassInfo classInfo, MethodInfo methodInfo) throws IOException {
        PromptInfo promptInfo = new PromptInfo(
                true,
                classInfo.className,
                methodInfo.methodName,
                methodInfo.methodSignature);
        List<String> otherBriefMethods = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : methodInfo.dependentMethods.entrySet()) {
            String depClassName = entry.getKey();
            if (depClassName.equals(className)) {
                Set<String> otherSig = methodInfo.dependentMethods.get(depClassName);
                for (String otherMethod : otherSig) {
                    MethodInfo otherMethodInfo = getMethodInfo(classInfo, otherMethod);
                    if (otherMethodInfo == null) {
                        continue;
                    }
                    otherBriefMethods.add(otherMethodInfo.brief);
                }
                continue;
            }
            Set<String> depMethods = entry.getValue();
            promptInfo.addMethodDeps(getDepInfo(promptInfo, depClassName, depMethods));
        }
        for (Map.Entry<String, Set<String>> entry : classInfo.constructorDeps.entrySet()) {
            String depClassName = entry.getKey();
            Set<String> depMethods = entry.getValue();
            if (methodInfo.dependentMethods.containsKey(depClassName)) {
                continue;
            }
            promptInfo.addConstructorDeps(getDepInfo(promptInfo, depClassName, depMethods));
        }

        String fields = joinLines(classInfo.fields);
        String imports = joinLines(classInfo.imports);

        String information = classInfo.packageDeclaration
                + "\n" + imports
                + "\n" + classInfo.classSignature
                + " {\n";
        //TODO: handle used fields instead of all fields
        String otherMethods = "";
        if (classInfo.hasConstructor) {
            otherMethods += joinLines(classInfo.constructors) + "\n";
        }
        if (methodInfo.useField) {
            information += fields + "\n";
            otherMethods +=  joinLines(classInfo.getterSetters) + "\n";
        }
        otherMethods += joinLines(otherBriefMethods) + "\n";
        information += methodInfo.sourceCode + "\n}";

        promptInfo.setInfo(information);
        promptInfo.setOtherMethods(otherMethods);
        return promptInfo;
    }

    public MethodInfo getMethodInfo(ClassInfo info, String mSig) throws IOException {
        String packagePath = info.packageDeclaration
                .replace("package ", "")
                .replace(".", File.separator)
                .replace(";", "");
        Path depMethodInfoPath = parseOutputPath
                .resolve(packagePath)
                .resolve(info.className)
                .resolve(ClassParser.getFilePathBySig(mSig, info));
        if (!depMethodInfoPath.toFile().exists()) {
            return null;
        }
        return GSON.fromJson(Files.readString(depMethodInfoPath, StandardCharsets.UTF_8), MethodInfo.class);
    }

    public Map<String, String> getDepInfo(PromptInfo promptInfo, String depClassName, Set<String> depMethods) throws IOException {
        Path depClassInfoPath = parseOutputPath.resolve(depClassName).resolve("class.json");
        if (!depClassInfoPath.toFile().exists()) {
            return null;
        }
        ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath, StandardCharsets.UTF_8), ClassInfo.class);

        String classSig = depClassInfo.classSignature;
        String fields = joinLines(depClassInfo.fields);
        String constructors = joinLines(depClassInfo.constructors);
        Map<String, String> methodDeps = new HashMap<>();

        String basicInfo = classSig + " {\n" + fields + "\n";
        if (depClassInfo.hasConstructor) {
            basicInfo += constructors + "\n";
        }

        String briefDepMethods = "";
        for (String sig : depMethods) {
            //TODO: identify used fields in dependent class
            MethodInfo depMethodInfo = getMethodInfo(depClassInfo, sig);
            if (depMethodInfo == null) {
                continue;
            }
            briefDepMethods += depMethodInfo.brief + "\n";
        }
        String getterSetter = joinLines(depClassInfo.getterSetters) + "\n";
        methodDeps.put(depClassName, basicInfo + getterSetter + briefDepMethods + "}");
        return methodDeps;
    }
}
