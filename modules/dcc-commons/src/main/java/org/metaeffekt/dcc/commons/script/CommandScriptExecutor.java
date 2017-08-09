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
package org.metaeffekt.dcc.commons.script;

import static org.metaeffekt.dcc.commons.DccConstants.ANT_FILE;
import static org.metaeffekt.dcc.commons.DccConstants.ANT_PROJECT_HELPER;
import static org.metaeffekt.dcc.commons.DccProperties.DCC_COMMAND;
import static org.metaeffekt.dcc.commons.DccProperties.DCC_DEPLOYMENT_ID;
import static org.metaeffekt.dcc.commons.DccProperties.DCC_EXECUTION_PROPERTIES;
import static org.metaeffekt.dcc.commons.DccProperties.DCC_JAVA_HOME;
import static org.metaeffekt.dcc.commons.DccProperties.DCC_PACKAGE_DIR;
import static org.metaeffekt.dcc.commons.DccProperties.DCC_PACKAGE_ID;
import static org.metaeffekt.dcc.commons.DccProperties.DCC_PREREQUISITES_PROPERTIES;
import static org.metaeffekt.dcc.commons.DccProperties.DCC_SOLUTION_DIR;
import static org.metaeffekt.dcc.commons.DccProperties.DCC_TARGET_DIR;
import static org.metaeffekt.dcc.commons.DccProperties.DCC_UNIT_ID;
import static org.metaeffekt.dcc.commons.DccProperties.DCC_UPGRADE_PROPERTIES;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.Commandline.Argument;
import org.apache.tools.ant.types.Environment.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.metaeffekt.dcc.commons.DccUtils;
import org.metaeffekt.dcc.commons.ant.ProjectAdapter;
import org.metaeffekt.dcc.commons.ant.PropertyUtils;
import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.PackageId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.execution.ExecutionStateHandler;
import org.metaeffekt.dcc.commons.properties.SortedProperties;

public class CommandScriptExecutor {
    
    private static final Logger LOG = LoggerFactory.getLogger(CommandScriptExecutor.class);

    private CommandScriptExecutionContext executionContext;
    private ExecutionStateHandler executionStateHandler;

    public CommandScriptExecutor(CommandScriptExecutionContext executionContext, ExecutionStateHandler executionStateHandler) {
        this.executionContext = executionContext;
        this.executionStateHandler = executionStateHandler;
    }

    public void executeCommand(Commands command, Id<UnitId> unitId, Id<PackageId> packageId,
            File executionPropertiesFile, File prerequisitesPropertiesFile) {
        
        // check the file exists (prevent race condition)
        if (executionPropertiesFile != null && !executionPropertiesFile.exists()) {
            throw new IllegalStateException(
                String.format("The required file '%s' does not exist.", executionPropertiesFile));
        }

        // support for BeforeAspect - script executed before the package command script
        executePreCommandScript(command, unitId, packageId, executionPropertiesFile,
                prerequisitesPropertiesFile);

        //executed the package command script, or InsteadOfAspect if any
        executeMainCommandScript(command, unitId, packageId, executionPropertiesFile,
                prerequisitesPropertiesFile);

        // support for AfterAspect - script executed after the package command script
        executePostCommandScript(command, unitId, packageId, executionPropertiesFile,
                prerequisitesPropertiesFile);

        if (executionStateHandler != null) {
            executionStateHandler.persistStateAfterSuccessfulExecution(unitId, command, executionPropertiesFile);
        }
    }

    private void executePreCommandScript(Commands command, Id<UnitId> unitId,
            Id<PackageId> packageId, File executionPropertiesFile, File prerequisitesPropertiesFile) {
        final File scriptFile =
            DccUtils.getPreCommandScriptFile(executionContext.getSolutionDir(), unitId, command);
        if (scriptFile.exists()) {
            executeCommandScript(command, scriptFile, unitId, packageId, executionPropertiesFile,
                    prerequisitesPropertiesFile);
        }
    }

    private void executeMainCommandScript(Commands command, Id<UnitId> unitId,
            Id<PackageId> packageId, File executionPropertiesFile, File prerequisitesPropertiesFile) {
        File scriptFile;

        final File insteadOfScriptFile =
            DccUtils.getInsteadOfCommandScriptFile(executionContext.getSolutionDir(), unitId,
                    command);
        final File packageScriptFile =
            DccUtils.getCommandScriptFile(executionContext.getPackageDir(), packageId, command);
        if (insteadOfScriptFile.exists() && insteadOfScriptFile.isFile()) {
            scriptFile = insteadOfScriptFile;
        } else {
            scriptFile = packageScriptFile;
        }

        executeCommandScript(command, scriptFile, unitId, packageId, executionPropertiesFile,
                prerequisitesPropertiesFile);
    }

    private void executePostCommandScript(Commands command, Id<UnitId> unitId,
            Id<PackageId> packageId, File executionPropertiesFile, File prerequisitesPropertiesFile) {
        final File scriptFile =
            DccUtils.getPostCommandScriptFile(executionContext.getSolutionDir(), unitId, command);
        if (scriptFile.exists()) {
            executeCommandScript(command, scriptFile, unitId, packageId, executionPropertiesFile,
                    prerequisitesPropertiesFile);
        }
    }

    private void executeCommandScript(Commands command, File scriptFile, Id<UnitId> unitId,
            Id<PackageId> packageId, File executionPropertiesFile, File prerequisitesPropertiesFile) {
        if (determineAntHome() == null) {
            executeCommandScriptSameJvm(command, scriptFile, unitId, packageId,
                    executionPropertiesFile, prerequisitesPropertiesFile);
        } else {
            executeCommandScriptForked(command, scriptFile, unitId, packageId,
                    executionPropertiesFile, prerequisitesPropertiesFile);
        }
    }

    private void executeCommandScriptSameJvm(Commands command, File scriptFile,
            final Id<UnitId> unitId, Id<PackageId> packageId,
            final File executionPropertiesFile, File prerequisitesPropertiesFile) {

        final File packageDir = new File(executionContext.getPackageDir(), packageId.getValue());

        final Project project = new ProjectAdapter();
        try {
            project.setUserProperty(ANT_FILE, scriptFile.getAbsolutePath());

            // all executions are relative to the package
            project.setBaseDir(packageDir);

            if (executionPropertiesFile != null) {
                project.setProperty(DCC_EXECUTION_PROPERTIES,
                        executionPropertiesFile.getAbsolutePath());
            }

            if (prerequisitesPropertiesFile != null) {
                project.setProperty(DCC_PREREQUISITES_PROPERTIES,
                        prerequisitesPropertiesFile.getAbsolutePath());
            }
            
            if (executionContext.getUpgradePropertiesFile() != null) {
                project.setProperty(DCC_UPGRADE_PROPERTIES, 
                        executionContext.getUpgradePropertiesFile().getAbsolutePath());
            }

            project.setProperty(DCC_COMMAND, command.toString());
            project.setProperty(DCC_TARGET_DIR, executionContext.getTargetDir().getAbsolutePath());
            project.setProperty(DCC_SOLUTION_DIR, executionContext.getSolutionDir()
                    .getAbsolutePath());
            project.setProperty(DCC_PACKAGE_DIR, executionContext.getPackageDir().getAbsolutePath());

            project.setProperty(DCC_UNIT_ID, unitId.getValue());
            project.setProperty(DCC_PACKAGE_ID, packageId.getValue());
            project.setProperty(DCC_JAVA_HOME, System.getProperty("java.home"));
            project.setProperty(DCC_DEPLOYMENT_ID, executionContext.getDeploymentId().getValue());

            project.init();

            final ProjectHelper helper = ProjectHelper.getProjectHelper();
            project.addReference(ANT_PROJECT_HELPER, helper);
            helper.parse(project, scriptFile);

            // the project helper modifies the projects baseDir; we ensure it points to the right
            // spot
            project.setBaseDir(packageDir);

            // preserve properties to avoid side-effects in between script executions
            final Properties originalSystemProperties = isolateSystemProperties();
            try {
                // execute the scripts default target
                project.executeTarget(project.getDefaultTarget());
            } finally {
                // restore original properties
                System.setProperties(originalSystemProperties);
            }
        } finally {
            //cleans the loaded classes
            project.fireBuildFinished(null);
        }
    }

    private void executeCommandScriptForked(Commands command, File scriptFile, final Id<UnitId> unitId, Id<PackageId> packageId,
            final File executionPropertiesFile, File prerequisitesPropertiesFile) {

        // produce a properties file that serves as input for the forked ant execution
        Properties properties = new SortedProperties();
        properties.setProperty(DCC_COMMAND, command.toString());
        properties.setProperty(DCC_TARGET_DIR, executionContext.getTargetDir().getAbsolutePath());
        properties.setProperty(DCC_UNIT_ID, unitId.getValue());
        properties.setProperty(DCC_SOLUTION_DIR, executionContext.getSolutionDir().getAbsolutePath());
        properties.setProperty(DCC_PACKAGE_ID, packageId.getValue());
        properties.setProperty(DCC_PACKAGE_DIR, executionContext.getPackageDir().getAbsolutePath());
        properties.setProperty(DCC_JAVA_HOME, System.getProperty("java.home"));
        properties.setProperty(DCC_DEPLOYMENT_ID, executionContext.getDeploymentId().getValue());
        
        if (executionPropertiesFile != null) {
            properties.setProperty(DCC_EXECUTION_PROPERTIES, executionPropertiesFile.getAbsolutePath());
        }

        if (prerequisitesPropertiesFile != null) {
            properties.setProperty(DCC_PREREQUISITES_PROPERTIES, prerequisitesPropertiesFile.getAbsolutePath());
        }
        
        if (executionContext.getUpgradePropertiesFile() != null) {
            properties.setProperty(DCC_UPGRADE_PROPERTIES, executionContext.getUpgradePropertiesFile().getAbsolutePath());
        }

        // write the file to the file system to be ready for later
        String filename = unitId + "." + scriptFile.getName();
        filename = filename.replace(".xml", "_exec.properties");
        final File execPropertiesDir = new File(getExecutionContext().getWorkingDir(), "exec");
        File execProperties = new File(execPropertiesDir, filename);
        execProperties.getParentFile().mkdirs();
        
        try {
            PropertyUtils.writeToFile(properties, execProperties, "Properties exported for script " + scriptFile);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot export properties for script " + scriptFile, e);
        }
        
        final File packageDir = new File(executionContext.getPackageDir(), packageId.getValue());
        final Project project = new ProjectAdapter();
        
        // all executions are relative to the package
        project.setBaseDir(packageDir);
        
        ExecTask exec = new ExecTask();
        exec.setProject(project);
        exec.setOutputproperty("exec.output");
        exec.setErrorProperty("exec.error");
        exec.setDir(packageDir);
        exec.setFailIfExecutionFails(true);
        exec.setResultProperty("exec.result");

        File antHome = determineAntHome();
        File antBin = determineAntExecutable(antHome);
        exec.setExecutable(antBin.getAbsolutePath());

        // provide ANT_HOME (may be not available or set to some other ANT causing unexpected issues)
        final Variable antHomeVar = new Variable();
        antHomeVar.setKey("ANT_HOME");
        antHomeVar.setValue(antHome.getPath());
        exec.addEnv(antHomeVar);

        // supply the JAVA_HOME (using the java the curren jvm runs with)
        final Variable javaHomeVar = new Variable();
        javaHomeVar.setKey("JAVA_HOME");
        javaHomeVar.setValue(System.getProperty("java.home"));
        exec.addEnv(javaHomeVar);

        // configure log4j for forked ant
        try {
            final URL log4jFileUrl = getClass().getResource("/log4j2.xml");
            if (log4jFileUrl != null) {
                File file = new File(log4jFileUrl.getFile());
                String log4jConfig = file.getAbsolutePath();
                File parentFile = file.getParentFile();
                File scriptLog4JFile = new File(parentFile, "log4j2-script-execution.xml");
                if (scriptLog4JFile.exists()) {
                    log4jConfig = scriptLog4JFile.getAbsolutePath();
                }
                LOG.debug("Log4j2 configuration detected: " + log4jConfig);
                final Variable antOptsVar = new Variable();
                antOptsVar.setKey("ANT_OPTS");
                antOptsVar.setValue("-Dlog4j.configurationFile=" + log4jConfig);
                exec.addEnv(antOptsVar);
            } else {
                LOG.warn("Log4j2 configuration not detected. Script execution may report errors initializing logging.");
            }
        } catch (RuntimeException e) {
            LOG.error("Cannot configure ant to use log4j.", e);
        }

        Argument scriptFileArg = exec.createArg();
        String sfLine = "-f " + scriptFile.getAbsolutePath();
        scriptFileArg.setDescription(sfLine);
        scriptFileArg.setLine(sfLine);
        
        Argument propertyFileArg = exec.createArg();
        final String pfLine = "-propertyfile " + execProperties.getAbsolutePath();
        propertyFileArg.setLine(pfLine);
        propertyFileArg.setDescription(pfLine);
        
        // preserve properties to avoid side-effects in between script executions
        final Properties originalSystemProperties = isolateSystemProperties();
        try {
            // execute the scripts default target
            LOG.info("Executing [{}] [{}] [{}]", antBin.getAbsolutePath(), 
                scriptFileArg.getDescription(), propertyFileArg.getDescription());
            exec.execute();
        } finally {
            // restore original properties
            System.setProperties(originalSystemProperties);
        }
        
        final String output = project.getProperty("exec.output");
        logOutput(output, false);
        
        final String errorContent = project.getProperty("exec.error");
        
        String[] lines = errorContent.split("\\r?\\n");
        
        for (int i = 0; i < lines.length; i++) {
            final boolean error = lines[i].startsWith("BUILD FAILED");
            // NOTE the error content alone is not sufficient. It may be an intermediate error from
            //   a script embedded exec call (with failonerror="false"). It is important that the
            //   line starts with "BUILD FAILED".
            if (error) {
                if (!StringUtils.isBlank(errorContent)) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = i + 1; j < lines.length; j++) {
                        if (!StringUtils.isBlank(lines[j])) {
                            if (!lines[j].trim().startsWith("Total time:")) {
                                if (j != i + 1) {
                                    sb.append("\n");
                                }
                                sb.append(lines[j].trim());
                            }
                        }
                    }
                    throw new BuildException(sb.toString());
                }
            }
        }
        
    }

    private File determineAntExecutable(File antHome) {
        File antBin = new File(antHome, "bin");
        String osName = System.getProperty("os.name");
        if (osName != null && osName.toLowerCase().indexOf("windows") != -1) {
            antBin = new File(antBin, "ant.bat");
        } else {
            antBin = new File(antBin, "ant");
        }
        return antBin;
    }

    private File determineAntHome() {
        File antHome = executionContext.getAntHomeDir();
        if (antHome == null) {
            // interpret as: execute in local jvm; do not resolve separate ant executable
        } else {
            if (!antHome.exists()) {
                LOG.warn("Ant was not found at configured location: [{}]", antHome);
                final String antHomeEnv = System.getenv("ANT_HOME");
                LOG.warn("Trying fallback to ANT_HOME provided by environment: [{}]", antHomeEnv);
                if (antHomeEnv != null) {
                    antHome = new File(antHomeEnv);
                } else {
                    antHome = null;
                }
                if (antHome != null && !antHome.exists()) {
                    LOG.warn("Ant not found at location: [{}]", antHome);
                    return null;
                }
            }
        }
        return antHome;
    }

    private String logOutput(String output, boolean errorLevel) {
        if (!StringUtils.isEmpty(output)) {
            output = output.replaceAll("\\r", "");
            boolean first = true;
            for (String line : output.split("\\n")) {
                if (!first) {
                    if (errorLevel) {
                        LOG.error(line);
                    } else {
                        LOG.info(line);
                    }
                }
                first = false;
            }
        }
        return output;
    }

    private static final Properties isolateSystemProperties() {
        final Properties originalSystemProperties = System.getProperties();
        final Properties tmpSystemProperties = new SortedProperties();
        tmpSystemProperties.putAll(originalSystemProperties);
        System.setProperties(tmpSystemProperties);
        return originalSystemProperties;
    }

    public CommandScriptExecutionContext getExecutionContext() {
        return executionContext;
    }

    public void setExecutionContext(CommandScriptExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

}
