package zju.cst.aces.util;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import zju.cst.aces.config.Config;
import zju.cst.aces.parser.ProjectParser;
import zju.cst.aces.runner.AbstractRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Merge all test classes with different methods in the same class.
 * @author <a href="mailto: sjiahui27@gmail.com">songjiahui</a>
 * @since 2023/7/10 16:47
 **/
public class TestClassMerger {

    private String sourceFullClassName;
    private String sourceClassName;
    private String packageName;
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private String targetClassName;
    public Config config;

    public TestClassMerger(Config config, String fullClassName) {
        this.config = config;
        this.sourceFullClassName = fullClassName;
        this.sourceClassName = StaticJavaParser.parseClassOrInterfaceType(fullClassName).getNameAsString();
        this.packageName = StaticJavaParser.parseClassOrInterfaceType(fullClassName).getScope().isPresent()?
                StaticJavaParser.parseClassOrInterfaceType(fullClassName).getScope().get().asString() : "";
    }

    /**
     * merge by adding suite annotation, only Junit5 is supported now.
     *
     * @return true if the test class is merged successfully.
     * @throws IOException
     */
    public boolean mergeWithSuite() throws IOException {
        targetClassName = sourceClassName + "_Suite";
        Path testSourcePath = config.getTestOutput().resolve(packageName.replace(".", File.separator));
        if (!Files.exists(testSourcePath)) {
            return false;
        }
        // list the java files and filter the file name start with sourceClassName in directory testPath
        List<String> testNames= findTestByClassName(sourceClassName, testSourcePath);
        if (testNames.isEmpty()) {
            return false;
        }

        CompilationUnit cu = new CompilationUnit();
        cu.setPackageDeclaration(packageName);
        cu.addImport("org.junit.runner.RunWith");
        cu.addImport("org.junit.platform.runner.JUnitPlatform");
        cu.addImport("org.junit.platform.suite.api.SelectClasses");
        ClassOrInterfaceDeclaration classNode = cu.addClass(targetClassName);
        classNode.setModifier(Modifier.Keyword.PUBLIC, true);

        // 为类添加@RunWith注解
        NormalAnnotationExpr runWithAnnotation = new NormalAnnotationExpr();
        runWithAnnotation.setName("RunWith");
        runWithAnnotation.addPair("value", new NameExpr("JUnitPlatform.class"));
        classNode.addAnnotation(runWithAnnotation);

        // 为类添加@SelectClasses注解，并设置其值
        NormalAnnotationExpr selectClassesAnnotation = new NormalAnnotationExpr();
        selectClassesAnnotation.setName("SelectClasses");
        ArrayInitializerExpr array = new ArrayInitializerExpr();
        testNames.forEach(testName -> {
            array.getValues().add(new NameExpr(testName.replace(".java", ".class")));
        });
        MemberValuePair pair = new MemberValuePair("value", array);
        selectClassesAnnotation.getPairs().add(pair);
        classNode.addAnnotation(selectClassesAnnotation);

        deleteRepeatTestFile(Collections.singletonList(targetClassName));

        return export(cu.toString());
    }

    private static List<String> findTestByClassName(String className, Path testSourcePath) throws IOException {
        List<String> testNames = new ArrayList<>();
        Files.list(testSourcePath).forEach(path -> {
            String name = path.getFileName().toString();
            if (name.endsWith(".java") && name.startsWith(className + "_")) {
                testNames.add(name);
            }
        });
        return testNames;
    }

    private void deleteRepeatTestFile(List<String> classNameToDel) {
        List<String> classPaths = new ArrayList<>();
        ProjectParser.scanSourceDirectory(config.project, classPaths);
        classPaths.forEach(classPath -> {
            String className = classPath.substring(classPath.lastIndexOf(File.separator) + 1, classPath.lastIndexOf("."));
            if(classNameToDel.contains(className)) {
                File classFile = new File(classPath);
                classFile.delete();
            }
        });
    }

    public boolean export(String code) {
        Path testSourcePath = config.getTestOutput().resolve(packageName.replace(".", File.separator));
        Path savePath = testSourcePath.resolve(targetClassName + ".java");
        AbstractRunner.exportTest(code, savePath);
        return true;
    }

}
