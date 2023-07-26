package zju.cst.aces.util;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName TestProcessor
 * @Description Process generated tests, remove the error test case in the test class
 */
public class TestProcessor {
    private static final JavaParser parser = new JavaParser();
    private String fullTestName;
    private TestExecutionSummary summary;
    private String uniTest;

    public TestProcessor(String fullTestName, TestExecutionSummary summary, String uniTest) {
        this.fullTestName = fullTestName;
        this.summary = summary;
        this.uniTest = uniTest;
    }

    public List<Integer> getErrorLineNum() {
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

    public String removeErrorTest() {
        try {
            ParseResult<CompilationUnit> parseResult = parser.parse(uniTest);
            CompilationUnit cu = parseResult.getResult().orElseThrow();
            List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
            List<Integer> errorLineNum = getErrorLineNum();
            for (MethodDeclaration method : methods) {
                //TODO: remove error test case and other methods with errors
                if (containError(errorLineNum, method)) {
                    method.remove();
                }
            }
            if (cu.findAll(MethodDeclaration.class).stream().filter(this::isTestCase).collect(Collectors.toList()).isEmpty()) {
                return null;
            }
            this.uniTest = cu.toString();
        } catch (Exception e) {
            System.out.println("In TestProcessor.removeErrorTest: " + e);
        }
        return this.uniTest;
    }
}
