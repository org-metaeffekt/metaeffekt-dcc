/**
 * Copyright 2009-2017 the original author or authors.
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
package org.metaeffekt.dcc.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.taskdefs.VerifyJar;
import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.ant.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.BuildException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.spring.xml.ProfileParser;
import org.metaeffekt.dcc.controller.DccControllerConstants;
import org.metaeffekt.dcc.controller.commands.*;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;
import org.metaeffekt.dcc.controller.execution.SSLConfiguration;
import org.metaeffekt.dcc.shell.DccShell;

public class ProfileCommandExecutionTest {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final String TARGET_BASE_DIR = "target/opt";
    private static final String SOLUTION_DIR = "target/dcc";

    @BeforeClass
    public static void setUp() throws IOException {
        File installationPath = new File(TARGET_BASE_DIR);
        if (installationPath.exists()) {
            FileUtils.deleteDirectory(installationPath);
        }
    }

    @Test
    public void testSimpleDeploymentInstallation() throws IOException {
        
        Profile profile = ProfileParser.parse(new File(SOLUTION_DIR, "dcc-test-deployment-profile.xml"));

        PropertiesHolder propertiesHolder = profile.createPropertiesHolder(true);

        profile.evaluate(propertiesHolder);

        propertiesHolder.dump();

        File targetBaseDir = new File(TARGET_BASE_DIR);
        if (!targetBaseDir.exists()) {
            targetBaseDir.mkdirs();
        }

        ExecutionContext executionContext =
            new ExecutionContext(new SSLConfiguration(
                    "target/dcc/config/dcc-shell.keystore", 
                    "DYKK8T8m9nKqBRPZ",
                    "target/dcc/config/dcc-shell.truststore", 
                    "DYKK8T8m9nKqBRPZ"));
        executionContext.setProfile(profile);
        executionContext.setSolutionDir(new File(SOLUTION_DIR));
        executionContext.setTargetBaseDir(targetBaseDir);
        executionContext.prepareForExecution();


        InitializeCommand initializeCommand = new InitializeCommand(executionContext);
        initializeCommand.execute(true, true);

        try {
            VerifyCommand verifyCommand = new VerifyCommand(executionContext);
            verifyCommand.execute(true, true);
        } catch (IllegalStateException e) {
            Assert.assertTrue(e.getSuppressed()[0].getMessage().contains("Execution of unit-04 failed"));
        }

        InstallCommand installCommand = new InstallCommand(executionContext);
        installCommand.execute(true, true);

        ConfigureCommand configureCommand = new ConfigureCommand(executionContext);
        configureCommand.execute(true, true);

        StartCommand startCommand = new StartCommand(executionContext);
        startCommand.execute(true, true);

        try {
            StopCommand stopCommand = new StopCommand(executionContext);
            stopCommand.execute(true, true);
        } catch (BuildException e) {
        }
    }

}
