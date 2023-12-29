package zju.cst.aces;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.FileUtils;
import zju.cst.aces.util.TestCompiler;

/**
 * @author chenyi
 * ChatUniTest maven plugin
 */

@Mojo(name = "clean")
public class CleanMojo
        extends ProjectTestMojo {

    /**
     * Clean output directory and restore backup test folder
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException {
        init();
        log.info("\n==========================\n[ChatUniTest] Cleaning project " +  project.getBasedir().getName() + " ...");
        log.info("\n==========================\n[ChatUniTest] Cleaning output directory "
                + config.getTmpOutput() + " and " + config.getTestOutput() + " ...");
        try {
            log.info("\n==========================\n[ChatUniTest] Restoring backup folder ...");
            FileUtils.deleteDirectory(config.getTmpOutput().toFile());
            FileUtils.deleteDirectory(config.getTestOutput().toFile());
            TestCompiler compiler = new TestCompiler(config.getTestOutput(), config.getCompileOutputPath(),
                    config.getProject().getBasedir().toPath().resolve("target"), config.getClassPaths());
            compiler.restoreBackupFolder();
        } catch (Exception e) {
            log.error(e);
        }
        log.info("\n==========================\n[ChatUniTest] Finished");
    }
}
