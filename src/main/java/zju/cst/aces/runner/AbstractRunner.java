package zju.cst.aces.runner;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import freemarker.template.TemplateException;
import okhttp3.Response;
import zju.cst.aces.config.Config;
import zju.cst.aces.dto.*;
import zju.cst.aces.parser.ClassParser;
import zju.cst.aces.prompt.PromptGenerator;
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
    public PromptGenerator promptGenerator = new PromptGenerator();
    // get configuration from Config, and move init() to Config
    public AbstractRunner(String fullClassname, Config config) throws IOException {
        fullClassName = fullClassname;
        className = fullClassname.substring(fullClassname.lastIndexOf(".") + 1);
        this.config = config;
        errorOutputPath = config.getErrorOutput();
        parseOutputPath = config.getParseOutput();
        testOutputPath = config.getTestOutput();
        promptGenerator.setConfig(config);
    }

    public List<Message> generateMessages(PromptInfo promptInfo) throws IOException {
        List<Message> messages = new ArrayList<>();
        if (promptInfo.errorMsg == null) { // round 1
            messages.add(Message.ofSystem(generateSystemPrompt(promptInfo)));
        }
        messages.add(Message.of(generateUserPrompt(promptInfo)));
        return messages;
    }

    // TODO: 替换为新的prompt生成方法，参考python版本的prompt(在askGPT.py里用jinja2生成)
    public String generateUserPrompt(PromptInfo promptInfo) throws IOException {
        return promptGenerator.getUserPrompt(promptInfo);
    }


    // TODO: 替换为新的prompt生成方法，参考python版本的prompt(在askGPT.py里用jinja2生成)
    public String generateSystemPrompt(PromptInfo promptInfo) {
       return promptGenerator.getSystemPrompt(promptInfo);
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

    // TODO: manually import * and source class improts
    public String repairImports(String code, List<String> imports, boolean asterisk) {
        CompilationUnit cu = StaticJavaParser.parse(code);
        if (asterisk) {
            cu.addImport("org.mockito", false, true);
            cu.addImport("org.junit.jupiter.api", false, true);
            cu.addImport("org.mockito.Mockito", true, true);
            cu.addImport("org.junit.jupiter.api.Assertions", true, true);
        }
        imports.forEach(i -> cu.addImport(i.replace("import ", "").replace(";", "")));
        return cu.toString();
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
            testCase = repairImports(testCase, timeoutImport, true);
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

    // TODO: 将单个测试方法包装到具有适当imports的测试类中
    public String wrapTestMethod(String testMethod) {
        return testMethod;
    }

    // TODO: 将每轮的生成结果合并为最终测试套件
    public void generateTestSuite() {

    }
}
