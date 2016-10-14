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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import org.metaeffekt.dcc.commons.ant.wrapper.StringBlockFormatter;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.mapping.Attribute;
import org.metaeffekt.dcc.commons.mapping.Attribute.AttributeType;
import org.metaeffekt.dcc.commons.mapping.AttributeKey;
import org.metaeffekt.dcc.commons.mapping.Capability;
import org.metaeffekt.dcc.commons.mapping.CapabilityDefinition;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Mapping;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.mapping.RequiredCapability;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;
import org.metaeffekt.dcc.shell.converters.UnitCapabilityIdConverter;

/**
 * {@link CommandMarker} instance which holds all commands that can be executed on the shell for 
 * displaying information about a selected profile (summary, units etc). 
 */
@Component
public class DisplayProfileCommands implements CommandMarker {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final Logger LOG = LoggerFactory.getLogger(DisplayProfileCommands.class);
    private final ExecutionContext executionContext;
    
    @Autowired
    public DisplayProfileCommands(ExecutionContext profileHolder) {
        this.executionContext = profileHolder;
    }
    
    // TODO: consider removing "profile" from all of these commands
    @CliAvailabilityIndicator({"display profile summary", "display profile unit", "display profile all units", "display profile all capability definitions", "display profile capability definition"})
    public boolean areDisplayCommandsAvailable() {
        return executionContext.containsProfile();
    }
    
    @CliCommand(value = "display profile summary", help = "Display the summary for the currently loaded profile.")
    public void displayProfileSummary() {
        Profile profile = CommandsUtils.retrieveProfile(executionContext);
        if (profile != null) {
            LOG.info("Profile id: " + profile.getId());
            LOG.info("Profile description: " + profile.getDescription());
            listCapabilityDefinitions(profile);
            listUnits(profile);
        } else {
            LOG.info("No profile selected.");
        }
    }
    
    @CliCommand(value = "display profile all units", help = "Display all units.")
    public void displayUnits() {
        Profile profile = CommandsUtils.retrieveProfile(executionContext);
        if (profile != null) {
            listUnits(profile);
        } else {
            LOG.info("No profile selected.");
        }
    }
    
    private void listUnits(Profile profile) {
        LOG.info("Units: ");
        List<ConfigurationUnit> units = profile.getUnits();
        for (ConfigurationUnit configurationUnit : units) {
            LOG.info("- " + configurationUnit.getId());
        }
        if (units.isEmpty()) {
            LOG.info("No units available.");
        }
    }

    @CliCommand(value = "display profile all capability definitions", help = "Display all capability definitions.")
    public void displayCapabilityDefinitions() {
        Profile profile = CommandsUtils.retrieveProfile(executionContext);
        if (profile != null) {
            listCapabilityDefinitions(profile);
        } else {
            LOG.info("No profile selected.");
        }
    }
    
    private void listCapabilityDefinitions(Profile profile) {
        LOG.info("Capability Definitions: ");
        List<CapabilityDefinition> capabilityDefinitions = profile.getCapabilityDefinitions();
        for (CapabilityDefinition capabilityDefinition : capabilityDefinitions) {
            LOG.info("- " + capabilityDefinition.getId());
        }
        if (capabilityDefinitions.isEmpty()) {
            LOG.info("No capability definitions available.");
        }
    }
    
    @CliCommand(value = "display profile capability definition", help = "Display the details of the capability definition by the given id.")
    public void displayCapabilityDefinition(@CliOption(key = "capabilityDefinitionId", optionContext = UnitCapabilityIdConverter.USE_CAPABILITY_DEFINITION_ID_COMPLETION, mandatory = true, help = "The id of the capability definition for which the details should be displayed.") String capabilityDefinitionId) {
        Profile profile = CommandsUtils.retrieveProfile(executionContext);
        if (profile != null) {
            CapabilityDefinition capabilityDefinition = profile.findCapabilityDefinition(capabilityDefinitionId);
            if (capabilityDefinition != null) {
                LOG.info("Capability Definition - " + capabilityDefinition.getId());
                LOG.info("Attribute Keys:");
                List<AttributeKey> attributeKeys = capabilityDefinition.getAttributeKeys();
                for (AttributeKey attributeKey : attributeKeys) {
                    LOG.info("- " + attributeKey);
                }
                if (attributeKeys.isEmpty()) {
                    LOG.info("No attribute keys defined.");
                }
            } else {
                LOG.info("The profile does not contain any capability definition with the id [" + capabilityDefinitionId + "].");
            }
        } else {
            LOG.info("No profile selected.");
        }
    }
    
    @CliCommand(value = "display profile unit", help = "Display the details of the unit identified by the given id.")
    public void displayUnit(@CliOption(key = "unitId", optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, mandatory = true, help = "The id of the unit for which the details should be displayed.") String unitId) {
        Profile profile = CommandsUtils.retrieveProfile(executionContext);
        if (profile != null) {
            ConfigurationUnit unit = profile.findUnit(Id.createUnitId(unitId));
            if (unit != null) {
                LOG.info("Unit id: " + unit.getId());
                LOG.info("Attributes:");
                List<Attribute> attributes = unit.getAttributes();
                if (attributes.isEmpty()) {
                    LOG.info("No attributes defined in unit.");
                } else {
                    for (Attribute attribute : attributes) {
                        LOG.info(attribute.toString());
                    }
                }
                
                LOG.info("Mappings:");
                List<Mapping> mappings = unit.getMappings();
                if (mappings.isEmpty()) {
                    LOG.info("No mappings defined in unit.");
                } else {
                    for (Mapping mapping : mappings) {
                        LOG.info(mapping.toString());
                    }
                }
                
                LOG.info("Provided Capabilities:");
                List<Capability> providedCapabilities = unit.getProvidedCapabilities();
                if (providedCapabilities.isEmpty()) {
                    LOG.info("No provided capabilities defined in unit.");
                } else {
                    for (Capability capability : providedCapabilities) {
                        LOG.info(capability.toString());
                    }
                }

                LOG.info("Required Capabilities:");
                List<RequiredCapability> requiredCapabilities = unit.getRequiredCapabilities();
                if (requiredCapabilities.isEmpty()) {
                    LOG.info("No required capabilities defined in unit.");
                } else {
                    for (Capability capability : requiredCapabilities) {
                        LOG.info(capability.toString());
                    }
                }
                
            } else {
                LOG.info("The profile does not contain any unit with the id [" + unitId + "].");
            }
        } else {
            LOG.info("No profile selected.");
        }
    }

    @CliCommand(value = "display attributes", help = "Display the details of the attributes which could be configured.")
    public void displayAttributes(
            @CliOption(key = "type", help = "The type of the attributes.") AttributeType type, 
            @CliOption(key = "unitId", optionContext = UnitCapabilityIdConverter.USE_UNIT_ID_COMPLETION, mandatory = false, 
                       help = "The id of the unit for which the details should be displayed.") String unitId) {
        if (type == null) {
            type = AttributeType.BASIC;
        }

        Profile profile = CommandsUtils.retrieveProfile(executionContext);
        if (profile != null) {
            final PropertiesHolder propertiesHolder = profile.createPropertiesHolder(true);

            StringBuilder sb = new StringBuilder("Displaying attributes:");
            sb.append(LINE_SEPARATOR);
            
            for (ConfigurationUnit unit : profile.getUnits()) {
                if (unitId == null || unitId.equals(unit.getId().getValue())) {
                    boolean hasContent = false;
                    for (Attribute attribute : unit.getAttributes()) {
                        if (type != AttributeType.EXPERT && attribute.getType() != type) {
                            continue;
                        }
                        if (!StringUtils.isEmpty(attribute.getDescription())) {
                            String trimmedComment = attribute.getDescription().replaceAll("\\s\\s*", " ");
                            trimmedComment = StringBlockFormatter.formatString(trimmedComment, "# ", LINE_SEPARATOR);
                            sb.append(trimmedComment);
                        }
                        sb.append(String.format("%s=%s", attribute.getGlobalKey(unit),
                                attribute.evaluateToString(propertiesHolder, unit)));
                        sb.append(LINE_SEPARATOR);
                        hasContent = true;
                    }
                    if (hasContent) {
                        sb.append(LINE_SEPARATOR);
                    }
                }
            }
            
            LOG.info(sb.toString());
        } else {
            LOG.info("No profile selected.");
        }
    }

}
