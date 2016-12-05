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

import java.util.Collection;

import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;
import org.metaeffekt.dcc.commons.domain.Type.HostName;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.execution.Executor;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;

/**
 * @author Alexander D.
 * @author Jochen K.
 */
public abstract class AbstractHostBasedCommand extends AbstractCommand {

    public AbstractHostBasedCommand(ExecutionContext executionContext) {
        super(executionContext);
    }

    @Override
    protected void doExecute(boolean force, final boolean parallel, Id<UnitId> unitId) {
        LOG.info("Executing command [{}] ...", getCommandVerb());
        if (unitId == null) {
            // do for all hosts
            Collection<Id<HostName>> hosts = getExecutionContext().getHostsExecutors().keySet();
            if (parallel) {
                hosts.parallelStream().forEach(h -> doExecuteForHost(h, force));
            } else {
                hosts.stream().forEach(h -> doExecuteForHost(h, force));
            }
        } else {
            // reduce the handling to the host hosting the unit
            Id<HostName> host = getExecutionContext().getHostForUnit(unitId);
            if (host != null) {
                doExecuteForHost(host, force);
            } else {
                throw new IllegalArgumentException(String.format("Cannot find host based executor for unit [%s]. "
                    + "Either the unit does not exists or is not bound to a host.", unitId));
            }
        }
    }

    private void doExecuteForHost(Id<HostName> host, boolean force) {
        if (isExecutionRequired(force, host, getCommandVerb())) {
            doExecuteCommand(getExecutionContext().getExecutorForHost(host));
        }
        updateStatus(host);
    }

    protected abstract void doExecuteCommand(Executor executor);

    private boolean isExecutionRequired(boolean force, Id<HostName> host, Commands command) {
        if (force) return true;
        if (!allowsToBeSkipped()) {
            return false;
        }
        updateStatus(host);
        final Id<DeploymentId> deploymentId = getExecutionContext().getProfile().getDeploymentId();
        return getExecutionStateHandler().alreadySuccessfullyExecuted(host, command, host, deploymentId);
    }

    private void updateStatus(Id<HostName> host) {
        getExecutionContext().getExecutorForHost(host).retrieveUpdatedState();
    }
    
    protected void afterSuccessfulUnitExecution(long startTimestamp) {
        super.afterSuccessfulExecution("", String.format("[%s]", getCommandVerb()), startTimestamp);
    }
}
