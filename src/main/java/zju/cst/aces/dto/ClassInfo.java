package zju.cst.aces.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassInfo {
    public String className;
    public String packageDeclaration;
    public String classSignature;
    public List<String> imports;
    public List<String> fields;
    public List<String> superClasses;
    public Map<String, String> methodSignatures;
    public List<String> briefMethods;
    public boolean hasConstructor;
    public List<String> constructors;
    public List<String> getterSetters;
    public Map<String, Set<String>> constructorDeps;

    public ClassInfo(String className, String packageDeclaration, String classSignature, List<String> imports,
                     List<String> fields, List<String> superClasses, Map<String, String> methodSignatures,
                     List<String> briefMethods, boolean hasConstructor, List<String> constructors,
                     List<String> getterSetters, Map<String, Set<String>> constructorDeps) {
        this.className = className;
        this.packageDeclaration = packageDeclaration;
        this.classSignature = classSignature;
        this.imports = imports;
        this.fields = fields;
        this.superClasses = superClasses;
        this.methodSignatures = methodSignatures;
        this.briefMethods = briefMethods;
        this.hasConstructor = hasConstructor;
        this.constructors = constructors;
        this.getterSetters = getterSetters;
        this.constructorDeps = constructorDeps;
    }
}
