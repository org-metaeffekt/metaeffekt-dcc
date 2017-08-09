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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import org.metaeffekt.dcc.controller.execution.ExecutionContext;

@Component
public class AssertCommands implements CommandMarker {

    private static final Logger LOG = LoggerFactory.getLogger(AssertCommands.class);
    
    private final ExecutionContext executionContext;
    
    @Autowired
    public AssertCommands(ExecutionContext executionContext) {
        super();
        this.executionContext = executionContext;
    }
    
    @CliAvailabilityIndicator({"assert stopped"})
    public boolean arePropertiesAvailable() {
        return executionContext.containsProfile();
    }
    
    @CliCommand(value = "assert stopped", help = "Asserts that all processes and daemons of the deployment have been stopped.")
    public void assertStopped(@CliOption(key = "timeout", mandatory = false, unspecifiedDefaultValue = "10", help = "Timeout for the assert in seconds. Default is 10.") int timeout) {
        // IDEA: check for status that all units have stopped; as long as the units have not stopped
        //   wait a few seconds and check again. Do this until the timeout has elapsed. Throw an
        //   exception in case the assertion is not met after the timeout.

        LOG.info("Checking assertion [stopped].");
        // for the time being we wait for the timeout
        try {
            Thread.sleep(timeout * 1000);
            LOG.info("Checking assertion timed out.");
        } catch (InterruptedException e) {
            LOG.info("Checking assertion interrupted.");
        }
    }

}
