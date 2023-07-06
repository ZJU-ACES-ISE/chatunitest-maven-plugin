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
import zju.cst.aces.utils.TestCompiler;

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
        log.info("\n==========================\n[ChatTester] Cleaning project " +  project.getBasedir().getName() + " ...");
        log.info("\n==========================\n[ChatTester] Cleaning output directory "
                + tmpOutput + " and " + testOutput + " ...");
        try {
            log.info("\n==========================\n[ChatTester] Restoring test folder ...");
            FileUtils.deleteDirectory(tmpOutput);
            FileUtils.deleteDirectory(testOutput);
            TestCompiler.restoreTestFolder();
        } catch (Exception e) {
            log.error(e);
        }
        log.info("\n==========================\n[ChatTester] Finished");
    }
}
