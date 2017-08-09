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
package org.metaeffekt.dcc.controller.execution;

import static org.metaeffekt.dcc.commons.commands.Commands.CLEAN;
import static org.metaeffekt.dcc.commons.commands.Commands.INITIALIZE;
import static org.metaeffekt.dcc.commons.commands.Commands.PURGE;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.DccProperties;
import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;
import org.metaeffekt.dcc.commons.domain.Type.HostName;
import org.metaeffekt.dcc.commons.execution.Executor;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;

/**
 * @author Alexander D.
 */
public class LocalExecutor extends BaseExecutor implements Executor {

    private static final Logger LOG = LoggerFactory.getLogger(LocalExecutor.class);

    public static final String LOCAL_DEPLOYMENT_TARGET_DIR = DccProperties.DCC_LOCAL_DESTINATION_DIR;
    public static final String ENV_LOCAL_DEPLOYMENT_TARGET_DIR = LOCAL_DEPLOYMENT_TARGET_DIR.toUpperCase().replace(".", "_");

    private final Object semaphore = new Object();
    
    public LocalExecutor(ExecutionContext executionContext, boolean mixedMode) {
        super(executionContext);

        // FIXME: we need to move this code into the executor itself
        if (getExecutionContext().getTargetBaseDir() == null) {
            if (!mixedMode) {
                // we support that the local destination dir can be specified in the deployment properties
                String localDestinationDir = null;
                final Properties deploymentProperties = getExecutionContext().getDeploymentProperties();
                if (deploymentProperties != null) {
                    localDestinationDir = deploymentProperties.getProperty(LOCAL_DEPLOYMENT_TARGET_DIR);
                }
                // In case the value is not set in the deployment properties, look in the environment for developer convenience
                if (localDestinationDir == null) {
                    localDestinationDir = System.getenv(ENV_LOCAL_DEPLOYMENT_TARGET_DIR);
                }

                if (localDestinationDir == null) {
                    LOG.debug("Cannot determine the target location of the deployment. Developers can provide a default value " +
                            "by providing the environment variable [" + ENV_LOCAL_DEPLOYMENT_TARGET_DIR + "]. Bear in mind, " +
                            "it will be overwritten by the setting in the deployment properties.");
                    // in case deployment properties are specified and the above property is not
                    // set an exception is raised
                    IllegalStateException e = new IllegalStateException(
                            "Cannot determine the target location of the deployment. Make sure to "
                                    + "specify the property [" + LOCAL_DEPLOYMENT_TARGET_DIR + "] in the "
                                    + "deployment properties to enable local deployments.");
                    throw e;
                }
                getExecutionContext().setTargetBaseDir(new File(localDestinationDir));
            } else {
                // mixed mode means that all target host are contacted using the agent (remote). Local commands
                // nevertheless use the LocalExecutor.

                // in order to provide all required paths for temporary folders and status tracking the following
                // setting is used
                final File workDir = new File(executionContext.getSolutionDir(), DccConstants.WORK_SUB_DIRECTORY);
                final File workTargetDir = new File(workDir, "local");
                getExecutionContext().setTargetBaseDir(workTargetDir);
            }
        }
    }

    @Override
    public void purge() {
        synchronized (semaphore) {
            logCommand(PURGE);
            cleanFolders(getExecutionContext().getTargetDir());
        }
    }

    @Override
    public void clean() {
        synchronized (semaphore) {
            logCommand(CLEAN);
            // FIXME: consolidate with base implementations. Can't this code be completely moved up
            cleanFolders(getExecutionStateHandler().getStateCacheDirectory());
            super.clean();
        }
    }

    @Override
    public void initialize() {
        synchronized (semaphore) {
            logCommand(INITIALIZE);
            initializeLocalFolders();
        }
    }

    @Override
    public boolean hostAvailable() {
        return true;
    }

    @Override
    public void retrieveUpdatedState() {
        synchronized (semaphore) {
            final Id<HostName> host = Id.createHostName("localhost");
            final Id<DeploymentId> deploymentId = getExecutionContext().getProfile().getDeploymentId();
            InputStream consolidatedState = getExecutionStateHandler().consolidateState(deploymentId);
            getExecutionStateHandler().updateConsolidatedState(consolidatedState, host, deploymentId);
        }
    }

    @Override
    public void retrieveLogs() {
    }

    public void execute(Commands command, ConfigurationUnit unit) {
        executeLocal(command, unit);
    }

    private void logCommand(Commands command) {
        LOG.info("  Executing command [{}] on [localhost].", command);
    }

    @Override
    protected void logCommand(Commands command, ConfigurationUnit unit) {
        String unitId = (unit == null ? "none" : unit.getId().getValue());
        LOG.info("  Executing command [{}] for unit [{}] on [localhost].", command, unitId);
    }

    @Override
    public void initializeUpgrade() {
        throw new UnsupportedOperationException("This operartion is not yet supported.");
    }
    
    public String getDisplayName() {
        return "localhost";
    }

}
