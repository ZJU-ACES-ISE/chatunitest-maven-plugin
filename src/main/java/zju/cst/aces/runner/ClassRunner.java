package zju.cst.aces.runner;

import org.codehaus.plexus.util.FileUtils;
import zju.cst.aces.parser.ClassParser;
import zju.cst.aces.utils.ClassInfo;
import zju.cst.aces.utils.MethodInfo;
import zju.cst.aces.utils.PromptInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ClassRunner extends AbstractRunner {
    public ClassInfo classInfo;
    public File infoDir;

    public ClassRunner(String classname, String parsePath, String testPath) throws IOException {
        super(classname, parsePath, testPath);
        infoDir = new File(parseOutputPath + File.separator + className);
        if (!infoDir.isDirectory()) {
            getLog().error("Error: " + className + "no parsed info found");
        }
        File classInfoFile = new File(parseOutputPath
                + File.separator + className + File.separator + "class.json");
        classInfo = GSON.fromJson(FileUtils.fileRead(classInfoFile), ClassInfo.class);
        className = classInfo.className;
    }

    public void start() throws IOException {
        List<File> files = List.of(infoDir.listFiles());
        for (String mSig : classInfo.methodSignatures.keySet()) {
            MethodInfo methodInfo = getMethodInfo(classInfo, mSig);
            if (methodInfo == null) {
                continue;
            }
            new MethodRunner(className, parseOutputPath.toString(), testOutputPath.toString(), methodInfo).start();
        }
    }

    public PromptInfo generatePromptInfoWithoutDep(ClassInfo classInfo, MethodInfo methodInfo) {
        PromptInfo promptInfo = new PromptInfo(
                false,
                classInfo.className,
                methodInfo.methodName,
                methodInfo.methodSignature,
                methodInfo.sourceCode);
        String fields = joinLines(classInfo.fields);
        String methods = filterAndJoinLines(classInfo.briefMethods, methodInfo.brief);
        String imports = joinLines(classInfo.imports);

        String information = classInfo.packageDeclaration
                + "\n" + imports
                + "\n" + classInfo.classSignature
                + " {"
                + "\n" + fields
                + "\n" + methods
                + "\n" + methodInfo.sourceCode
                + "\n}";

        promptInfo.setInfo(information);

        return promptInfo;
    }

    public PromptInfo generatePromptInfoWithDep(ClassInfo classInfo, MethodInfo methodInfo) throws IOException {
        PromptInfo promptInfo = new PromptInfo(
                true,
                classInfo.className,
                methodInfo.methodName,
                methodInfo.methodSignature,
                methodInfo.sourceCode);
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
            getDepInfo(promptInfo, depClassName, depMethods);
        }

        String fields = joinLines(classInfo.fields);
        String imports = joinLines(classInfo.imports);

        String information = classInfo.packageDeclaration
                + "\n" + imports
                + "\n" + classInfo.classSignature
                + " {\n";
        //TODO: handle used fields instead of all fields
        if (methodInfo.useField) {
            information += fields + "\n" + joinLines(classInfo.getterSetters) + "\n";
        }
        if (classInfo.hasConstructor) {
            information += joinLines(classInfo.constructors);
        }
        information += joinLines(otherBriefMethods) + "\n";
        information += methodInfo.sourceCode + "\n}";

        promptInfo.setInfo(information);
        return promptInfo;
    }

    public MethodInfo getMethodInfo(ClassInfo info, String mSig) throws IOException {
        Path depMethodInfoPath = parseOutputPath
                .resolve(info.className)
                .resolve(ClassParser.getFilePathBySig(mSig, info));
        if (!depMethodInfoPath.toFile().exists()) {
            return null;
        }
        return GSON.fromJson(Files.readString(depMethodInfoPath), MethodInfo.class);
    }

    public void getDepInfo(PromptInfo promptInfo, String depClassName, Set<String> depMethods) throws IOException {
        Path depClassInfoPath = parseOutputPath.resolve(depClassName).resolve("class.json");
        if (!depClassInfoPath.toFile().exists()) {
            return;
        }
        ClassInfo depClassInfo = GSON.fromJson(Files.readString(depClassInfoPath), ClassInfo.class);

        String classSig = depClassInfo.classSignature;
        String fields = joinLines(depClassInfo.fields);
        String constructors = joinLines(depClassInfo.constructors);
        Map<String, String> classDeps = new HashMap<>();
        Map<String, String> methodDeps = new HashMap<>();

        String basicInfo = classSig + " {\n" + fields + "\n";
        if (depClassInfo.hasConstructor) {
            basicInfo += constructors + "\n";
        }

        classDeps.put(depClassName, basicInfo + "}");
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

        promptInfo.addClassDeps(classDeps);
        promptInfo.addMethodDeps(methodDeps);
    }
}
