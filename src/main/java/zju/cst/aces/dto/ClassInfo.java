package zju.cst.aces.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassInfo {
    public String className;
    public int index;
    public String modifier;
    public String extend;
    public String implement;
    public String packageDeclaration;
    public String classSignature;
    public List<String> imports;
    public List<String> fields;
    public List<String> superClasses;
    public Map<String, String> methodSigs;
    public List<String> methodsBrief;
    public boolean hasConstructor;
    public List<String> constructorSigs;
    public List<String> constructorBrief;
    public List<String> getterSetterSigs;
    public List<String> getterSetterBrief;
    public Map<String, Set<String>> constructorDeps;

    public ClassInfo(String className, int index, String modifier, String extend, String implement,
                     String packageDeclaration, String classSignature, List<String> imports,
                     List<String> fields, List<String> superClasses, Map<String, String> methodSigs,
                     List<String> methodsBrief, boolean hasConstructor, List<String> constructorSigs,
                     List<String> constructorBrief, List<String> getterSetterSigs, List<String> getterSetterBrief, Map<String, Set<String>> constructorDeps) {
        this.className = className;
        this.index = index;
        this.modifier = modifier;
        this.extend = extend;
        this.implement = implement;
        this.packageDeclaration = packageDeclaration;
        this.classSignature = classSignature;
        this.imports = imports;
        this.fields = fields;
        this.superClasses = superClasses;
        this.methodSigs = methodSigs;
        this.methodsBrief = methodsBrief;
        this.hasConstructor = hasConstructor;
        this.constructorSigs = constructorSigs;
        this.constructorBrief = constructorBrief;
        this.getterSetterSigs = getterSetterSigs;
        this.getterSetterBrief = getterSetterBrief;
        this.constructorDeps = constructorDeps;
    }
}
