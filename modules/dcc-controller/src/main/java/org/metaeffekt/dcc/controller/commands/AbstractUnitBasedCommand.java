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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.DccUtils;
import org.metaeffekt.dcc.commons.ant.PropertyUtils;
import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.dependency.UnitDependencies;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;
import org.metaeffekt.dcc.commons.domain.Type.HostName;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.execution.Executor;
import org.metaeffekt.dcc.commons.mapping.Binding;
import org.metaeffekt.dcc.commons.mapping.Capability;
import org.metaeffekt.dcc.commons.mapping.CapabilityDefinitionReference;
import org.metaeffekt.dcc.commons.mapping.CommandDefinition;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.PrerequisiteAssert;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.properties.SortedProperties;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;

/**
 * @author Alexander D.
 * @author Jochen K.
 */
abstract class AbstractUnitBasedCommand extends AbstractCommand {

    public AbstractUnitBasedCommand(ExecutionContext executionContext) {
        super(executionContext);
    }

    @Override
    protected void doExecute(final boolean force, final boolean parallel, final Id<UnitId> limitToUnitId) {
        LOG.info("Executing command [{}] ...", getCommandVerb());
        List<ConfigurationUnit> units = selectUnitsWithCommandOrdered(getCommandVerb());
        boolean unitFound = false;

        // parallel processing approach
        // - collect units that are execute in a ordered list
        // - analyze dependencies; independent units (to be defined) are broken is separate lists
        // - process lists in parallel; within the lists in sequence

        for (ConfigurationUnit unit : units) {
            final Id<UnitId> unitId = unit.getId();
            if (limitToUnitId == null || unitId.equals(limitToUnitId)) {
                unitFound = true;
                prepareProperties(unit);
                if (isExecutionRequired(force, unitId, getCommandVerb())) {
                    LOG.debug("  Executing command [{}] for unit [{}]", getCommandVerb(),
                            unitId);
                    long timestamp = System.currentTimeMillis();
                    doExecuteCommand(unit);
                    
                    updateStatus(unitId);
                    afterSuccessfulUnitExecution(unit, timestamp);
                } else {
                    LOG.info("  Skipping command [{}] for unit [{}] as it already has been executed.",
                        getCommandVerb(), unitId);
                }
            }
        }

        if (limitToUnitId != null && !unitFound) {
            throw new IllegalArgumentException(String.format(
                "  Command [%s] not executable for unit [%s]. " + 
                "Either the unit does not exist or the command does not apply for the unit.",
                getCommandVerb(), limitToUnitId));
        }
    }

    protected Executor getExecutor(Id<UnitId> unitId) {
        return getExecutionContext().getExecutorForUnit(unitId);
    }

    protected void doExecuteCommand(ConfigurationUnit unit) {
        getExecutor(unit).execute(getCommandVerb(), unit);
    }

    protected void afterSuccessfulUnitExecution(ConfigurationUnit unit, long startTimestamp) {
        super.afterSuccessfulExecution("  ", String.format("[%s] for unit [%s]", getCommandVerb(), unit.getId()), startTimestamp);
    }

    protected Executor getExecutor(ConfigurationUnit unit) {
        if (isLocal()) {
            return getExecutionContext().getInstallationHostExecutor(unit.getId());
        } else {
            return getExecutionContext().getExecutorForUnit(unit.getId());
        }
    }

    protected List<ConfigurationUnit> processUnitsList(List<ConfigurationUnit> commandInstallUnits) {
        return commandInstallUnits;
    }

    protected File exportPropertiesFile(Properties p, ConfigurationUnit unit, CommandDefinition command, String classifier)
            throws IOException {
        File file = DccUtils.propertyFile(getConfigurationTargetPath(), unit.getId(), classifier);

        // NOTE the following encodes the unit location into the comment. This
        //  information is parsed by the agent watchdog.
        final StringBuilder comment = new StringBuilder("Command execution properties of unit (");
        comment.append(getExecutionContext().getProfile().getDeploymentId().getValue());
        comment.append(":");
        comment.append(unit.getId().getValue());
        if (command != null) {
            comment.append(":");
            comment.append(command.getPackageId().getValue());
        }
        comment.append(")");
        
        PropertyUtils.writeToFile(p, file, comment.toString());
        return file;
    }

    protected List<ConfigurationUnit> selectUnitsWithCommandOrdered(Commands commandId) {
        List<ConfigurationUnit> unitsWithCommandDefinition =
            getExecutionContext().getProfile().findUnitsDefinedCommand(commandId);

        UnitDependencies unitDependencies =
            getExecutionContext().getProfile().getUnitDependencies();
        unitDependencies.sortDownstream(unitsWithCommandDefinition);
        unitsWithCommandDefinition = processUnitsList(unitsWithCommandDefinition);
        return unitsWithCommandDefinition;
    }

    protected void prepareProperties(ConfigurationUnit unit) {
        prepareExecutionProperties(unit);
        preparePrerequisitesProperties(unit);
    }

    protected void preparePrerequisitesProperties(ConfigurationUnit unit) {
        final Properties properties = new SortedProperties();

        final Profile profile = getExecutionContext().getProfile();
        final PropertiesHolder propertiesHolder = getExecutionContext().getPropertiesHolder();
        for (PrerequisiteAssert assertItem : profile.getAsserts(
                PrerequisiteAssert.class)) {
            final String value = assertItem.evaluate(propertiesHolder, profile);
            if (value != null) {
                properties.setProperty(assertItem.getKey(), value);
            }
        }

        for (PrerequisiteAssert assertItem : unit.getAsserts(PrerequisiteAssert.class)) {
            final String value = assertItem.evaluate(propertiesHolder, profile);
            if (value != null) {
                properties.setProperty(assertItem.getKey(), value);
            }
        }

        try {
            exportPropertiesFile(properties, unit, null, DccConstants.PREREQUISITES_PROPERTIES_FILE_NAME);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void prepareExecutionProperties(ConfigurationUnit unit) {
        final Properties properties = new SortedProperties();

        final CommandDefinition command = unit.getCommand(getCommandVerb());
        Validate.notNull(command, "Command should not be null.");

        processCapabilities(unit, properties, command);
        processContributions(unit, properties, command);
        processRequisitions(unit, properties, command);
        processProvisions(unit, properties, command);

        try {
            exportPropertiesFile(properties, unit, command, DccUtils.propertyFileName(getCommandVerb()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void processProvisions(ConfigurationUnit unit, final Properties properties,
            CommandDefinition command) {
        UnitCapabilityPropertiesAggregatorFactory.createProvisionAggregator(getExecutionContext())
                .processCollectInstructions(unit, command, properties, false);
    }

    private void processRequisitions(ConfigurationUnit unit, final Properties properties,
            final CommandDefinition command) {
        // process requisitions (SPI pattern)
        UnitCapabilityPropertiesAggregatorFactory
                .createRequisitionAggregator(getExecutionContext())
                .processCollectInstructions(unit, command, properties, true);
    }

    private void processContributions(ConfigurationUnit unit, final Properties properties,
            final CommandDefinition command) {
        // process contributions (
        UnitCapabilityPropertiesAggregatorFactory
                .createContributionAggregator(getExecutionContext())
                .processCollectInstructions(unit, command, properties, false);
    }

    private void processCapabilities(ConfigurationUnit unit, final Properties properties,
            final CommandDefinition command) {
        for (CapabilityDefinitionReference ref : command.getCapabilities()) {
            final Id<CapabilityId> commandInputCapabilityId =
                Id.createCapabilityId(ref.getReferencedCapabilityDefId());

            if (commandInputCapabilityId != null) {
                // access the capability reference by capabilityId
                Capability commandCapability =
                    unit.findProvidedCapability(commandInputCapabilityId);

                // for backward compatibility...
                if (commandCapability == null) {
                    commandCapability =
                        unit.findProvidedCapabilityWithCapabilityDefinition(commandInputCapabilityId
                                .getValue());
                    if (commandCapability != null) {
                        LOG.warn(
                                "The unit [{}] uses a id reference to a capability definition [{}] instead of a "
                                        + "concrete capability. This may not be supported in future versions. "
                                        + "Please replace the reference to [{}] by the id of a provided capability.",
                                unit.getId(), commandInputCapabilityId, commandInputCapabilityId);
                    }
                    if (commandCapability == null) {
                        commandCapability = unit.findRequiredCapability(commandInputCapabilityId);
                        if (commandCapability != null) {
                            Collection<Binding> bindings = getExecutionContext().getProfile().
                                    findBindings(commandCapability);
                            if (bindings.size() == 1) {
                                Binding binding = bindings.iterator().next();
                                commandCapability = binding.getSourceCapability();
                            } else {
                                // no action, no exception: binding may be optional
                            }
                        }
                    }
                }
                if (commandCapability == null) {
                    throw new IllegalStateException(String.format("The capability with id [%s] "
                            + "for command %s in unit %s cannot be found!",
                            commandInputCapabilityId.getValue(),
                            command.getCommandId(), unit.getId()));
                }
                final Properties propertiesToCopy = getExecutionContext().
                        getPropertiesHolder().getProperties(commandCapability);
                if (propertiesToCopy != null) {
                    for (Enumeration<?> enumeration = propertiesToCopy.propertyNames(); enumeration
                            .hasMoreElements();) {
                        final String key = (String) enumeration.nextElement();
                        properties.put(adaptKey(ref.getPrefix(), key),
                                propertiesToCopy.getProperty(key));
                    }
                }
            }
        }
    }

    private String adaptKey(String prefix, String key) {
        if (StringUtils.isBlank(prefix)) {
            return key;
        }

        return prefix.trim() + "." + key;
    }

    protected boolean isExecutionRequired(final boolean force, final Id<UnitId> unitId, final Commands command) {
        if (force) {
            return true;
        } 

        if (!allowsToBeSkipped()) {
            return true;
        }

        if (!isLocal()) {
            Id<HostName> hostForUnit = getExecutionContext().getHostForUnit(unitId);
            updateStatus(unitId);
            
            final Id<DeploymentId> deploymentId = getExecutionContext().getProfile().getDeploymentId();
            return !getExecutionStateHandler().alreadySuccessfullyExecuted(unitId, command, hostForUnit, deploymentId);
        } else {
            return true;
        }
    }

    protected void updateStatus(final Id<UnitId> unitId) {
        if (!isLocal()) {
            final Executor executorForUnit = getExecutionContext().getExecutorForUnitIfExists(unitId);
            if (executorForUnit != null) {
                executorForUnit.retrieveUpdatedState();
            }
        }
    }

}
