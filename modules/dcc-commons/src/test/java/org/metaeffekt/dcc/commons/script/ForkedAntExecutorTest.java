/**
 * Copyright 2009-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.metaeffekt.dcc.commons.script;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.junit.Before;
import org.junit.Test;

import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;

public class ForkedAntExecutorTest {

    private CommandScriptExecutor commandScriptExecutor;

    private boolean forkedExecution = false;

    @Before
    public void init() {
        CommandScriptExecutionContext executionContext = new CommandScriptExecutionContext();
        if (forkedExecution) {
            executionContext.setAntHomeDir(new File("C:\\dev\\bin\\apache-ant-1.9.6"));
        }
        executionContext.setDeploymentId(Id.createDeploymentId("test"));
        executionContext.setTargetDir(new File("target/test"));
        executionContext.setSolutionDir(new File("src/test/resources/test-solution"));
        executionContext.setWorkingDir(new File("target/working"));
        executionContext.setPackageDir(new File("src/test/resources/test-solution/packages"));
        commandScriptExecutor = new CommandScriptExecutor(executionContext, null);
    }

    @Test
    public void test_success() {
        commandScriptExecutor.executeCommand(Commands.CONFIGURE, Id.createUnitId("test-unit"),
                Id.createPackageId("test-package"), null, null);
    }

    @Test(expected = BuildException.class)
    public void test_failInstall() {
        commandScriptExecutor.executeCommand(Commands.INSTALL, Id.createUnitId("test-unit"),
                Id.createPackageId("test-package"), null, null);
    }
    
    @Test(expected = BuildException.class)
    public void test_failBootstrap() {
        commandScriptExecutor.executeCommand(Commands.BOOTSTRAP, Id.createUnitId("test-unit"),
                Id.createPackageId("test-package"), null, null);
    }
    
    @Test(expected = BuildException.class)
    public void test_failImport() {
        commandScriptExecutor.executeCommand(Commands.IMPORT, Id.createUnitId("test-unit"),
                Id.createPackageId("test-package"), null, null);
    }

}
