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
package org.metaeffekt.dcc.controller.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.taskdefs.Delete;
import org.junit.Before;
import org.junit.Test;

import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.DccUtils;
import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.mapping.TestProfiles;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;
import org.metaeffekt.dcc.controller.execution.SSLConfiguration;

/**
 * Created by i001450 on 30.06.14.
 */
public class CommandTest {

    private TestCommand testCommand;
    private ExecutionContext executionContext;
    private static final File SOLUTION_LOCATION = new File("target/solution/");

    @Before
    public void prepare() throws IOException {
        executionContext = new ExecutionContext(new SSLConfiguration("keyStoreLocation",
                "keyStorePassword", "trustStoreLocation", "trustStorePassword"));
        executionContext.setSolutionDir(SOLUTION_LOCATION);
        executionContext.setTargetBaseDir(new File("target/dcc-destination"));
        testCommand = new TestCommand(executionContext);

        Delete delete = new Delete();
        delete.setDir(new File(SOLUTION_LOCATION, DccConstants.TMP_SUB_DIRECTORY));
        delete.execute();
    }

    @Test
    public void executeTestCommand() throws IOException {

        executionContext.setProfile(TestProfiles.QUITECOMLEXPROFILE);

        String PROPERTY_KEY = "foo";
        String PROPERTY_VALUE = "bar";
        
        executionContext.prepareForExecution();
        PropertiesHolder propertiesHolder = executionContext.getPropertiesHolder();
        propertiesHolder.setProperty(TestProfiles.PROVIDED_HOST_CAPABILITY, PROPERTY_KEY, PROPERTY_VALUE);

        testCommand.execute(true);

        assertTrue(testCommand.executed);
        assertEquals(1, testCommand.executedUnits.size());
        ConfigurationUnit unit = testCommand.executedUnits.get(0);
        assertEquals(TestProfiles.CONFIGURATION_UNIT_03, unit);

        File propertiesBaseDir = DccUtils.workingTmpConfigDir(SOLUTION_LOCATION, executionContext.getProfile().getDeploymentId());
        File executionPropertiesFile = new File(propertiesBaseDir, TestProfiles.CONFIGURATION_UNIT_03.getId() + "/" + testCommand.getCommandVerb() + ".properties");
        assertTrue("Execution properties not found", executionPropertiesFile.exists());

        Properties executionProperties = new Properties();
        executionProperties.load(new FileInputStream(executionPropertiesFile));
        assertEquals(1, executionProperties.size());
        assertEquals(PROPERTY_VALUE, executionProperties.getProperty(PROPERTY_KEY));
    }
    @Test
    public void executeTestCommandWithEmptyProfile() throws IOException {

        executionContext.setProfile(TestProfiles.EMPTY_PROFILE);

        testCommand.execute(true);

        assertTrue(testCommand.executed);
        assertEquals(0, testCommand.executedUnits.size());

        File executionPropertiesFile = new File(SOLUTION_LOCATION + "tmp/" + TestProfiles.CONFIGURATION_UNIT_03.getId() + "." + testCommand.getCommandVerb() + ".properties");
        assertFalse(executionPropertiesFile.exists());
    }


    private class TestCommand extends AbstractUnitBasedCommand {

        public boolean executed = false;
        public List<ConfigurationUnit> executedUnits = new LinkedList<>();

        public TestCommand(ExecutionContext executionContext) {
            super(executionContext);
        }

        @Override
        protected void doExecuteCommand(ConfigurationUnit unit) {
            executedUnits.add(unit);
        }

        @Override
        protected Commands getCommandVerb() {
            return Commands.START;
        }

        @Override
        protected void afterSuccessfulExecution(String indent, String context, long startTimestamp) {
            executed = true;
        }
    }
}
