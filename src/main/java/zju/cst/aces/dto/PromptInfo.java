package zju.cst.aces.dto;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.Data;

import java.nio.file.Path;
import java.util.*;

@Data
public class PromptInfo {
    public boolean hasDep;
    public String className;
    public String methodName;
    public String methodSignature;
    public String info; // move other methods and constructors to otherMethods.
    public String otherMethodBrief;
    public String otherMethodBodies;
    //TODO: do not need use Set
    public Set<Map<String, String>> constructorDeps = new HashSet<>(); // dependent classes in constructor.
    public Set<Map<String, String>> methodDeps = new HashSet<>(); // dependent classes in method parameters and body.
    public TestMessage errorMsg;
    public String unitTest = "";
    public String fullTestName;
    public Path testPath;
    public MethodInfo methodInfo;
    public ClassInfo classInfo;

    public Map<String, List<MethodDeclaration>> correctTests = new HashMap<>();
    public List<RoundRecord> records = new ArrayList<>();

    public PromptInfo(boolean hasDep, String className, String methodName,
                      String methodSignature) {
        this.hasDep = hasDep;
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
    }

    public PromptInfo(){}

    public PromptInfo(PromptInfo p) {
        this.setHasDep(p.isHasDep());
        this.setClassName(p.getClassName());
        this.setMethodName(p.getMethodName());
        this.setMethodSignature(p.getMethodSignature());
        this.setInfo(p.getInfo());
        this.setOtherMethodBrief(p.getOtherMethodBrief());
        this.setConstructorDeps(p.getConstructorDeps());
        this.setMethodDeps(p.getMethodDeps());
        this.setErrorMsg(p.getErrorMsg());
        this.setUnitTest(p.getUnitTest());
        this.setFullTestName(p.getFullTestName());
        this.setTestPath(p.getTestPath());
        this.setCorrectTests(p.getCorrectTests());
        this.setRecords(p.getRecords());
    }

    public void addMethodDeps(Map<String, String> methodDep) {
        if (methodDep == null) {
            return;
        }
        this.methodDeps.add(methodDep);
    }

    public void addConstructorDeps(Map<String, String> constructorDep) {
        if (constructorDep == null) {
            return;
        }
        this.constructorDeps.add(constructorDep);
    }

    public void addCorrectTest(MethodDeclaration m) {
        ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) m.getParentNode().orElseThrow();
        String className = c.getNameAsString();
        if (this.correctTests.containsKey(className)) {
            this.correctTests.get(className).add(m);
            return;
        } else {
            List<MethodDeclaration> methods = new ArrayList<>();
            methods.add(m);
            this.correctTests.put(className, methods);
        }
    }

    public void addRecord(RoundRecord r) {
        this.records.add(r);
    }
}
