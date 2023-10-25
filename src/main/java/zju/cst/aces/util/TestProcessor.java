package zju.cst.aces.util;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import zju.cst.aces.dto.PromptInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName TestProcessor
 * @Description Process generated tests, remove the error test case in the test class
 */

//TODO: remove correct test case in the repair prompt.
public class TestProcessor {
    private static final JavaParser parser = new JavaParser();
    private String fullTestName;

    public TestProcessor(String fullTestName) {
        this.fullTestName = fullTestName;
    }

    public List<Integer> getErrorLineNum(TestExecutionSummary summary) {
        List<Integer> errorLineNum = new ArrayList<>();
        summary.getFailures().forEach(failure -> {
            for (StackTraceElement st : failure.getException().getStackTrace()) {
                if (st.getClassName().contains(fullTestName)) {
                    int lineNum = st.getLineNumber();
                    errorLineNum.add(lineNum);
                }
            }
        });
        return errorLineNum;
    }

    public boolean containError(List<Integer> errorLineNum, MethodDeclaration method) {
        int beginPosition = method.getBegin().get().line;
        int endPosition = method.getEnd().get().line;
        return !errorLineNum.stream().filter(lineNum -> lineNum >= beginPosition && lineNum <= endPosition)
                .collect(Collectors.toList()).isEmpty();
    }

    public boolean isTestCase(MethodDeclaration method) {
        return !method.getAnnotations().stream().filter(annotationExpr -> {
            String annotationName = annotationExpr.getNameAsString();
            return annotationName.equals("Test") || annotationName.equals("ParameterizedTest");
        }).collect(Collectors.toList()).isEmpty();
    }

    public String removeErrorTest(PromptInfo promptInfo, TestExecutionSummary summary) {
        String result = promptInfo.getUnitTest();
        try {
            ParseResult<CompilationUnit> parseResult = parser.parse(result);
            CompilationUnit cu = parseResult.getResult().orElseThrow();
            List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
            List<Integer> errorLineNum = getErrorLineNum(summary);
            for (MethodDeclaration method : methods) {
                //TODO: remove error test case and other methods with errors
                if (containError(errorLineNum, method)) {
                    method.remove();
                }
            }
            if (cu.findAll(MethodDeclaration.class).stream().filter(this::isTestCase).collect(Collectors.toList()).isEmpty()) {
                return null;
            }
            result = cu.toString();
        } catch (Exception e) {
            System.out.println("In TestProcessor.removeErrorTest: " + e);
            return null;
        }
        return result;
    }

    public String removeCorrectTest(PromptInfo promptInfo, TestExecutionSummary summary) {
        String result = promptInfo.getUnitTest();
        try {
            ParseResult<CompilationUnit> parseResult = parser.parse(result);
            CompilationUnit cu = parseResult.getResult().orElseThrow();
            List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
            List<Integer> errorLineNum = getErrorLineNum(summary);
            for (MethodDeclaration method : methods) {
                if (!containError(errorLineNum, method) && isTestCase(method)) {
                    promptInfo.addCorrectTest(method);
                    method.remove();
                }
            }
            result = cu.toString();
            if (cu.findAll(MethodDeclaration.class).stream().filter(this::isTestCase).collect(Collectors.toList()).isEmpty()) {
//                throw new Exception("In TestProcessor.removeCorrectTest: No test case left");
                System.out.println("In TestProcessor.removeCorrectTest: No test case left");
            }
        } catch (Exception e) {
            System.out.println("In TestProcessor.removeCorrectTest: " + e);
        }
        promptInfo.setUnitTest(result);
        return result;
    }

    public String addCorrectTest(PromptInfo promptInfo) {
        String result = promptInfo.getUnitTest();
        try {
            ParseResult<CompilationUnit> parseResult = parser.parse(result);
            CompilationUnit cu = parseResult.getResult().orElseThrow();
            promptInfo.getCorrectTests().keySet().forEach(className -> {
                cu.getClassByName(className).ifPresent(classOrInterfaceDeclaration -> {
                    promptInfo.getCorrectTests().get(className).forEach(methodDeclaration -> {
                        classOrInterfaceDeclaration.addMember(methodDeclaration);
                    });
                });
            });
            result = cu.toString();
        } catch (Exception e) {
            System.out.println("In TestProcessor.addCorrectTest: " + e);
        }
        promptInfo.setUnitTest(result);
        return result;
    }
}
