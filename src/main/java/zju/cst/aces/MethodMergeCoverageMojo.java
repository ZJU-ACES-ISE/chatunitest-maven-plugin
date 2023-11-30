package zju.cst.aces;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import zju.cst.aces.util.BackupUtil;
import zju.cst.aces.util.JacocoParser;
import zju.cst.aces.util.JacocoParser.CoverageData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 为每个测试类单独生成覆盖率数据（每个都是单独运行）
 */
@Mojo(name = "generateMethodCoverage_merge")
public class MethodMergeCoverageMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    public MavenProject project;


    public static Log log;

    @Parameter(property = "targetDir")
    public String targetDir;
    @Parameter(property = "sourceDir")
    public String sourceDir;
    @Parameter(property = "mavenHome")
    public String mavenHome;


    public static boolean createDirectory(File directoryPath){
        if(!directoryPath.exists()){
            return directoryPath.mkdirs();
        }
        return false;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log = getLog();
        if (project.getPackaging().equals("pom")) {
            log.info("\n==========================\n[ChatUniTest] Skip pom-packaging ...");
            return;
        }
        File pomFile = new File(project.getBasedir(), "pom.xml");

        // 复制外部目录到 src/test/java
        String srcTestJavaPath = Paths.get(project.getBasedir().toString() , "/src/test/java/chatunitest").toString();

        try {
            if (sourceDir.equals(project.getBasedir().toPath().resolve("chatunitest-tests").toString())) {
                copyDirectory(new File(sourceDir), new File(srcTestJavaPath));
            } else {
                MavenProject p = project.clone();
                String parentPath = "";
                while(p != null && p.getBasedir() != null) {
                    parentPath =  Paths.get(p.getArtifactId()).resolve(parentPath).toString();
                    p = p.getParent();
                }
                Path resolvedSourceDir = Paths.get(sourceDir).resolve(parentPath);
                if(!Files.exists(resolvedSourceDir)){
                    log.warn(resolvedSourceDir.toString()+" does not exist.");
                    return;
                }

                copyDirectory(resolvedSourceDir.toFile(), new File(srcTestJavaPath));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        SignatureGetter signatureGetter = new SignatureGetter();
        List<File> files = listJavaFiles(new File(srcTestJavaPath));
        //先备份target目录（执行mvn clean compile的状态）
        try {
            BackupUtil.backupTargetFolder(Paths.get(project.getBasedir().toString() , "target").toString(), Paths.get(project.getBasedir().toString() , "backup").toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        HashMap<String, List<CoverageData>> coverageMap = new HashMap<>();
        HashMap<String, List<String>> executeClassMap = new HashMap<>();
        Properties properties = new Properties();
        String javaHome = System.getenv("JAVA_HOME");
        properties.setProperty("JAVA_HOME", javaHome);
        for(File file:files){
            String testclassName=extractClassName(srcTestJavaPath,file);
            String[] s1=testclassName.split("_", 4);
            String prefix_className = s1[0]+"_"+s1[1]+"_"+s1[2];
            if(executeClassMap.get(prefix_className)!=null){
                executeClassMap.get(prefix_className).add(testclassName);
            }
            else {
                ArrayList<String> classNameList = new ArrayList<>();
                classNameList.add(testclassName);
                executeClassMap.put(prefix_className,classNameList);
            }
        }
        for (String key : executeClassMap.keySet()) {
            List<String> executeClasses = executeClassMap.get(key);
            for (int i = 0; i < executeClasses.size(); i++) {
                String s = executeClasses.get(i);
                s=s.replaceAll("\\.","/");
                executeClasses.set(i,s);
            }
            if(executeClasses.size()>1){
                for (int i = 0; i < executeClasses.size(); i++) {
                    String join_execute_classes = String.join(",", sortByLastDigit(executeClasses).subList(0,i+1));
                    log.info("the test this epoch runs: "+join_execute_classes);
                    //遍历executeClassMap，一个list为一次运行，抽取覆盖率
                    String testclassName = key+"_"+"X";//X表示第几轮生成的
                    testclassName = testclassName.replaceAll("\\.", "/");
                    try {
                        String[] s = signatureGetter.extractClassNameAndIndex(testclassName);
                        String className = s[0];
                        int index = Integer.parseInt(s[1]);
                        String methodSignature=signatureGetter.getMethodSignature(className, String.valueOf(project.getBasedir()),index);
                        //解析jacoco.xml需要用到的methodName
                        String xml_methodName=s[2];
                        log.info("xml_methodName = " + xml_methodName);
                        //清空上一次的测试信息
                        BackupUtil.restoreTargetFolder(Paths.get(project.getBasedir().toString() , "backup").toString(), Paths.get(project.getBasedir().toString() , "target").toString());
                        InvocationRequest request = new DefaultInvocationRequest();
                        Invoker invoker = new DefaultInvoker();
                        invoker.setMavenHome(new File(mavenHome));
                        request.setPomFile(pomFile);
                        request.setGoals(Arrays.asList("test", "-Dtest=" + join_execute_classes));
                        request.setProperties(properties);
                        try {
                            invoker.execute(request);
                        } catch (MavenInvocationException e) {
                            throw new RuntimeException(e);
                        }
                        request = new DefaultInvocationRequest();
                        request.setPomFile(pomFile);
                        request.setGoals(Arrays.asList("jacoco:report"));
                        invoker = new DefaultInvoker();
                        invoker.setMavenHome(new File(mavenHome));
                        try {
                            invoker.execute(request);
                        } catch (MavenInvocationException e) {
                            throw new RuntimeException(e);
                        }
                        String tempName=className;
                        int lastIndex = tempName.lastIndexOf(".");
                        if(lastIndex!=-1){
                            String prefix=className.substring(0,lastIndex);
                            String postfix=className.substring(lastIndex+1);
                            tempName=prefix+"/"+postfix;
                        }
                        File htmlFile = new File(project.getBasedir().toString() + "/target/site/jacoco/" + tempName + ".html");
                        //jacoco.xml路径
                        String xmlFilePath=project.getBasedir().toString()+"/target/site/jacoco/jacoco.xml";
                        JacocoParser jacocoParser = new JacocoParser();
                        CoverageData coverageData = jacocoParser.getJacocoHtmlParsedInfo(htmlFile, methodSignature, testclassName.replaceAll("/", "."));
                        List<JacocoParser.CoverageInfo> coverageInfo = jacocoParser.getCoverageInfo(xmlFilePath, className, methodSignature);
                        coverageData.setCoverageInfo(coverageInfo);
                        List<CoverageData> dataList = coverageMap.get(className);
                        //添加记录
                        if (dataList == null) {
                            dataList = new ArrayList<>();
                            coverageMap.put(className, dataList);
                        }
                        dataList.add(coverageData);
                        try {
                            File designate_path = Paths.get(targetDir,"merge", String.valueOf(i+1)).toFile();
                            createDirectory(designate_path);
                            copyDirectory(new File(project.getBasedir().toString()+"/target/site"), designate_path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            else {
                String join_execute_classes = String.join(",", executeClasses);
                //遍历executeClassMap，一个list为一次运行，抽取覆盖率
                String testclassName = key+"_"+"X";//X表示第几轮生成的
                log.info("debugging 2" + testclassName);
                testclassName = testclassName.replaceAll("\\.", "/");
                try {
                    String[] s = signatureGetter.extractClassNameAndIndex(testclassName);
                    String className = s[0];
                    int index = Integer.parseInt(s[1]);
                    String methodSignature=signatureGetter.getMethodSignature(className, String.valueOf(project.getBasedir()),index);
                    //解析jacoco.xml需要用到的methodName
                    String xml_methodName=s[2];
                    log.info("xml_methodName = " + xml_methodName);
                    // 运行 Maven 测试
                    //清空上一次的测试信息
                    BackupUtil.restoreTargetFolder(Paths.get(project.getBasedir().toString() , "backup").toString(), Paths.get(project.getBasedir().toString() , "target").toString());

                    InvocationRequest request = new DefaultInvocationRequest();
                    Invoker invoker = new DefaultInvoker();
                    request.setPomFile(pomFile);
                    request.setGoals(Arrays.asList("test", "-Dtest=" + join_execute_classes));
                    request.setProperties(properties);
                    invoker.setMavenHome(new File(mavenHome));
                    try {
                        invoker.execute(request);
                    } catch (MavenInvocationException e) {
                        throw new RuntimeException(e);
                    }

                    request = new DefaultInvocationRequest();
                    request.setPomFile(pomFile);
                    request.setGoals(Arrays.asList("jacoco:report"));
                    invoker = new DefaultInvoker();
                    invoker.setMavenHome(new File(mavenHome));
                    try {
                        invoker.execute(request);
                    } catch (MavenInvocationException e) {
                        throw new RuntimeException(e);
                    }
                    String tempName=className;
                    int lastIndex = tempName.lastIndexOf(".");
                    if(lastIndex!=-1){
                        String prefix=className.substring(0,lastIndex);
                        String postfix=className.substring(lastIndex+1);
                        tempName=prefix+"/"+postfix;
                    }
                    File htmlFile = new File(project.getBasedir().toString() + "/target/site/jacoco/" + tempName + ".html");
                    //jacoco.xml路径
                    String xmlFilePath=project.getBasedir().toString()+"/target/site/jacoco/jacoco.xml";
                    JacocoParser jacocoParser = new JacocoParser();
                    CoverageData coverageData = jacocoParser.getJacocoHtmlParsedInfo(htmlFile, methodSignature, testclassName.replaceAll("/", "."));
                    List<JacocoParser.CoverageInfo> coverageInfo = jacocoParser.getCoverageInfo(xmlFilePath, className, methodSignature);
                    coverageData.setCoverageInfo(coverageInfo);
                    List<CoverageData> dataList = coverageMap.get(className);
                    //添加记录
                    if (dataList == null) {
                        dataList = new ArrayList<>();
                        coverageMap.put(className, dataList);
                    }
                    dataList.add(coverageData);
                    try {
                        File designate_path = Paths.get(targetDir,"merge", String.valueOf(0)).toFile();
                        createDirectory(designate_path);
                        copyDirectory(new File(project.getBasedir().toString()+"/target/site"), designate_path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        File directory = new File(targetDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(directory, "methodCoverage_MERGE.json");
        try (FileWriter writer = new FileWriter(file)) {
            Gson gson = new Gson();
            gson.toJson(coverageMap, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 删除临时复制的目录
        try {
            FileUtils.deleteDirectory(new File(srcTestJavaPath));
            BackupUtil.restoreTargetFolder(Paths.get(project.getBasedir().toString() , "backup").toString(), Paths.get(project.getBasedir().toString() , "target").toString());
            BackupUtil.deleteBackupFolder(Paths.get(project.getBasedir().toString() , "backup").toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copyDirectory(File sourceDirectory, File targetDirectory) throws IOException {
        FileUtils.copyDirectory(sourceDirectory, targetDirectory);
    }

    public class SignatureGetter {
        public String getMethodSignature(String className, String projectPath, int methodIndex) throws IOException {
            JavaParser javaParser = new JavaParser();
            ParseResult<CompilationUnit> parseResult = javaParser.parse(Paths.get(projectPath, "src/main/java",
                    className.replaceAll("\\.", "/") + ".java"));
            if (parseResult.isSuccessful()) {
                CompilationUnit cu = parseResult.getResult().get();
                MethodSignatureVisitor methodVisitor = new MethodSignatureVisitor(methodIndex);
                methodVisitor.visit(cu, null);
                return methodVisitor.getMethodSignature();
            } else {
                throw new IOException("Failed to parse the file.");
            }
        }

        private class MethodSignatureVisitor extends VoidVisitorAdapter<Void> {
            private final int targetMethodIndex;
            private int currentIndex;
            private String methodSignature;

            public MethodSignatureVisitor(int targetMethodIndex) {
                this.targetMethodIndex = targetMethodIndex;
                this.currentIndex = 0;
                this.methodSignature = null;
            }

            public String getMethodSignature() {
                return methodSignature;
            }

            @Override
            public void visit(MethodDeclaration methodDeclaration, Void arg) {
                super.visit(methodDeclaration, arg);
                if (currentIndex == targetMethodIndex) {
                    String methodName = methodDeclaration.getNameAsString();
                    String paramTypes = getParameterTypes(methodDeclaration);
                    methodSignature = methodName + "(" + paramTypes + ")";
                }
                currentIndex++;
            }

            private String getParameterTypes(MethodDeclaration methodDeclaration) {
                StringBuilder paramTypes = new StringBuilder();
                boolean isFirst = true;
                for (com.github.javaparser.ast.body.Parameter parameter : methodDeclaration.getParameters()) {
                    if (!isFirst) {
                        paramTypes.append(", ");
                    }
                    Type type = parameter.getType();
                    paramTypes.append(type.toString());
                    isFirst = false;
                }
                return paramTypes.toString();
            }
        }

        public String[] extractClassNameAndIndex(String className) {
            String[] parts = className.split("_");
            if (parts.length >= 4) {
                String classPart = parts[0].replaceAll("/", ".");
                String indexPart = parts[2];
                String methodNamePart=parts[1];
                return new String[]{classPart, indexPart,methodNamePart};//result[0]是className，[1]是index,[2]是methodName
            }
            return new String[]{};
        }
    }
    public static List<String> sortByLastDigit(List<String> list) {
        List<String> sortedList = new ArrayList<>(list);

        Collections.sort(sortedList, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                // 获取s1和s2中最后一个数字
                int lastDigit1 = getLastDigit(s1);
                int lastDigit2 = getLastDigit(s2);

                // 比较最后一个数字并返回比较结果
                return Integer.compare(lastDigit1, lastDigit2);
            }
        });
        return sortedList;
    }

    public static int getLastDigit(String s) {
        return Integer.parseInt(s.split("_",5)[3]);
    }
    public static List<File> listJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        if (directory.isDirectory()) {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                if (file.isDirectory()) {
                    javaFiles.addAll(listJavaFiles(file));
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }
        return javaFiles;
    }

    public static String extractClassName(String baseDirectory, File javaFile) {
        String path = javaFile.getAbsolutePath();
        String relativePath = path.substring(baseDirectory.length() + 1);
        String className = relativePath.replace(File.separatorChar, '.');
        return className.substring(0, className.length() - ".java".length());
    }




}
