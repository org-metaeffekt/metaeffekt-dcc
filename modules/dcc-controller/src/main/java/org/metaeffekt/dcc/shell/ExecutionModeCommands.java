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

import org.metaeffekt.dcc.controller.execution.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

/**
 * Command used to change the available execution modes while executing the shell.
 * 
 * @author Jochen K.
 */
@Component
public class ExecutionModeCommands implements CommandMarker {

    public static final String FAIL_ON_ERROR = "failOnError";
    public static final String FORCE = "force";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionModeCommands.class);

    private ExecutionContext executionContext;

    @Autowired
    public ExecutionModeCommands(ExecutionContext executionContext) {
        super();
        this.executionContext = executionContext;
    }

    @CliAvailabilityIndicator({ "mode activate", "mode deactivate", "mode status" })
    public boolean arePropertiesAvailable() {
        return true;
    }

    @CliCommand(value = "mode activate", help = "Activates the given execution mode.")
    public void activate(@CliOption(key = "name", mandatory = true, help = "The execution mode to activate.") final ExecutionMode mode) {
        handleMode(mode, true);
    }

    @CliCommand(value = "mode deactivate", help = "Deactivates the given execution mode.")
    public void deactivate(@CliOption(key = "name", mandatory = true, help = "The execution mode to deactivate.") final ExecutionMode mode) {
        handleMode(mode, false);
    }

    @CliCommand(value = "mode status", help = "Show the status off all existing execution modes.")
    public void status() {
        LOG.info("Current execution mode status:");
        LOG.info("[{}] [{}]", ExecutionMode.failOnError.getMode(), executionContext.isFailOnError());
        LOG.info("[{}] [{}]", ExecutionMode.force.getMode(), executionContext.isForce());
    }

    private void handleMode(ExecutionMode mode, boolean desiredState) {
        switch (mode) {
            case failOnError:
                LOG.info("Switching mode [{}] to [{}].", mode, desiredState);
                executionContext.setFailOnError(desiredState);
                break;
            case force:
                LOG.info("Switching mode [{}] to [{}].", mode, desiredState);
                executionContext.setForce(desiredState);
                break;
            default:
                LOG.warn("Unsupported execution mode [{}].", mode);
                break;
        }
    }

    enum ExecutionMode {
        failOnError("failOnError"),
        force("force");

        private String mode;

        private ExecutionMode(String mode) {
            this.mode = mode;
        }

        public String getMode() {
            return mode;
        }
    }

}
