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

import java.util.Map.Entry;
import java.util.Properties;

import org.metaeffekt.dcc.controller.execution.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.stereotype.Component;

/**
 * {@link CommandMarker} instance which holds all commands that can be executed on the shell for 
 * displaying properties associated with the selected profile ({@code list solution properties}, 
 * {@code list deployment properties}, {@code list all properties}). 
 */
@Component
public class PropertyCommands implements CommandMarker {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyCommands.class);

    private final ExecutionContext executionContext;
    
    @Autowired
    public PropertyCommands(ExecutionContext executionContext) {
        super();
        this.executionContext = executionContext;
    }
    
    @CliAvailabilityIndicator({"list deployment properties", "list solution properties", "list all properties"})
    public boolean arePropertiesAvailable() {
        return executionContext.containsProfile();
    }
    
    @CliCommand(value = "list deployment properties", help = "list the deployment properties currently loaded.")
    public void listDeploymentProperties() {
        LOG.info("Deployment Properties:");
        if (arePropertiesAvailable()) {
            Properties deploymentProperties = executionContext.getDeploymentProperties();
            if (deploymentProperties.isEmpty()) {
                LOG.info("Deployment properties are empty.");
            } else {
                for (Entry<Object, Object> entry : deploymentProperties.entrySet()) {
                    LOG.info(entry.getKey() + "=" + entry.getValue());
                }
            }
        } else {
            LOG.info("No profile is currently loaded.");
        }
    }

    @CliCommand(value = "list solution properties", help = "List the solution properties currently loaded.")
    public void listSolutionProperties() {
        LOG.info("Solution Properties:");
        if (arePropertiesAvailable()) {
            Properties solutionProperties = executionContext.getSolutionProperties();
            if (solutionProperties.isEmpty()) {
                LOG.info("Solution properties are empty.");
            } else {
                for (Entry<Object, Object> entry : solutionProperties.entrySet()) {
                    LOG.info(entry.getKey() + "=" + entry.getValue());
                }
            }
        } else {
            LOG.info("No profile is currently loaded.");
        }
    }
    
    @CliCommand(value = "list all properties", help = "List all properties currently loaded.")
    public void listAllProperties() {
        if (arePropertiesAvailable()) {
            listSolutionProperties();
            listDeploymentProperties();
        } else {
            LOG.info("No profile is currently loaded.");
        }
    }
    
}
