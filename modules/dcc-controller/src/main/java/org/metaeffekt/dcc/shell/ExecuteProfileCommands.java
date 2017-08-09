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

import static org.metaeffekt.dcc.shell.Constants.HELP_FORCE;
import static org.metaeffekt.dcc.shell.Constants.HELP_UNITID;
import static org.metaeffekt.dcc.shell.Constants.HELP_PARALLEL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import org.metaeffekt.dcc.controller.commands.BootstrapCommand;
import org.metaeffekt.dcc.controller.commands.CleanCommand;
import org.metaeffekt.dcc.controller.commands.ConfigureCommand;
import org.metaeffekt.dcc.controller.commands.DeployCommand;
import org.metaeffekt.dcc.controller.commands.EvaluateTemplatesCommand;
import org.metaeffekt.dcc.controller.commands.InitializeResourcesCommand;
import org.metaeffekt.dcc.controller.commands.ImportCommand;
import org.metaeffekt.dcc.controller.commands.ImportTestDataCommand;
import org.metaeffekt.dcc.controller.commands.InitializeCommand;
import org.metaeffekt.dcc.controller.commands.InstallCommand;
import org.metaeffekt.dcc.controller.commands.PreparePersistenceCommand;
import org.metaeffekt.dcc.controller.commands.PurgeCommand;
import org.metaeffekt.dcc.controller.commands.ReconfigureCommand;
import org.metaeffekt.dcc.controller.commands.StartCommand;
import org.metaeffekt.dcc.controller.commands.StopCommand;
import org.metaeffekt.dcc.controller.commands.UninstallCommand;
import org.metaeffekt.dcc.controller.commands.UploadCommand;
import org.metaeffekt.dcc.controller.commands.VerifyCommand;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;
import org.metaeffekt.dcc.shell.converters.UnitCapabilityIdConverter;

/**
 * {@link CommandMarker} instance which holds all commands that can be executed on the shell for 
 * executing a selected profile ({@code install}, {@code configure}, {@code start}, {@code stop}, 
 * {@code bootstrap} etc). 
 */
@Component
public class ExecuteProfileCommands extends AbstractExecutionCommands {
    
    @Autowired
    public ExecuteProfileCommands(ExecutionContext executionContext) {
        super(executionContext);
    }

    /**
     * Provides the availability indicator for all commands.
     * 
     * @return Returns <code>true</code> in case the command can be applied.
     */
    @Override
    @CliAvailabilityIndicator(
        {
            // Make sure all commands are added here.
            "execute initialize", 
            "execute initialize-resources", 
            "execute install", 
            "execute configure",
            "execute deploy",
            "execute prepare-persistence",
            "execute bootstrap",
            "execute import",
            "execute import-test-data",
            "execute reconfigure",
            "execute start",
            "execute stop",
            "execute uninstall",
            "execute upload",
            "execute clean"
        }
    )
    public boolean canExecuteProfile() {
        return super.canExecuteProfile();
    }
    
    @CliCommand(value = "execute initialize", help = "Initialize the hosts referenced by the selected profile.")
    public void executeInitialize(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",  help = HELP_FORCE)
            final boolean force,
            @CliOption(key = "parallel", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_PARALLEL)
            final boolean parallel,
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        
        // FIXME: review force mode in this context. 
        //   - It this relevant in this context? Does it apply to all subcommands
        //   - Do we want to make the evaluate and verify independent commands?
        
        // evaluate templates is enforced independent of force flag (DCC-403)
        executeCommand(new EvaluateTemplatesCommand(executionContext), true, false, false, unitId);
        
        executeCommand(new InitializeResourcesCommand(executionContext), force, false, false, unitId);
        executeCommand(new InitializeCommand(executionContext), force, parallel, true, unitId);
        executeCommand(new VerifyCommand(executionContext), force, parallel, false, unitId);
    }
    
    @CliCommand(value = "execute initialize-resources", help = "Initialize resources locally.")
    public void executeInitializeResources(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",  help = HELP_FORCE)
            final boolean force,
            @CliOption(key = "parallel", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_PARALLEL)
            final boolean parallel,
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        executeCommand(new EvaluateTemplatesCommand(executionContext), force, parallel, false, unitId);
        executeCommand(new InitializeResourcesCommand(executionContext), force, parallel, false, unitId);
    }

    @CliCommand(value = "execute clean", help = "Clean the temporary area on the hosts referenced by the selected profile.")
    public void executeClean(
            @CliOption(key = "parallel", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_PARALLEL)
            final boolean parallel,
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
        String unitId) {
        executeCommand(new CleanCommand(executionContext), true, parallel, false, unitId);
    }
    
    @CliCommand(value = "execute install", help = "Install the units as specified by the selected profile.")
    public void executeInstall(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_FORCE)
            final boolean force,
            @CliOption(key = "parallel", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_PARALLEL)
            final boolean parallel,
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        executeCommand(new InstallCommand(executionContext), force, parallel, false, unitId);
    }

    @CliCommand(value = "execute uninstall", help = "Uninstall the units as specified by the selected profile.")
    public void executeUninstall(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_FORCE)
            final boolean force,
            @CliOption(key = "parallel", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_PARALLEL)
            final boolean parallel,
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        executeCommand(new UninstallCommand(executionContext), force, parallel, false, unitId);
        
        // in case a unit id is provided the uninstall was focussed on the unit only. In this case a purge is
        // not applicable.
        if (unitId == null) {
            executeCommand(new PurgeCommand(executionContext), force, parallel, false, unitId);
        }
    }

    @CliCommand(value = "execute configure", help = "Configure the units as specified by the selected profile.")
    public void executeConfigure(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",  help = HELP_FORCE)
            final boolean force,
            @CliOption(key = "parallel", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_PARALLEL)
            final boolean parallel,
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        executeCommand(new ConfigureCommand(executionContext), force, parallel, false, unitId);
    }

    @CliCommand(value = "execute deploy", help = "Deploy artifacts on the units as referenced by the selected profile.")
    public void executeDeploy(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",  help = HELP_FORCE)
            final boolean force,
            @CliOption(key = "parallel", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_PARALLEL)
            final boolean parallel,
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        executeCommand(new DeployCommand(executionContext), force, parallel, false, unitId);
    }

    @CliCommand(value = "execute prepare-persistence", help = "Prepare the database as specified by the selected profile.")
    public void executePreparePersistence(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",  help = HELP_FORCE)
            final boolean force,
            @CliOption(key = "parallel", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_PARALLEL)
            final boolean parallel,
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        executeCommand(new PreparePersistenceCommand(executionContext), force, parallel, false, unitId);
    }

    @CliCommand(value = "execute bootstrap", help = "Bootstrap the units as specified by the selected profile.")
    public void executeBootstrap(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",  help = HELP_FORCE)
            final boolean force,
            @CliOption(key = "parallel", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_PARALLEL)
            final boolean parallel,
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        executeCommand(new BootstrapCommand(executionContext), force, parallel, false, unitId);
    }

    @CliCommand(value = "execute import", help = "Import data from the units as specified by the selected profile.")
    public void executeImport(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",  help = HELP_FORCE)
            final boolean force,
            @CliOption(key = "parallel", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_PARALLEL)
            final boolean parallel,
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        executeCommand(new ImportCommand(executionContext), force, parallel, false, unitId);
    }

    @CliCommand(value = "execute import-test-data", help = "Import test data from the units as specified by the selected profile.")
    public void executeImportTestData(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",  help = HELP_FORCE)
            final boolean force,
            @CliOption(key = "parallel", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_PARALLEL)
            final boolean parallel,
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        executeCommand(new ImportTestDataCommand(executionContext), force, parallel, false, unitId);
    }

    @CliCommand(value = "execute upload", help = "Upload data after the processes have started.")
    public void executeUpload(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",  help = HELP_FORCE)
            final boolean force,
            @CliOption(key = "parallel", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_PARALLEL)
            final boolean parallel,
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        executeCommand(new UploadCommand(executionContext), force, parallel, false, unitId);
    }

    @CliCommand(value = "execute reconfigure", help = "Reconfigure the units.")
    public void executeReconfigure(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",  help = HELP_FORCE)
            final boolean force,
            @CliOption(key = "parallel", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_PARALLEL)
            final boolean parallel,
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        
        // TODO: the external folder needs to be redistributed to the target hosts; an optimization
        //   would be to only focus on the external folder
        executeCommand(new InitializeCommand(executionContext), force, parallel, true, unitId);
        executeCommand(new ReconfigureCommand(executionContext), force, parallel, false, unitId);
    }

    @CliCommand(value = "execute start", help = "Start units as specified by the selected profile.")
    public void executeStart(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",  help = HELP_FORCE)
            final boolean force,
            @CliOption(key = "parallel", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_PARALLEL)
            final boolean parallel,
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        executeCommand(new StartCommand(executionContext), force, parallel, false, unitId);
    }

    @CliCommand(value = "execute stop", help = "Stop the units as specified by the selected profile.")
    public void executeStop(
            @CliOption(key = "force", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",  help = HELP_FORCE)
            final boolean force,
            @CliOption(key = "parallel", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = HELP_PARALLEL)
            final boolean parallel,
            @CliOption(key = "unitId", mandatory = false, optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, help = HELP_UNITID)
            String unitId
            ) {
        executeCommand(new StopCommand(executionContext), force, parallel, false, unitId);
    }
    
}
