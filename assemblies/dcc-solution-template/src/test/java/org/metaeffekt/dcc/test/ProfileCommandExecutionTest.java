package org.metaeffekt.dcc.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.metaeffekt.dcc.commons.ant.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.BuildException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.spring.xml.ProfileParser;
import org.metaeffekt.dcc.controller.commands.ConfigureCommand;
import org.metaeffekt.dcc.controller.commands.InstallCommand;
import org.metaeffekt.dcc.controller.commands.StartCommand;
import org.metaeffekt.dcc.controller.commands.StopCommand;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;
import org.metaeffekt.dcc.controller.execution.SSLConfiguration;

public class ProfileCommandExecutionTest {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final String TARGET_BASE_DIR = "target/opt";
    private static final String SOLUTION_DIR = "target/dcc";
    private static final String EQUALS = "=";
    private static final String RUNTIME_HSQLDB_AUDIT_PROPERTIES = "hsqldb-runtime-audit/install.properties";
    private static final String RUNTIME_HSQLDB_DEFAULT_PROPERTIES = "hsqldb-runtime-default/install.properties";

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

        InstallCommand installCommand = new InstallCommand(executionContext);
        installCommand.execute(true);

        File configurationTargetPath = new File(executionContext.getTargetDir(), "config");
        
        ConfigureCommand configureCommand = new ConfigureCommand(executionContext);
        configureCommand.execute(true);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            // ignore
        }

        StartCommand startCommand = new StartCommand(executionContext);
        startCommand.execute(true);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            // ignore
        }
        
        try {
            StopCommand stopCommand = new StopCommand(executionContext);
            stopCommand.execute(true);
        } catch (BuildException e) {
        }
    }

}
