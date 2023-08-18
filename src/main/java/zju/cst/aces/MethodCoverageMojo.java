package zju.cst.aces;

import com.sun.jdi.request.WatchpointRequest;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
    @Parameter(property = "goalMethod")
    public String goalMethod;
    @Parameter(property = "executeTests")
    public String[] executeTests;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log=getLog();
        File pomFile=new File(project.getBasedir(), "pom.xml");
//        String goalMethod="com.hhh.plugin.Calculator#add(int,int)";
        String target_className = goalMethod.substring(0, goalMethod.lastIndexOf(".")) + "/" + goalMethod.substring(goalMethod.lastIndexOf(".") + 1,goalMethod.lastIndexOf("#"));
        String target_methodSignature=goalMethod.substring(goalMethod.lastIndexOf("#")+1);
        // 复制外部目录到 src/test/java
        String srcTestJavaPath = project.getBasedir().toString() + "/src/test/java/chatunitest";
        for (int i = 0; i < executeTests.length; i++) {
            executeTests[i]=executeTests[i].replace(".","/");
        }

        try {
            copyDirectory(new File(sourceDir), new File(srcTestJavaPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // 运行 Maven 测试
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pomFile);
        request.setGoals(Arrays.asList("clean", "test-compile"));
        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File(mavenHome));
        try {
            invoker.execute(request);
        } catch (MavenInvocationException e) {
            throw new RuntimeException(e);
        }


        String commandTest=String.join(",",executeTests);
        request = new DefaultInvocationRequest();
        request.setPomFile(pomFile);
        request.setGoals(Arrays.asList("test", "-Dtest=" + commandTest));
        invoker = new DefaultInvoker();
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

        // 删除临时复制的目录
        try {
            FileUtils.deleteDirectory(new File(srcTestJavaPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        File htmlFile = new File(project.getBasedir().toString()+"/target/site/jacoco/"+target_className+".html");
        String htmlContent = ""; // 在这里替换为你的HTML内容
        try {
            htmlContent = FileUtils.readFileToString(htmlFile,"UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Document doc = Jsoup.parse(htmlContent);
        Element coverageTable = doc.getElementById("coveragetable");

        if (coverageTable != null) {
            Elements rows = coverageTable.select("tbody > tr");
            for (Element row : rows) {
                Element methodNameElement = row.selectFirst("td[id^='a']");
                String methodName = methodNameElement.text();
                if (methodName.replace(" ","").equals(target_methodSignature.replace(" ",""))) {
                    Element instructionCoverageElement = row.selectFirst("td.ctr2:nth-child(3)");
                    Element branchCoverageElement = row.selectFirst("td.ctr2:nth-child(5)");
                    String instructionCoverage = instructionCoverageElement.text();
                    String branchCoverage = branchCoverageElement.text();
                    log.info("行覆盖率: " + instructionCoverage + ", 分支覆盖率: " + branchCoverage);
//                    System.out.println("行覆盖率: " + instructionCoverage + ", 分支覆盖率: " + branchCoverage);
                    break;
                }
            }
        } else {
//            System.out.println("未找到覆盖率表格");
            log.info("未找到覆盖率表格");
        }

    }
    public static void copyDirectory(File sourceDirectory, File targetDirectory) throws IOException {
        FileUtils.copyDirectory(sourceDirectory, targetDirectory);
    }
}
