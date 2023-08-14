package zju.cst.aces;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.util.Arrays;

@Mojo(name = "generateCoverage")
public class CoverageMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    public MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(project.getBasedir()+"/pom.xml")); // Set the path to the target project's pom.xml
        request.setGoals(Arrays.asList("clean", "test", "jacoco:report"));

        Invoker invoker = new DefaultInvoker();
//        invoker.setMavenHome(new File(System.getProperty("user.home"), ".m2"));
        invoker.setMavenHome(new File("C:\\software\\apache-maven-3.9.2"));


        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new MojoExecutionException("Execution failed.");
            }
        } catch (MavenInvocationException e) {
            throw new MojoExecutionException("Failed to execute goals.", e);
        }
    }
}
