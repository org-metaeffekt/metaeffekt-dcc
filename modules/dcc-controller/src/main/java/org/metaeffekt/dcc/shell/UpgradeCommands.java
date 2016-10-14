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

import static org.metaeffekt.dcc.shell.Constants.HELP_FORCE;
import static org.metaeffekt.dcc.shell.Constants.HELP_UNITID;

import java.io.File;

import org.apache.tools.ant.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import org.metaeffekt.dcc.commons.ant.UpgradePropertiesTask;
import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.controller.commands.NamedCommand;
import org.metaeffekt.dcc.controller.commands.UpgradeResourcesCommand;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;
import org.metaeffekt.dcc.shell.converters.UnitCapabilityIdConverter;

/**
 * {@link CommandMarker} instance which holds all commands that can be executed on the shell for 
 * displaying properties associated with the selected profile ({@code list solution properties}, 
 * {@code list deployment properties}, {@code list all properties}). 
 */
@Component
public class UpgradeCommands extends AbstractExecutionCommands {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileShellCommands.class);

    @Autowired
    public UpgradeCommands(ExecutionContext executionContext) {
        super(executionContext);
    }
    
    @CliAvailabilityIndicator(
        {
            "upgrade properties" 
        })
    public boolean arePropertiesAvailable() {
        return true;
    }
    
    /**
     * Provides the availability indicator for all commands.
     * 
     * @return Returns <code>true</code> in case the command can be applied.
     */
    @Override
    @CliAvailabilityIndicator(
        {
            "upgrade resources",
            
            "execute upgrade-prepare", 
            "execute upgrade", 
            "execute upgrade-finalize",
        }
    )
    public boolean canExecuteProfile() {
        return super.canExecuteProfile();
    }
    
    @CliCommand(value = "upgrade properties", help = "Derives solution and deployment property files from an existing deployment.")
    public void upgradeProperties
        (@CliOption(key = "location", mandatory = true, help = "The location of the upgrade properties, which contain the necessary configuration.") File upgradePropertiesFile) {
        
        // upgrade the deployment and solution property files
        LOG.info("Upgrade properties executing with file [{}]." + upgradePropertiesFile);
        UpgradePropertiesTask.apply(new Project(), upgradePropertiesFile);
    }
    
    @CliCommand(value = "upgrade resources", help = "Upgrades the binaries and files from the previous solution to the new solution.")
    public void upgradeResources(
            @CliOption(key = "location", mandatory = true, help = "The location of the upgrade properties, which contain the necessary configuration.") File upgradePropertiesFile,
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",  help = HELP_FORCE)
            final boolean force, 
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        
        executeCommand(new UpgradeResourcesCommand(executionContext, upgradePropertiesFile), force, false, unitId);
    }
    
    @CliCommand(value = "execute upgrade-prepare", help = "Triggers or processes data-level upgrade manipulations in a compatible fashion. Does not require any downtime.")
    public void executeUpgradePrepare(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",  help = HELP_FORCE)
            final boolean force, 
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        executeCommand(new NamedCommand(Commands.UPGRADE_PREPARE, executionContext), force, false, unitId);
    }

    @CliCommand(value = "execute upgrade", help = "Triggers or processes data-level upgrade manipulations in a compatible fashion. Does not require any downtime.")
    public void executeUpgrade(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",  help = HELP_FORCE)
            final boolean force, 
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        executeCommand(new NamedCommand(Commands.UPGRADE, executionContext), force, false, unitId);
    }

    @CliCommand(value = "execute upgrade-finalize", help = "Triggers or processes data-level upgrade manipulations in a compatible fashion. Does not require any downtime.")
    public void executeUpgradeFinalize(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",  help = HELP_FORCE)
            final boolean force, 
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        executeCommand(new NamedCommand(Commands.UPGRADE_FINALIZE, executionContext), force, false, unitId);
    }

}
