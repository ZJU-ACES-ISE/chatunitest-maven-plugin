package zju.cst.aces.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import zju.cst.aces.config.Config;
import zju.cst.aces.dto.ClassInfo;
import zju.cst.aces.dto.MethodInfo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassParser {
    private static final String separator = "_";
    private static Path classOutputPath;
    private static ClassInfo classInfo;
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static JavaParser parser;
    public int methodCount = 0;
    private final Config config;

    public ClassParser(Config config, String path) {
        this.config = config;
        this.parser = config.getParser();
        setOutputPath(path);
    }

    public ClassParser(Config config, Path path) {
        this.config = config;
        this.parser = config.getParser();
        setOutputPath(path.toString());
    }

    public void extractClass(String classPath) throws FileNotFoundException {
        File file = new File(classPath);
        ParseResult<CompilationUnit> parseResult = parser.parse(file);
        CompilationUnit cu = parseResult.getResult().orElseThrow();
        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
        for (ClassOrInterfaceDeclaration classDeclaration : classes) {
            try {
                classInfo = getInfoByClass(cu, classDeclaration);
                exportClassInfo(classInfo, classDeclaration);
                extractMethods(cu, classDeclaration);
                extractConstructors(cu, classDeclaration);

                addClassMapping(classInfo);
                methodCount += classDeclaration.getMethods().size();
            } catch (Exception e) {
                config.getLog().error("In ClassParser.extractClass Exception: when parse class " + classDeclaration.getNameAsString() + " :\n" + e);
            }
        }
    }

    private static boolean isJavaSourceDir(Path path) {
        return Files.isDirectory(path) && Files.exists(path.resolve(
                "src" + File.separator + "main" + File.separator + "java"));
    }

    private void setOutputPath(String path) {
        classOutputPath = Paths.get(path);
    }

    private void extractMethods(CompilationUnit cu, ClassOrInterfaceDeclaration classDeclaration) throws IOException {
        List<MethodDeclaration> methods = classDeclaration.getMethods();
        for (MethodDeclaration m : methods) {
            MethodInfo info = getInfoByMethod(cu, classDeclaration, m);
            exportMethodInfo(info, classDeclaration, m);
        }
    }

    private void extractConstructors(CompilationUnit cu, ClassOrInterfaceDeclaration classDeclaration) throws IOException {
        List<ConstructorDeclaration> constructors = classDeclaration.getConstructors();
        for (ConstructorDeclaration c : constructors) {
            MethodInfo info = getInfoByMethod(cu, classDeclaration, c);
            exportConstructorInfo(info, classDeclaration, c);
        }
    }

    /**
     * Extract class information to json format
     */
    private ClassInfo getInfoByClass(CompilationUnit cu, ClassOrInterfaceDeclaration classNode) {
        ClassInfo ci = new ClassInfo(
                cu,
                classNode,
                config.sharedInteger.getAndIncrement(),
                getClassSignature(cu, classNode),
                getImports(getImportDeclarations(cu)),
                getFields(cu, classNode.getFields()),
                getSuperClasses(classNode),
                getMethodSignatures(classNode),
                getBriefMethods(cu, classNode),
                hasConstructors(classNode),
                getConstructorSignatures(classNode),
                getBriefConstructors(cu, classNode),
                getGetterSetterSig(cu, classNode),
                getGetterSetter(cu, classNode),
                getConstructorDeps(cu, classNode));

        ci.setPublic(classNode.isPublic());
        ci.setAbstract(classNode.isAbstract());
        ci.setInterface(classNode.isInterface());
        ci.setCode(cu.toString(), classNode.toString());
        return ci;
    }

    /**
     * Generate extracted information of focal method(constructor).
     */
    private MethodInfo getInfoByMethod(CompilationUnit cu, ClassOrInterfaceDeclaration classNode, CallableDeclaration node) {
        MethodInfo mi = new MethodInfo(
                classNode.getNameAsString(),
                node.getNameAsString(),
                getBriefMethod(cu, node),
                getMethodSig(node),
                getMethodCode(cu, node),
                getParameters(node),
                getDependentMethods(cu, node));

        mi.setUseField(useField(node));
        mi.setConstructor(node.isConstructorDeclaration());
        mi.setGetSet(isGetSet2(node));
        mi.setPublic(isPublic(node));
        mi.setBoolean(isBoolean(node));
        mi.setAbstract(node.isAbstract());
        return mi;
    }

    private Map<String, Set<String>> getConstructorDeps(CompilationUnit cu, ClassOrInterfaceDeclaration classNode) {
        Map<String, Set<String>> constructorDeps = new HashMap<>();
        for (ConstructorDeclaration c : classNode.getConstructors()) {
            Map<String, Set<String>> tmp = getDependentMethods(cu, c);
            for (String key : tmp.keySet()) {
                // Do not need method dependency
                if (constructorDeps.containsKey(key)) {
                    continue;
                } else {
                    constructorDeps.put(key, tmp.get(key));
                }
            }
        }
        return constructorDeps;
    }

    private List<String> getGetterSetter(CompilationUnit cu, ClassOrInterfaceDeclaration classNode) {
        List<String> getterSetter = new ArrayList<>();
        for (MethodDeclaration m : classNode.getMethods()) {
            if (isGetSet2(m)) {
                getterSetter.add(getBriefMethod(cu, m));
            }
        }
        return getterSetter;
    }

    private List<String> getGetterSetterSig(CompilationUnit cu, ClassOrInterfaceDeclaration classNode) {
        List<String> getterSetter = new ArrayList<>();
        for (MethodDeclaration m : classNode.getMethods()) {
            if (isGetSet2(m)) {
                getterSetter.add(m.getSignature().asString());
            }
        }
        return getterSetter;
    }

    /**
     * Get method signature (the parameters in signature are qualified name)
     */
    private String getMethodSig(CallableDeclaration node) {
        if (node instanceof MethodDeclaration) {
            return node.getSignature().asString();
        } else {
            return node.getSignature().asString();
        }
    }

    private List<ImportDeclaration> getImportDeclarations(CompilationUnit compilationUnit) {
        return compilationUnit.getImports();
    }

    /**
     * get String format imports by imports declaration, each import declaration is a line
     */
    private List<String> getImports(List<ImportDeclaration> importDeclarations) {
        List<String> imports = new ArrayList<>();
        for (ImportDeclaration i : importDeclarations) {
            imports.add(i.toString().trim());
        }
        return imports;
    }

    private boolean hasConstructors(ClassOrInterfaceDeclaration classNode) {
        return classNode.getConstructors().size() > 0;
    }

    private List<String> getSuperClasses(ClassOrInterfaceDeclaration node) {
        List<String> superClasses = new ArrayList<>();
        node.getExtendedTypes().forEach(sup -> {
            superClasses.add(sup.getNameAsString());
        });
        return superClasses;
    }

    /**
     * Get the map of all method signatures and id in class
     */
    private Map<String, String> getMethodSignatures(ClassOrInterfaceDeclaration node) {
        Map<String, String> mSigs = new HashMap<>();
        List<MethodDeclaration> methods = node.getMethods();
        int i = 0;
        for (; i < methods.size(); i++) {
            try {
                mSigs.put(methods.get(i).getSignature().asString(), String.valueOf(i));
            } catch (Exception e) {
                throw new RuntimeException("In ClassParser getMethodSignatures: when resolve method: " + methods.get(i).getNameAsString() + ": " + e);
            }
        }
        List<ConstructorDeclaration> constructors = node.getConstructors();
        for (; i < methods.size() + constructors.size(); i++) {
            mSigs.put(constructors.get(i - methods.size()).getSignature().asString(), String.valueOf(i));
        }
        return mSigs;
    }

    private List<String> getConstructorSignatures(ClassOrInterfaceDeclaration node) {
        List<String> cSigs = new ArrayList<>();
        node.getConstructors().forEach(c -> {
            cSigs.add(c.getSignature().asString());
        });
        return cSigs;
    }

    private List<String> getBriefConstructors(CompilationUnit cu, ClassOrInterfaceDeclaration node) {
        List<ConstructorDeclaration> constructors = node.getConstructors();
        List<String> cSigs = new ArrayList<>();
        for (ConstructorDeclaration c : constructors) {
            cSigs.add(getBriefMethod(cu, c));
        }
        return cSigs;
    }


    /**
     * Get all brief method in class
     */
    private List<String> getBriefMethods(CompilationUnit cu, ClassOrInterfaceDeclaration node) {
        List<String> mSigs = new ArrayList<>();
        node.getMethods().forEach(m -> {
            mSigs.add(getBriefMethod(cu, m));
        });
        node.getConstructors().forEach(c -> {
            mSigs.add(getBriefMethod(cu, c));
        });
        return mSigs;
    }

    /**
     * Get brief method(construcor)
     * Note:
     * Get source code from begin of method to begin of body
     */
    private String getBriefMethod(CompilationUnit cu, CallableDeclaration node) {
        String sig = "";
        if (node instanceof MethodDeclaration) {
            MethodDeclaration methodNode = (MethodDeclaration) node;
            if (methodNode.getBody().isPresent()) {
                sig = getSourceCodeByPosition(getTokenString(cu),
                        methodNode.getBegin().orElseThrow(), methodNode.getBody().get().getBegin().orElseThrow());
                sig = sig.substring(0, sig.lastIndexOf("{") - 1) + "{}";
            } else {
                sig = getSourceCodeByPosition(getTokenString(cu),
                        methodNode.getBegin().orElseThrow(), methodNode.getEnd().orElseThrow());
            }
        } else if (node instanceof ConstructorDeclaration) {
            ConstructorDeclaration constructorNode = (ConstructorDeclaration) node.removeComment();
            sig = getSourceCodeByPosition(getTokenString(cu),
                    constructorNode.getBegin().orElseThrow(), constructorNode.getBody().getBegin().orElseThrow());
            sig = sig.substring(0, sig.lastIndexOf("{") - 1) + "{}";
        }
        return sig;
    }

    /**
     * Get class signature
     */
    public static String getClassSignature(CompilationUnit cu, ClassOrInterfaceDeclaration node) {
        return getSourceCodeByPosition(getTokenString(cu), node.getBegin().orElseThrow(), node.getName().getEnd().orElseThrow());
    }

    /**
     * Get method(constructor) source code start from the first modifier to the end of the node.
     */
    private String getMethodCode(CompilationUnit cu, CallableDeclaration node) {
        return node.getTokenRange().orElseThrow().toString();
    }

    /**
     * Get full fields declaration of.
     */
    private List<String> getFields(CompilationUnit cu, List<FieldDeclaration> nodes) {
        List<String> fields = new ArrayList<>();
        for (FieldDeclaration f : nodes) {
            fields.add(getFieldCode(cu, f));
        }
        return fields;
    }

    /**
     * Get field source code start from the first modifier to the end of the node
     */
    private String getFieldCode(CompilationUnit cu, FieldDeclaration node) {
        return node.getTokenRange().orElseThrow().toString();
    }

    /**
     * Whether the method uses a field
     */
    private boolean useField(CallableDeclaration node) {
        return node.findAll(FieldAccessExpr.class).size() > 0;
    }

    /**
     * Whether the method is a getter or setter (assume the getter and setter access the field by "this")
     */
    private boolean isGetSet(CallableDeclaration node) {
        if (node.isConstructorDeclaration()) {
            return false;
        }
        if (!node.getNameAsString().startsWith("get") && !node.getNameAsString().startsWith("set")) {
            return false;
        }

        List<FieldAccessExpr> fieldAccesses = node.findAll(FieldAccessExpr.class);
        for (FieldAccessExpr fa : fieldAccesses) {
            // getter: return field
            if (fa.getParentNode().orElse(null) instanceof ReturnStmt) {
                return true;
            }
            // setter: assign field
            if (fa.getParentNode().orElse(null) instanceof AssignExpr && ((AssignExpr) fa.getParentNode().orElseThrow()).getTarget().equals(fa)) {
                return true;
            }
        }
        return false;
    }

    private boolean isGetSet2(CallableDeclaration node) {
        if (node.isConstructorDeclaration()) {
            return false;
        }
        if (node.getNameAsString().startsWith("get") && node.getParameters().size() == 0) {
            return true;
        }
        if (node.getNameAsString().startsWith("set")) {
            return true;
        }
        return false;
    }

    private boolean isPublic(CallableDeclaration node) {
        return node.isPublic();
    }

    private boolean isBoolean(CallableDeclaration node) {
        if (node.isConstructorDeclaration()) {
            return false;
        }
        if (node.getNameAsString().startsWith("is")
                && node.getParameters().size() == 0
                && node.asMethodDeclaration().getTypeAsString().equals("boolean")) {
            return true;
        }
        return false;
    }

    /**
     * Get method parameters
     */
    private List<String> getParameters(CallableDeclaration node) {
        List<String> parameters = new ArrayList<>();
        node.getParameters().forEach(p -> {
            parameters.add(((Parameter) p).getType().asString());
        });
        return parameters;
    }

    private Map<String, Set<String>> getDependentMethods(CompilationUnit cu, CallableDeclaration node) {
        Map<String, Set<String>> dependentMethods = new HashMap<>();
        List<MethodCallExpr> methodCalls = node.findAll(MethodCallExpr.class);
        List<Parameter> pars = node.getParameters();

        for (Parameter p : pars) {
            try {
                if (p.getType().isPrimitiveType()) {
                    continue;
                }
                if (p.getType().isArrayType()) {
                    String dependentType = p.resolve().getType().asArrayType().getComponentType().describe();
                    dependentMethods.put(dependentType, new HashSet<String>());
                    continue;
                } else {
                    String dependentType = p.resolve().describeType();
                    dependentMethods.put(dependentType, new HashSet<String>());
                }
            } catch (Exception e) {

            }
        }
        for (MethodCallExpr m : methodCalls) {
            try {
                ResolvedMethodDeclaration md = m.resolve();
                String dependentType = md.declaringType().getQualifiedName();
                String mSig = getParamTypeInSig(md); // change parameters' type to non-qualified name
                Set<String> invocations = dependentMethods.get(dependentType);
                if (invocations == null) {
                    invocations = new HashSet<>();
                }
                invocations.add(mSig);
                dependentMethods.put(dependentType, invocations);
            } catch (Exception e) {
//                config.getLog().warn("Cannot resolve method call: " + m.getNameAsString() + " in: " + node.getNameAsString());
            }
        }
        return dependentMethods;
    }

    private static String getParamTypeInSig(ResolvedMethodDeclaration md) {
        String sig = md.getName() + "(";
        for (int i = 0; i < md.getNumberOfParams(); i++) {
            String paramType = md.getParam(i).getType().erasure().describe();
            if (paramType.contains(".")) {
                paramType = paramType.substring(paramType.lastIndexOf(".") + 1);
            }
            if (i == md.getNumberOfParams() - 1) {
                sig += paramType;
            } else {
                sig += paramType + ", ";
            }
        }
        sig += ")";
        return sig;
    }

    public String getLastType(String type) {
        return type.substring(type.lastIndexOf(".") + 1);
    }

    private static String getSourceCodeByPosition(String code, Position begin, Position end) {
        String[] lines = code.split("\\n");
        StringBuilder sb = new StringBuilder();

        for (int i = begin.line - 1; i < end.line; i++) {
            if (i == begin.line - 1 && i == end.line - 1) {
                // The range is within a single line
                sb.append(lines[i].substring(begin.column - 1, end.column));
            } else if (i == begin.line - 1) {
                // The first line of the range
                sb.append(lines[i].substring(begin.column - 1));
            } else if (i == end.line - 1) {
                // The last line of the range
                sb.append(lines[i].substring(0, end.column));
            } else {
                // A middle line in the range
                sb.append(lines[i]);
            }

            // Add line breaks except for the last line
            if (i < end.line - 1) {
                sb.append(System.lineSeparator());
            }
        }

        return sb.toString();
    }

    private static String getTokenString(@NotNull Node node) {
        if (node.getTokenRange().isPresent()) {
            return node.getTokenRange().get().toString();
        } else {
            return "";
        }
    }

    private void exportClassInfo(ClassInfo classInfo, ClassOrInterfaceDeclaration classNode) throws IOException {
        Path classOutputDir = classOutputPath.resolve(classNode.getName().getIdentifier());
        if (!Files.exists(classOutputDir)) {
            Files.createDirectories(classOutputDir);
        }
        Path classInfoPath = classOutputDir.resolve("class.json");
        //set charset utf-8
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(classInfoPath.toFile()), StandardCharsets.UTF_8)){
            writer.write(GSON.toJson(classInfo));
        }
    }

    private void exportMethodInfo(MethodInfo methodInfo, ClassOrInterfaceDeclaration classNode, MethodDeclaration node) throws IOException {
        Path classOutputDir = classOutputPath.resolve(classNode.getName().getIdentifier());
        if (!Files.exists(classOutputDir)) {
            Files.createDirectories(classOutputDir);
        }
        Path info = classOutputDir.resolve(getFilePathBySig(node.getSignature().asString()));
        //set charset utf-8
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(info.toFile()),StandardCharsets.UTF_8)){
            writer.write(GSON.toJson(methodInfo));
        }
    }

    private void exportConstructorInfo(MethodInfo methodInfo, ClassOrInterfaceDeclaration classNode, ConstructorDeclaration node) throws IOException {
        Path classOutputDir = classOutputPath.resolve(classNode.getName().getIdentifier());
        if (!Files.exists(classOutputDir)) {
            Files.createDirectories(classOutputDir);
        }
        Path info = classOutputDir.resolve(getFilePathBySig(node.getSignature().asString()));
        //set charset utf-8
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(info.toFile()),StandardCharsets.UTF_8)){
            writer.write(GSON.toJson(methodInfo));
        }
    }

    /**
     * Generate a filename for the focal method json file by method signature.
     */
    private Path getFilePathBySig(String sig) {
        Map<String, String> mSigs = classInfo.methodSigs;
        return Paths.get(mSigs.get(sig) + ".json");
    }

    /**
     * Get the filename of the focal method by finding method name and parameters in mSig.
     */
    public static Path getFilePathBySig(String mSig, ClassInfo info) {
        Map<String, String> mSigs = info.methodSigs;
        return Paths.get(mSigs.get(mSig) + ".json");
    }

    private List<Path> getSources() {
        try (Stream<Path> paths = Files.walk(Path.of(System.getProperty("user.dir")))) {
            return paths
                    .filter(ClassParser::isJavaSourceDir)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("In ClassParser.getSources: " + e);
        }
    }

    public void addClassMapping(ClassInfo classInfo) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("className", classInfo.className);
        map.put("packageDeclaration", classInfo.packageDeclaration);
        map.put("modifier", classInfo.modifier);
        map.put("extend", classInfo.extend);
        map.put("implement", classInfo.implement);
        if (config.classMapping == null) {
            config.classMapping = new LinkedHashMap<>();
        }
        config.classMapping.put("class" + classInfo.index, map);
    }
}
