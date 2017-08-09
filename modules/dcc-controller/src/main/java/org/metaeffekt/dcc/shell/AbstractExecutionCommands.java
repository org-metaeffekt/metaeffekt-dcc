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
package org.metaeffekt.dcc.shell;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;

import org.metaeffekt.dcc.commons.commands.Command;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.HostName;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.execution.Executor;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;

public abstract class AbstractExecutionCommands implements CommandMarker {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractExecutionCommands.class);
    
    protected final ExecutionContext executionContext;

    @Autowired
    public AbstractExecutionCommands(ExecutionContext executionContext) {
        super();
        this.executionContext = executionContext;
    }
    
    public boolean canExecuteProfile() {
        return executionContext.containsProfile() && executionContext.getProfile().getType() == Profile.Type.DEPLOYMENT;
    }

    protected void executeCommand(final Command command, final boolean force, final boolean parallel, final boolean forceInitializeExecutors, final String unitId) {
        executeCommand(command, force, parallel, forceInitializeExecutors, Id.createUnitId(unitId));
    }

    protected void executeCommand(final Command command, final boolean force, final boolean parallel, final boolean forceInitializeExecutors, final Id<UnitId> unitId) {
        try {
            // ensure the executionContext is ready for command execution
            prepareForExecution(command, forceInitializeExecutors);
            
            // execute the command
            command.execute(force || executionContext.isForce(), parallel, unitId);
            
            retrieveLogs(command, unitId);
        } catch (RuntimeException | IOException e) {
            LOG.error("Error executing command [{}]: {}", command.toString(), String.format("%n%n%s%n%n", e.getMessage()));
            retrieveLogs(command, unitId);
            if (executionContext.isFailOnError()) {
                LOG.error("Aborting shell execution ...", e);
                // that seems a bit harsh ... but the spring shell is surprisingly robust :-)
                System.exit(1);
            } else {
                LOG.debug(String.format("Continuing execution. Ignoring [%s] command execution exception.", command.toString()), e);
            }
        }
    }

    protected void prepareForExecution(final Command command, final boolean forceInitializeExecutors) {
        executionContext.prepareForExecution();
        if (!command.isLocal()) {
            executionContext.initializeExecutors(forceInitializeExecutors);
        }
    }

    protected void retrieveLogs(final Command command, final Id<UnitId> unitId) {
        if (!command.isLocal()) {
            for (Map.Entry<Id<HostName>, Executor> entry : executionContext.getHostsExecutors().entrySet()) {
                try {
                    if (entry.getValue() != null) {
                        entry.getValue().retrieveLogs();
                    }
                } catch (Exception ex) {
                    LOG.warn("Cannot retrieve logs for host [%s] ...", entry.getKey());
                }
            }
        }
    }

}
