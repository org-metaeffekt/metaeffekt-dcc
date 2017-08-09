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
package org.metaeffekt.dcc.agent;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.DccUtils;
import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;
import org.metaeffekt.dcc.commons.domain.Type.PackageId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.execution.ExecutionStateHandler;
import org.metaeffekt.dcc.commons.script.CommandScriptExecutionContext;
import org.metaeffekt.dcc.commons.script.CommandScriptExecutor;

public class AgentScriptExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(AgentScriptExecutor.class);
    
    private File workingBaseDir;

    private File destinationBaseDir;
    
    private final Object semaphore = new Object();

    public AgentScriptExecutor(File workingBaseDir, File targetBaseDir) {
        this.workingBaseDir = notNull(workingBaseDir, "workingBaseDir");
        this.destinationBaseDir = notNull(targetBaseDir, "destinationBaseDir");
        
        LOG.info("DCC Agent initialized with workingBaseDir [{}] and destinationBaseDir [{}]", 
            getWorkingBaseDir(), getDestinationBaseDir());

    }

    protected ExecutionStateHandler getExecutionStateHandler(Id<DeploymentId> deploymentId) {
        return new ExecutionStateHandler(new File(destinationBaseDir, deploymentId.getValue()),
                new File(workingBaseDir, deploymentId.getValue()));
    }

    private <T> T notNull(T obj, String label) {
        if (obj == null) {
            throw new IllegalArgumentException(
                String.format("No [%s] parameter found in request", label));
        }
        return obj;
    }

    public File getWorkingBaseDir() {
        return workingBaseDir;
    }

    public void setWorkingBaseDir(File workingBaseDir) {
        this.workingBaseDir = workingBaseDir;
    }

    public void setDestinationBaseDir(File destinationBaseDir) {
        this.destinationBaseDir = destinationBaseDir;
    }

    public File getWorkingDir(Id<DeploymentId> deploymentId) {
        return new File(getWorkingBaseDir(), deploymentId.getValue());
    }

    public File getDestinationBaseDir() {
        return destinationBaseDir;
    }

    protected File getDestinationDir(Id<DeploymentId> deploymentId) {
        return new File(getDestinationBaseDir(), deploymentId.getValue());
    }

    public File getSolutionDir(Id<DeploymentId> deploymentId) {
        return new File(getWorkingDir(deploymentId), "solution");
    }

    public File getTmpDir(Id<DeploymentId> deploymentId) {
        return new File(getWorkingDir(deploymentId), DccConstants.TMP_SUB_DIRECTORY);
    }
    
    public void prepareFilesystemLocations(Id<DeploymentId> deploymentId) {
        DccUtils.prepareFoldersForWriting(
                getDestinationBaseDir(),
                getDestinationDir(deploymentId),
                getWorkingBaseDir(),
                getWorkingDir(deploymentId),
                getSolutionDir(deploymentId),
                getTmpDir(deploymentId));
    }

    public void cleanFilesystemLocations(Id<DeploymentId> deploymentId) throws IOException {
        final ExecutionStateHandler executionStateHandler = getExecutionStateHandler(deploymentId);
        
        // clean is symmetric to initialize. as such it affects only the solution folders
        File[] files = new File[] {
            executionStateHandler.getStateCacheDirectory(),
            getTmpDir(deploymentId),
            getWorkingDir(deploymentId),
        };

        for (File file: files) {
            if (file.exists()) {
                FileUtils.cleanDirectory(file);
            }
        }
    }

    public void purgeFilesystemLocations(Id<DeploymentId> deploymentId) throws IOException {
        final ExecutionStateHandler executionStateHandler = getExecutionStateHandler(deploymentId);

        // purge is affecting the destination of the deployment and the meta data about the deployment
        File[] files = new File[] {
            executionStateHandler.getStateCacheDirectory(),
            executionStateHandler.getConfigurationDirectory(),
            getTmpDir(deploymentId),
            getDestinationDir(deploymentId)
        };

        for (File file: files) {
            if (file.exists()) {
                FileUtils.deleteDirectory(file);
            }
        }
    }
    
    public void executeScript(Id<DeploymentId> deploymentId, Id<PackageId> packageId,
            Id<UnitId> unitId, String commandString, File executionPropertiesFile,
            File prerequisitesPropertiesFile) {
        synchronized (semaphore) {
            LOG.info("Received PUT request for command [{}], unit [{}], package [{}].", commandString, unitId, packageId);
    
            DccUtils.prepareFoldersForWriting(getWorkingDir(deploymentId));
    
            Validate.notNull(executionPropertiesFile, "Execution properties must not be null!");
            Validate.notNull(deploymentId, "Deployment id must not be null!");
            Validate.notNull(unitId, "Unit id must not be null!");
            Validate.notNull(packageId, "Package id must not be null!");
    
            File localSolutionDirectory = getSolutionDir(deploymentId);
            File packageDir = new File(localSolutionDirectory, "packages");
            File installationDir = getDestinationDir(deploymentId);
    
            CommandScriptExecutionContext executionContext = new CommandScriptExecutionContext();
            executionContext.setTargetDir(installationDir);
            executionContext.setPackageDir(packageDir);
            executionContext.setSolutionDir(localSolutionDirectory);
            executionContext.setDeploymentId(deploymentId);
            executionContext.setWorkingDir(getWorkingDir(deploymentId));
    
            File agentHomeDir = new File(System.getProperty("dcc.agent.home"), "./");
            File antHomeDir = determineAntHome(agentHomeDir);
            executionContext.setAntHomeDir(antHomeDir);
    
            CommandScriptExecutor executor = new CommandScriptExecutor(executionContext, 
                    getExecutionStateHandler(deploymentId));
            final Commands command = Commands.parseConfigurableCommand(commandString);
            try {
                executor.executeCommand(command, unitId, packageId, executionPropertiesFile, 
                        prerequisitesPropertiesFile);
            } catch (BuildException e) {
                LOG.error(String.format("Failed to execute command %s on unit %s", commandString, unitId), e);
                throw e;
            }
        }
    }

    private File determineAntHome(File agentHomeDir) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(agentHomeDir);
        scanner.setIncludes(new String[] {"tools/apache-ant-*"});
        scanner.scan();
        
        final String[] foundDirs = scanner.getIncludedDirectories();
        if (foundDirs != null && foundDirs.length == 1) {
            return new File(agentHomeDir, foundDirs[0]);
        }
        return null;
    }

}
