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
package org.metaeffekt.dcc.shell;

import org.metaeffekt.dcc.commons.execution.Executor;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Proudly crafted by i001450 on 09.12.14.
 */
@Component
public class LoggingCommands implements CommandMarker {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionModeCommands.class);
    
    private ExecutionContext executionContext;

    @Autowired
    public LoggingCommands(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    @CliCommand(value = "retrieve agent-logs", help = "Retrieve the agent logs.")
    public void retrieveAgentLogs() {
        if (executionContext == null || executionContext.getHostsExecutors().isEmpty()) {
            LOG.info("No executors to retrieve logs from.");
        }

        Collection<Executor> executors = executionContext.getHostsExecutors().values();
        
        for (Executor executor : executors) {
            LOG.info("Retrieving logs from [{}].", executor.getDisplayName());
            executor.retrieveLogs();
        }
    }
}
