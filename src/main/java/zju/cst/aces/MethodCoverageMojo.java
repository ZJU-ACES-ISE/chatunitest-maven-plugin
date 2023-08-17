package zju.cst.aces;

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
import java.util.Arrays;

@Mojo(name = "generateMethodCoverage")
public class MethodCoverageMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    public MavenProject project;

    @Parameter(defaultValue = "com/hhh/plugin/MyTest1_sayHello2_1_1_Test#testSayHello2", required = true)
    public String testMethod;
    public static Log log;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log=getLog();
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(project.getBasedir(), "pom.xml"));
        request.setGoals(Arrays.asList("clean", "test-compile")); // Compile tests only

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File("C:\\software\\apache-maven-3.9.2"));

        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new MojoExecutionException("Compilation failed.");
            }
        } catch (MavenInvocationException e) {
            log.error("Failed to execute goals.", e);
            throw new MojoExecutionException("Failed to execute goals.", e);
        }

        // Run the specified test method
        request = new DefaultInvocationRequest();
        request.setPomFile(new File(project.getBasedir(), "pom.xml"));
        request.setGoals(Arrays.asList("test", "-Dtest=" + testMethod));

        // Set the test method as a property in the request

        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new MojoExecutionException("Test execution failed.");
            }
        } catch (MavenInvocationException e) {
            log.error("Failed to execute test.", e);
            throw new MojoExecutionException("Failed to execute test.", e);
        }

        request.setGoals(Arrays.asList("jacoco:report"));

        try {
            InvocationResult result = invoker.execute(request);
            System.out.println("Coverage of method:"+testMethod+" is "+getMethodCoverage());
            log.info("Coverage of method:"+testMethod+" is "+getMethodCoverage());
            if (result.getExitCode() != 0) {
                throw new MojoExecutionException("Execution failed.");
            }
        } catch (MavenInvocationException e) {
            log.error("Failed to execute goals.", e);
            throw new MojoExecutionException("Failed to execute goals.", e);
        }
    }
    public String getMethodCoverage(){
        File htmlFile = new File(project.getBasedir()+"/target/site/jacoco/index.html"); // Replace with your HTML file path
        try {
            Document doc = Jsoup.parse(htmlFile, "UTF-8");
            Element table = doc.select("table.coverage").first(); // Assuming the table has the class "coverage"

            if (table != null) {
                Elements rows = table.select("tfoot tr");
                Element lastRow = rows.first(); // Get the first row of the tfoot section

                if (lastRow != null) {
                    Elements columns = lastRow.select("td");

                    if (columns.size() >= 3) { // The second column is index 1
                        String data = columns.get(2).text(); // Index 2 corresponds to the third column
                        System.out.println("Data from last row, third column: " + data);
                        return data;
                    } else {
                        System.out.println("Table does not have enough columns in the last row.");
                    }
                } else {
                    System.out.println("Tfoot section is empty.");
                }
            } else {
                System.out.println("Table with class 'coverage' not found.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
