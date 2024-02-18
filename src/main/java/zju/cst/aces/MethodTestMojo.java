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
import org.apache.maven.plugins.annotations.Parameter;
import zju.cst.aces.api.Task;
import zju.cst.aces.api.impl.RunnerImpl;

/**
 * @author chenyi
 * ChatUniTest maven plugin
 */

@Mojo(name = "method")
public class MethodTestMojo
        extends ProjectTestMojo {
    @Parameter(property = "selectMethod", required = true)
    public String selectMethod;

    /**
     * Generate test for target method in given class
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException {
        init();
        String className = selectMethod.split("#")[0];
        String methodName = selectMethod.split("#")[1];
        new Task(config, new RunnerImpl(config)).startMethodTask(className, methodName);
    }
}
