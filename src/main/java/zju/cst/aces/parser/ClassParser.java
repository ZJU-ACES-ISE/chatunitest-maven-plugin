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
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import zju.cst.aces.utils.ClassInfo;
import zju.cst.aces.utils.MethodInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassParser {
    private static final JavaParser parser = new JavaParser();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String separator = "_";
    private static Path outputPath = null;
    private static ClassInfo classInfo;

    public ClassParser(String path, String depPath) {
        setOutputPath(path);
        JavaSymbolSolver symbolSolver = getSymbolSolver(depPath);
        parser.getParserConfiguration().setSymbolResolver(symbolSolver);
    }

    private JavaSymbolSolver getSymbolSolver(String depPath) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        try {
            // traverse the jarDepPath and add all jar files to type solver
            Files.walk(Paths.get(depPath))
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            combinedTypeSolver.add(new JarTypeSolver(file.toAbsolutePath()));
                        } catch (IOException e) {
                            System.out.println("Error when adding jar file to type solver: " + e);
                        }
                    });
        } catch (IOException e) {
            System.out.println(e);
        }
        for (Path source : getSources()) {
            combinedTypeSolver.add(new JavaParserTypeSolver(source.resolve(
                    "src" + File.separator + "main" + File.separator + "java").toFile()));
        }
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        return symbolSolver;
    }

    private static boolean isJavaSourceDir(Path path) {
        return Files.isDirectory(path) && Files.exists(path.resolve(
                "src" + File.separator + "main" + File.separator + "java"));
    }

    private static void setOutputPath(String path) {
        outputPath = Paths.get(path);
    }

    private static void extractMethods(CompilationUnit cu, ClassOrInterfaceDeclaration classDeclaration) throws IOException {
        List<MethodDeclaration> methods = classDeclaration.getMethods();
        for (MethodDeclaration m : methods) {
            MethodInfo info = getInfoByMethod(cu, classDeclaration, m);
            exportMethodInfo(GSON.toJson(info), classDeclaration, m);
        }
    }

    private static void extractConstructors(CompilationUnit cu, ClassOrInterfaceDeclaration classDeclaration) throws IOException {
        List<ConstructorDeclaration> constructors = classDeclaration.getConstructors();
        for (ConstructorDeclaration c : constructors) {
            MethodInfo info = getInfoByMethod(cu, classDeclaration, c);
            exportConstructorInfo(GSON.toJson(info), classDeclaration, c);
        }
    }

    /**
     * Extract class information to json format
     */
    private static ClassInfo getInfoByClass(CompilationUnit cu, ClassOrInterfaceDeclaration classNode) {
        return new ClassInfo(
                classNode.getNameAsString(),
                getPackageDeclaration(cu),
                getClassSignature(cu, classNode),
                getImports(getImportDeclarations(cu)),
                getFields(cu, classNode.getFields()),
                getSuperClasses(classNode),
                getMethodSignatures(classNode),
                getBriefMethods(cu, classNode),
                hasConstructors(classNode),
                getBriefConstructors(cu, classNode),
                getGetterSetter(cu, classNode));
    }

    /**
     * Generate extracted information of focal method(constructor).
     */
    private static MethodInfo getInfoByMethod(CompilationUnit cu, ClassOrInterfaceDeclaration classNode, CallableDeclaration node) {
        return new MethodInfo(
                classNode.getNameAsString(),
                node.getNameAsString(),
                getBriefMethod(cu, node),
                getMethodSig(node),
                getMethodCode(cu, node),
                node.isConstructorDeclaration(),
                useField(node),
                isGetSet(node),
                isPublic(node),
                getParameters(node),
                getDependentMethods(cu, node));
    }

    private static List<String> getGetterSetter(CompilationUnit cu, ClassOrInterfaceDeclaration classNode) {
        List<String> getterSetter = new ArrayList<>();
        for (MethodDeclaration m : classNode.getMethods()) {
            if (isGetSet(m)) {
                getterSetter.add(getBriefMethod(cu, m));
            }
        }
        return getterSetter;
    }

    /**
     * Get method signature (the parameters in signature are qualified name)
     */
    private static String getMethodSig(CallableDeclaration node) {
        if (node instanceof MethodDeclaration) {
            return ((MethodDeclaration) node).resolve().getSignature();
        } else {
            return ((ConstructorDeclaration) node).resolve().getSignature();
        }
    }

    private static String getPackageDeclaration(CompilationUnit compilationUnit) {
        return compilationUnit.getPackageDeclaration().orElse(null).toString().trim();
    }

    private static List<ImportDeclaration> getImportDeclarations(CompilationUnit compilationUnit) {
        return compilationUnit.getImports();
    }

    /**
     * get String format imports by imports declaration, each import declaration is a line
     */
    private static List<String> getImports(List<ImportDeclaration> importDeclarations) {
        List<String> imports = new ArrayList<>();
        for (ImportDeclaration i : importDeclarations) {
            imports.add(i.toString().trim());
        }
        return imports;
    }

    private static boolean hasConstructors(ClassOrInterfaceDeclaration classNode) {
        return classNode.getConstructors().size() > 0;
    }

    private static List<String> getSuperClasses(ClassOrInterfaceDeclaration node) {
        List<String> superClasses = new ArrayList<>();
        node.getExtendedTypes().forEach(sup -> {
            superClasses.add(sup.getNameAsString());
        });
        return superClasses;
    }

    /**
     * Get the map of all method signatures and id in class
     */
    private static Map<String, String> getMethodSignatures(ClassOrInterfaceDeclaration node) {
        Map<String, String> mSigs = new HashMap<>();
        List<MethodDeclaration> methods = node.getMethods();
        int i = 0;
        for (; i < methods.size(); i++) {
            try {
                mSigs.put(methods.get(i).resolve().getSignature(), String.valueOf(i));
            } catch (Exception e) {
                throw new RuntimeException("Error in get method signature: " + methods.get(i).getNameAsString());
            }
        }
        List<ConstructorDeclaration> constructors = node.getConstructors();
        for (; i < methods.size() + constructors.size(); i++) {
            mSigs.put(constructors.get(i - methods.size()).resolve().getSignature(), String.valueOf(i));
        }
        return mSigs;
    }

    private static List<String> getBriefConstructors(CompilationUnit cu, ClassOrInterfaceDeclaration node) {
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
    private static List<String> getBriefMethods(CompilationUnit cu, ClassOrInterfaceDeclaration node) {
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
     * Get source code by modifiers and body may fail because the method node may have no modifiers or body, may occur errors:
     * no modifiers error: Exception: java.lang.IndexOutOfBoundsException: Index 0 out of bounds for length 0.
     * no body error: Exception: java.util.NoSuchElementException: No value present.
     */
    private static String getBriefMethod(CompilationUnit cu, CallableDeclaration node) {
        String sig = "";
        if (node instanceof MethodDeclaration) {
            MethodDeclaration methodNode = (MethodDeclaration) node;
            if (methodNode.getModifiers().size() > 0) {
                if (methodNode.getBody().isPresent()) {
                    sig = getSourceCodeByPosition(getTokenString(cu),
                            methodNode.getModifiers().get(0).getBegin().orElseThrow(), methodNode.getBody().get().getBegin().orElseThrow());
                    sig = sig.substring(0, sig.lastIndexOf("{") - 1) + ";";
                } else {
                    sig = getSourceCodeByPosition(getTokenString(cu),
                            methodNode.getModifiers().get(0).getBegin().orElseThrow(), methodNode.getEnd().orElseThrow());
                }
            } else {
                if (methodNode.getBody().isPresent()) {
                    sig = getSourceCodeByPosition(getTokenString(cu),
                            methodNode.getType().getBegin().orElseThrow(), methodNode.getBody().get().getBegin().orElseThrow());
                    sig = sig.substring(0, sig.lastIndexOf("{") - 1) + ";";
                } else {
                    sig = getSourceCodeByPosition(getTokenString(cu),
                            methodNode.getType().getBegin().orElseThrow(), methodNode.getEnd().orElseThrow());
                }
            }
        } else if (node instanceof ConstructorDeclaration) {
            ConstructorDeclaration constructorNode = (ConstructorDeclaration) node.removeComment();
            sig = getSourceCodeByPosition(getTokenString(cu),
                    constructorNode.getBegin().orElseThrow(), constructorNode.getBody().getBegin().orElseThrow());
            sig = sig.substring(0, sig.lastIndexOf("{") - 1) + ";";
        }
        return sig;
    }

    /**
     * Get class signature
     */
    private static String getClassSignature(CompilationUnit cu, ClassOrInterfaceDeclaration node) {
        return getSourceCodeByPosition(getTokenString(cu), node.getBegin().orElseThrow(), node.getName().getEnd().orElseThrow());
    }

    /**
     * Get method(constructor) source code start from the first modifier to the end of the node.
     */
    private static String getMethodCode(CompilationUnit cu, CallableDeclaration node) {
        String code = "";
        if (node instanceof MethodDeclaration) {
            MethodDeclaration methodNode = (MethodDeclaration) node;
            if (methodNode.getModifiers().size() > 0) {
                code = getSourceCodeByPosition(getTokenString(cu),
                        methodNode.getModifiers().get(0).getBegin().orElseThrow(), methodNode.getEnd().orElseThrow());
            } else {
                code = getSourceCodeByPosition(getTokenString(cu),
                        methodNode.getType().getBegin().orElseThrow(), methodNode.getEnd().orElseThrow());
            }
        } else if (node instanceof ConstructorDeclaration) {
            ConstructorDeclaration constructorNode = (ConstructorDeclaration) node.removeComment();
            code = getSourceCodeByPosition(getTokenString(cu),
                    constructorNode.getBegin().orElseThrow(), constructorNode.getEnd().orElseThrow());
        }
        return code;
    }

    /**
     * Get full fields declaration of.
     */
    private static List<String> getFields(CompilationUnit cu, List<FieldDeclaration> nodes) {
        List<String> fields = new ArrayList<>();
        for (FieldDeclaration f : nodes) {
            fields.add(getFieldCode(cu, f));
        }
        return fields;
    }

    /**
     * Get field source code start from the first modifier to the end of the node
     */
    private static String getFieldCode(CompilationUnit cu, FieldDeclaration node) {
        return getSourceCodeByPosition(getTokenString(cu),
                node.getModifiers().get(0).getBegin().orElseThrow(), node.getEnd().orElseThrow());
    }

    /**
     * Whether the method uses a field
     */
    private static boolean useField(CallableDeclaration node) {
        return node.findAll(FieldAccessExpr.class).size() > 0;
    }

    /**
     * Whether the method is a getter or setter (assume the getter and setter access the field by "this")
     */
    private static boolean isGetSet(CallableDeclaration node) {
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

    private static boolean isPublic(CallableDeclaration node) {
        return node.isPublic();
    }

    /**
     * Get method parameters
     */
    private static List<String> getParameters(CallableDeclaration node) {
        List<String> parameters = new ArrayList<>();
        node.getParameters().forEach(p -> {
            parameters.add(((Parameter) p).getType().asString());
        });
        return parameters;
    }

    private static Map<String, Set<String>> getDependentMethods(CompilationUnit cu, CallableDeclaration node) {
        Map<String, Set<String>> dependentMethods = new HashMap<>();
        List<MethodCallExpr> methodCalls = node.findAll(MethodCallExpr.class);

        for (MethodCallExpr m : methodCalls) {
            try {
                ResolvedMethodDeclaration md = m.resolve();
                String dependentType = md.getClassName();
                String mSig = md.getSignature();
                Set<String> invocations = dependentMethods.get(dependentType);
                if (invocations == null) {
                    invocations = new HashSet<>();
                }
                invocations.add(mSig);
                dependentMethods.put(dependentType, invocations);
            } catch (Exception e) {
                // TODO: handle the methods generated by annotations
            }
        }
        return dependentMethods;
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
        return node.getTokenRange().orElse(null).toString();
    }

    private static void exportClassInfo(String json, ClassOrInterfaceDeclaration classNode) throws IOException {
        Path classOutputDir = outputPath.resolve(classNode.getName().getIdentifier());
        if (!Files.exists(classOutputDir)) {
            Files.createDirectories(classOutputDir);
        }
        Path classInfoPath = classOutputDir.resolve("class.json");
        try (FileWriter writer = new FileWriter(classInfoPath.toFile())) {
            writer.write(json);
        }
    }

    private static void exportMethodInfo(String json, ClassOrInterfaceDeclaration classNode, MethodDeclaration node) throws IOException {
        Path classOutputDir = outputPath.resolve(classNode.getName().getIdentifier());
        if (!Files.exists(classOutputDir)) {
            Files.createDirectories(classOutputDir);
        }
        Path info = classOutputDir.resolve(getFilePathBySig(node.resolve().getSignature()));
        try (FileWriter writer = new FileWriter(info.toFile())) {
            writer.write(json);
        }
    }

    private static void exportConstructorInfo(String json, ClassOrInterfaceDeclaration classNode, ConstructorDeclaration node) throws IOException {
        Path classOutputDir = outputPath.resolve(classNode.getName().getIdentifier());
        if (!Files.exists(classOutputDir)) {
            Files.createDirectories(classOutputDir);
        }
        Path info = classOutputDir.resolve(getFilePathBySig(node.resolve().getSignature()));
        try (FileWriter writer = new FileWriter(info.toFile())) {
            writer.write(json);
        }
    }

    /**
     * Generate a filename for the focal method json file by method signature.
     */
    private static Path getFilePathBySig(String sig) {
        Map<String, String> mSigs = classInfo.methodSignatures;
        return Paths.get(mSigs.get(sig) + ".json");
    }

    /**
     * Get the filename of the focal method by finding method name and parameters in mSig.
     */
    public static Path getFilePathBySig(String mSig, ClassInfo info) {
        Map<String, String> mSigs = info.methodSignatures;
        return Paths.get(mSigs.get(mSig) + ".json");
    }

    private List<Path> getSources() {
        try (Stream<Path> paths = Files.walk(Path.of(System.getProperty("user.dir")))) {
            return paths
                    .filter(ClassParser::isJavaSourceDir)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void extractClass(String classPath) {
        File file = new File(classPath);
        try {
            ParseResult<CompilationUnit> parseResult = parser.parse(file);
            CompilationUnit cu = parseResult.getResult().orElseThrow();
            List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
            for (ClassOrInterfaceDeclaration classDeclaration : classes) {
                classInfo = getInfoByClass(cu, classDeclaration);
                exportClassInfo(GSON.toJson(classInfo), classDeclaration);
                extractMethods(cu, classDeclaration);
                extractConstructors(cu, classDeclaration);
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            e.printStackTrace();
        }
    }
}
