package zju.cst.aces.dto;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class ClassInfo {
    public String className;
    public int index;
    public String modifier;
    public String extend;
    public String implement;
    public String packageName;
    public String packageDeclaration;
    public String classSignature;
    public boolean hasConstructor;
    public boolean isPublic;
    public boolean isInterface;
    public boolean isAbstract;
    public List<String> imports;
    public List<String> fields;
    public List<String> superClasses;
    public Map<String, String> methodSigs;
    public List<String> methodsBrief;
    public List<String> constructorSigs;
    public List<String> constructorBrief;
    public List<String> getterSetterSigs;
    public List<String> getterSetterBrief;
    public Map<String, Set<String>> constructorDeps;
    public String compilationUnitCode;
    public String classDeclarationCode;

    public ClassInfo(CompilationUnit cu, ClassOrInterfaceDeclaration classNode, int index, String classSignature,
                     List<String> imports, List<String> fields, List<String> superClasses, Map<String, String> methodSigs,
                     List<String> methodsBrief, boolean hasConstructor, List<String> constructorSigs,
                     List<String> constructorBrief, List<String> getterSetterSigs, List<String> getterSetterBrief, Map<String, Set<String>> constructorDeps) {
        this.className = classNode.getNameAsString();
        this.index = index;
        this.modifier = classNode.getModifiers().toString();
        this.extend = classNode.getExtendedTypes().toString();
        this.implement = classNode.getImplementedTypes().toString();
        this.packageName = cu.getPackageDeclaration().orElse(null) == null ? "" : cu.getPackageDeclaration().get().getNameAsString();
        this.packageDeclaration = getPackageDeclaration(cu);
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

    public void setCode(String compilationUnitCode, String classDeclarationCode) {
        this.compilationUnitCode = compilationUnitCode;
        this.classDeclarationCode = classDeclarationCode;
    }

    private String getPackageDeclaration(CompilationUnit compilationUnit) {
        if (compilationUnit.getPackageDeclaration().isPresent()) {
            return compilationUnit.getPackageDeclaration().get().toString().trim();
        } else {
            return "";
        }
    }
}
