package zju.cst.aces.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MethodInfo {
    public String className;
    public String methodName;
    public String brief;
    public String methodSignature;
    public String sourceCode;
    public boolean isConstructor;
    public boolean useField;
    public boolean isGetSet;
    public boolean isPublic;
    public List<String> parameters;
    public Map<String, Set<String>> dependentMethods;

    public MethodInfo(String className, String methodName, String brief, String methodSignature, String sourceCode,
                      boolean isConstructor, boolean useField, boolean isGetSet, boolean isPublic,
                      List<String> parameters, Map<String, Set<String>> dependentMethods) {
        this.className = className;
        this.methodName = methodName;
        this.brief = brief;
        this.methodSignature = methodSignature;
        this.sourceCode = sourceCode;
        this.isConstructor = isConstructor;
        this.useField = useField;
        this.isGetSet = isGetSet;
        this.isPublic = isPublic;
        this.parameters = parameters;
        this.dependentMethods = dependentMethods;
    }
}
