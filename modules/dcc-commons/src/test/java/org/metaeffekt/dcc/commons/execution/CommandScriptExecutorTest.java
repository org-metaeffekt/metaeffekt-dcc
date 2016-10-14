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
package org.metaeffekt.dcc.commons.execution;

import static org.metaeffekt.dcc.commons.commands.Commands.CONFIGURE;
import static org.metaeffekt.dcc.commons.commands.Commands.INSTALL;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.BuildException;
import org.junit.Before;
import org.junit.Test;

import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type;
import org.metaeffekt.dcc.commons.script.CommandScriptExecutionContext;
import org.metaeffekt.dcc.commons.script.CommandScriptExecutor;


public class CommandScriptExecutorTest {

    public static final Id<Type.UnitId> UNIT_ID = Id.createUnitId("testunit");
    public static final Id<Type.PackageId> PACKAGE_ID = Id.createPackageId("testpackage");
    public static final File UNIT_PROPERTIES_FILE = new File("target", "test/script-execution-test/tmp/unit.properties");

    private CommandScriptExecutionContext executionContext;
    private ExecutionStateHandler executionStateHandler;

    @Before
    public void initCommonsExecutionContext() throws IOException {
        executionContext = new CommandScriptExecutionContext();
        executionContext.setSolutionDir(new File("src/test/resources/script-execution-test"));
        executionContext.setPackageDir(new File("src/test/resources/script-execution-test/packages"));
        executionContext.setTargetDir(new File("target", "test/script-execution-test/opt"));
        executionContext.setWorkingDir(new File("target", "test/script-execution-test/workingdir"));

        executionStateHandler = new ExecutionStateHandler(executionContext.getTargetDir(), executionContext.getSolutionDir());
        
        // the property file must be made available for every execution
        FileUtils.write(UNIT_PROPERTIES_FILE, "# unit properties");
    }

    @Test
    public void testSeparateJvm() {
        executionContext.setAntHomeDir(new File("some-file-location-that-does-not-exist"));

        CommandScriptExecutor commandScriptExecutor = new CommandScriptExecutor(executionContext, executionStateHandler);
        commandScriptExecutor.executeCommand(CONFIGURE, UNIT_ID, PACKAGE_ID,
            UNIT_PROPERTIES_FILE, null);
    }

    @Test(expected=BuildException.class)
    public void testSeparateJvm_Failure() {
        executionContext.setAntHomeDir(new File("some-file-location-that-does-not-exist"));

        CommandScriptExecutor commandScriptExecutor = new CommandScriptExecutor(executionContext, executionStateHandler);
        commandScriptExecutor.executeCommand(INSTALL, UNIT_ID, PACKAGE_ID,
            UNIT_PROPERTIES_FILE, null);
    }

    @Test
    public void testLocalJvm() {
        CommandScriptExecutor commandScriptExecutor = new CommandScriptExecutor(executionContext, executionStateHandler);
        commandScriptExecutor.executeCommand(CONFIGURE, UNIT_ID, PACKAGE_ID,
            UNIT_PROPERTIES_FILE, null);
    }

    @Test
    public void testLocalJvmGenerate() {
        CommandScriptExecutor commandScriptExecutor = new CommandScriptExecutor(executionContext, executionStateHandler);
        commandScriptExecutor.executeCommand(Commands.INITIALIZE_RESOURCES, UNIT_ID, PACKAGE_ID,
                UNIT_PROPERTIES_FILE, null);
    }

    @Test(expected=BuildException.class)
    public void testLocalJvm_Failure() {
        CommandScriptExecutor commandScriptExecutor = new CommandScriptExecutor(executionContext, executionStateHandler);
        commandScriptExecutor.executeCommand(INSTALL, UNIT_ID, PACKAGE_ID,
            UNIT_PROPERTIES_FILE, null);
    }

}
