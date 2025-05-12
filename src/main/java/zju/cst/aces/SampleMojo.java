package zju.cst.aces;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import zju.cst.aces.util.Counter;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.dto.ClassInfo;
import zju.cst.aces.dto.MethodInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author chenyi
 * ChatUniTest maven plugin for sampling
 */

@Mojo(name = "sample")
public class SampleMojo extends ProjectTestMojo {

    /**
     * Generate sample data using Counter
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException {
        init();
        log = getLog();
        log.info("\n==========================\n[ChatUniTest] Generating sample data...");
        
        try {
            // Get sampleSize from config
            int sampleSize = config.getSampleSize();
            log.info("Using sample size: " + sampleSize);
            
            // Execute Counter.generateMethodCSV method
            Counter.generateMethodCSV(config);
            
            log.info("\n==========================\n[ChatUniTest] Sample data generation completed.");
        } catch (Exception e) {
            log.error("Error during sample data generation: " + e.getMessage(), e);
            throw new MojoExecutionException("Failed to generate sample data", e);
        }
    }

}
