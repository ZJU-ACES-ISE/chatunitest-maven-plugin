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
@Mojo(name = "generateMethodCoverage")
public class MethodCoverageMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    public MavenProject project;

    public static Log log;
    @Parameter(property = "targetDir")
    public String targetDir;
    @Parameter(property = "sourceDir")
    public String sourceDir;
    @Parameter(property = "mavenHome")
    public String mavenHome;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log = getLog();
        if (project.getPackaging().equals("pom")) {
            log.info("\n==========================\n[ChatUniTest] Skip pom-packaging ...");
            return;
        }
        File pomFile = new File(project.getBasedir(), "pom.xml");
        // 复制外部目录到 src/test/java
        String srcTestJavaPath = Paths.get(project.getBasedir().toString(), "/src/test/java/chatunitest").toString();
        String testDir;
        if (sourceDir.equals(project.getBasedir().toPath().resolve("chatunitest-tests").toString())) {
            testDir = sourceDir;
        } else { // 指定输出目录
            MavenProject p = project.clone();
            String parentPath = "";
            while(p != null && p.getBasedir() != null) {
                parentPath =  Paths.get(p.getArtifactId()).resolve(parentPath).toString();
                p = p.getParent();
            }
            Path resolvedSourceDir = Paths.get(sourceDir).resolve(parentPath);
            if (!Files.exists(resolvedSourceDir)) {
                log.warn(resolvedSourceDir.toString() + " does not exist.");
                return;
            }
            testDir = resolvedSourceDir.toFile().toString();
        }

        SignatureGetter signatureGetter = new SignatureGetter();
        List<File> files = listJavaFiles(new File(testDir));
        //先备份target目录（执行mvn clean compile的状态）
        try {
            BackupUtil.backupTargetFolder(Paths.get(project.getBasedir().toString(), "target").toString(), Paths.get(project.getBasedir().toString(), "backup").toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        HashMap<String, List<HashMap<String, List<JacocoParser.CoverageInfo>>>> coverageMap = new HashMap<>();

        for (File file : files) {
            log.info("testClassName:" + file.toString());
            File copiedFile;
            try {
                Path basePath = (new File(testDir)).toPath();
                Path filePath = file.toPath();
                Path relativePath = basePath.relativize(filePath);
                Path destinationDir = Paths.get(srcTestJavaPath);
                copiedFile = destinationDir.resolve(relativePath).toFile();
                copiedFile.getParentFile().mkdirs();
                FileUtils.copyFile(file, copiedFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String originTestClassName = extractClassName(srcTestJavaPath, copiedFile);
            String testclassName;
            String testFileName = copiedFile.getName().replace(".java", "");
            testclassName = originTestClassName.replace(".", "/");
            try {
                String[] s = signatureGetter.extractClassNameAndIndex(testclassName);
                String className = s[0];
                int index = Integer.parseInt(s[1]);
                String methodSignature = signatureGetter.getMethodSignature(className, String.valueOf(project.getBasedir()), index);
                //解析jacoco.xml需要用到的methodName
                Properties properties = new Properties();
                String javaHome = System.getenv("JAVA_HOME");
                properties.setProperty("JAVA_HOME", javaHome);
                properties.setProperty("gpg.skip", "true");
                properties.setProperty("license.skip", "true");
                properties.setProperty("sortpom.skip", "true");
                properties.setProperty("maven.javadoc.skip", "true");
                properties.setProperty("checkstyle.skip", "true");
                properties.setProperty("rat.skip", "true");
                //清空上一次的测试信息
                BackupUtil.restoreTargetFolder(Paths.get(project.getBasedir().toString(), "backup").toString(), Paths.get(project.getBasedir().toString(), "target").toString());
                // 运行 Maven 测试
                log.info("Running mvn test ...");
                InvocationRequest request = new DefaultInvocationRequest();
                Invoker invoker = new DefaultInvoker();
                invoker.setMavenHome(new File(mavenHome));
                request.setPomFile(pomFile);
                request = new DefaultInvocationRequest();
                request.setPomFile(pomFile);
                request.setGoals(Arrays.asList("test", "-Dtest=" + testclassName));
                request.setProperties(properties);
                try {
                    invoker.execute(request);
                    log.info("running mvn test" + request.getGoals());
                } catch (Exception e) {
                    log.warn("Error happened during compilation or execution of "+file.getName());
                    FileUtils.deleteQuietly(file);
                    FileUtils.deleteQuietly(copiedFile);
                    continue;
                }
                log.info("Running mvn jacoco:report");
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
                //jacoco report info
                //full class name
                log.info("ClassName" + className);
                String tempName = className;
                int lastIndex = tempName.lastIndexOf(".");
                if (lastIndex != -1) {
                    String prefix = className.substring(0, lastIndex);
                    String postfix = className.substring(lastIndex + 1);
                    tempName = prefix + "/" + postfix;
                }
                //jacoco html路径 "your.package.name/ClassName.html"
                /*File htmlFile = new File(project.getBasedir().toString() + "/target/site/jacoco/" + tempName + ".html");
                log.info("jacoco html file path:"+htmlFile);*/
                //jacoco.xml路径
                String xmlFilePath = project.getBasedir().toString() + "/target/site/jacoco/jacoco.xml";
                log.info("jacoco xml file path:" + xmlFilePath);
                JacocoParser jacocoParser = new JacocoParser();
                /*log.warn("htmlpath:"+htmlFile.getAbsolutePath());
                log.warn("methodSignature:"+methodSignature);
                log.warn("testclassName:"+testclassName.replaceAll("/", "."));
                log.warn("xmlFilePath:"+xmlFilePath);
                log.warn("className:"+className);*/
                //添加记录
                List<HashMap<String, List<JacocoParser.CoverageInfo>>> dataList = jacocoParser.getJacocoXmlParsedInfo(xmlFilePath, className);
                coverageMap.put(originTestClassName, dataList);
                try {
                    File designate_path = Paths.get(targetDir, "separate", testFileName).toFile();
                    createDirectory(designate_path);
                    copyDirectory(new File(project.getBasedir().toString() + "/target/site"), designate_path);
                } catch (IOException e) {
                    log.warn("Jacoco not found in /target/site/ :" + copiedFile.getName());
                    log.warn(e);
                }
                FileUtils.deleteQuietly(copiedFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        File directory = new File(targetDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(directory, "methodCoverage.json");
        try (FileWriter writer = new FileWriter(file)) {
            Gson gson = new Gson();
            gson.toJson(coverageMap, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 删除临时复制的目录
        try {
            FileUtils.deleteDirectory(new File(srcTestJavaPath));
            //恢复target
            BackupUtil.restoreTargetFolder(Paths.get(project.getBasedir().toString(), "backup").toString(), Paths.get(project.getBasedir().toString(), "target").toString());
            BackupUtil.deleteBackupFolder(Paths.get(project.getBasedir().toString(), "backup").toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copyDirectory(File sourceDirectory, File targetDirectory) throws IOException {
        FileUtils.copyDirectory(sourceDirectory, targetDirectory);
    }

    public static boolean createDirectory(File directoryPath) {
        if (!directoryPath.exists()) {
            return directoryPath.mkdirs();
        }
        return false;
    }

    public static class SignatureGetter {
        public String getMethodSignature(String className, String projectPath, int methodIndex) throws IOException {
            JavaParser javaParser = new JavaParser();
            ParseResult<CompilationUnit> parseResult = javaParser.parse(Paths.get(projectPath, "src/main/java", className.replace(".", "/") + ".java"));
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
                String classPart = parts[0].replace("/", ".");
                String indexPart = parts[2];
                String methodNamePart = parts[1];
                return new String[]{classPart, indexPart, methodNamePart};//result[0]是className，[1]是index,[2]是methodName
            }
            return new String[]{};
        }
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
