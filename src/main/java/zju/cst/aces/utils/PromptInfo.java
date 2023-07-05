package zju.cst.aces.utils;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class PromptInfo {
    public boolean hasDep;
    public String className;
    public String methodName;
    public String methodSignature;
    public String methodCode;
    public String info;
    public List<Map<String, String>> classDeps = new ArrayList<>();
    public List<Map<String, String>> methodDeps = new ArrayList<>();
    public List<String> errorMsg = null;
    public String unitTest = "";

    public PromptInfo(boolean hasDep, String className, String methodName,
                      String methodSignature, String methodCode) {
        this.hasDep = hasDep;
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.methodCode = methodCode;
    }

    public void addMethodDeps(Map<String, String> methodDep) {
        this.methodDeps.add(methodDep);
    }

    public void addClassDeps(Map<String, String> classDep) {
        this.classDeps.add(classDep);
    }
}
