package zju.cst.aces;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;


@Mojo(name = "generateCoverage")
public class CoverageMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    public MavenProject project;
    public static Log log;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log=getLog();
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(project.getBasedir()+"/pom.xml")); // Set the path to the target project's pom.xml
        request.setGoals(Arrays.asList("clean", "test", "jacoco:report"));

        Invoker invoker = new DefaultInvoker();
//        invoker.setMavenHome(new File(System.getProperty("user.home"), ".m2"));
        //环境变量中配置的MavenHome
        invoker.setMavenHome(new File("C:\\software\\apache-maven-3.9.2"));

        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new MojoExecutionException("Execution failed.");
            }
        } catch (MavenInvocationException e) {
            throw new MojoExecutionException("Failed to execute goals.", e);
        }
        Path sourcePath = Paths.get(project.getBasedir().toString(), "target", "site", "jacoco", "jacoco.csv");
        String target="D:\\coverage";
        Path targetPath = Paths.get(target);
        if (!Files.exists(targetPath)) {
            try {
                Files.createDirectories(targetPath);
                log.info("目标文件夹已创建！");
            } catch (IOException e) {
                log.error("无法创建目标文件夹：" + e.getMessage());
                return;
            }
        }
        try {
            Files.copy(sourcePath,targetPath.resolve("testcoverage.csv"), StandardCopyOption.REPLACE_EXISTING);
            log.info("copy succesful");
        } catch (IOException e) {
            log.info("copy failed");
            throw new RuntimeException(e);
        }

    }
}
