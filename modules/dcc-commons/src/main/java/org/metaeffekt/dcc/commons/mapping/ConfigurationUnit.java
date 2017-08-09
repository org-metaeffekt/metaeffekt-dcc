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
package org.metaeffekt.dcc.commons.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;

/**
 * A {@link ConfigurationUnit} defines a unit that has required capabilities and provided
 * capabilities. A {@link ConfigurationUnit} may further define local attributes statically known
 * to the unit. Using unit internal mappings attributes (internal and evaluated from required
 * capabilities) can be mapped to cover the provided capabilities.
 * 
 * @author Karsten Klein
 */
public class ConfigurationUnit extends AbstractDocumented implements Identifiable, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationUnit.class);

    private Id<UnitId> id;

    private boolean isAbstract = false;

    private List<Capability> providedCapabilities = new ArrayList<>();

    private List<RequiredCapability> requiredCapabilities = new ArrayList<>();

    private final List<Mapping> mappings = new ArrayList<>();

    private final List<Attribute> attributes = new ArrayList<>();

    private final Map<Commands, CommandDefinition> commands = new EnumMap<>(Commands.class);

    private final List<Assert> asserts = new ArrayList<>();

    private String parentId;

    public ConfigurationUnit(String id) {
        this(Id.createUnitId(id));
    }

    public ConfigurationUnit(Id<UnitId> id) {
        super(null, null);
        this.id = id;
    }

    public Id<UnitId> getId() {
        return id;
    }

    public void setId(Id<UnitId> id) {
        this.id = id;
    }

    public List<Capability> getProvidedCapabilities() {
        return providedCapabilities;
    }

    public void setProvidedCapabilities(List<Capability> providedCapabilities) {
        this.providedCapabilities = providedCapabilities;
    }

    public List<RequiredCapability> getRequiredCapabilities() {
        return requiredCapabilities;
    }

    public void setRequiredCapabilities(List<RequiredCapability> requiredCapabilities) {
        this.requiredCapabilities = requiredCapabilities;
    }

    public void add(Mapping mapping) {
        mappings.add(mapping);
    }

    public List<Mapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<Mapping> mappings) {
        this.mappings.clear();
        if (!CollectionUtils.isEmpty(mappings)) {
            this.mappings.addAll(mappings);
        }
    }

    public void add(Attribute attribute) {
        attributes.add(attribute);
        Attribute.anticipateOverwrites(this.attributes);
    }

    public List<Attribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes.clear();
        if (!CollectionUtils.isEmpty(attributes)) {
            this.attributes.addAll(attributes);
            Attribute.anticipateOverwrites(this.attributes);
        }
    }

    public void setCommands(List<CommandDefinition> commandsDef) {
        this.commands.clear();

        if (!CollectionUtils.isEmpty(commandsDef)) {
            for (CommandDefinition commandDef : commandsDef) {
                this.commands.put(commandDef.getCommandId(), commandDef);
            }
        }
    }

    public CommandDefinition getCommand(Commands commandId) {
        return commands.get(commandId);
    }

    public List<CommandDefinition> getCommands() {
        return new ArrayList<CommandDefinition>(commands.values());
    }

    public Capability findProvidedCapability(Id<CapabilityId> capabilityId) {
        for (Capability capability : providedCapabilities) {
            if (capability.getId().equals(capabilityId)) {
                return capability;
            }
        }
        return null;
    }

    public Capability findRequiredCapability(Id<CapabilityId> capabilityId) {
        for (Capability capability : requiredCapabilities) {
            if (capability.getId().equals(capabilityId)) {
                return capability;
            }
        }
        return null;
    }

    public Capability findProvidedCapabilityWithCapabilityDefinition(String capabilityDefinitionId) {
        if (capabilityDefinitionId == null) { // TODO review when deployment command is available
            return null;
        }
        for (Capability capability : getProvidedCapabilities()) {
            if (capabilityDefinitionId.equals(capability.getCapabilityDefinition().getId())) {
                return capability;
            }
        }
        return null;
    }

    public Capability findRequiredCapabilityWithCapabilityDefinition(String capabilityDefinitionId) {
        for (Capability capability : getRequiredCapabilities()) {
            if (capabilityDefinitionId.equals(capability.getCapabilityDefinition().getId())) {
                return capability;
            }
        }
        return null;
    }

    public void evaluate(PropertiesHolder propertiesHolder, Profile profile) {
        // FIXME: we should have a better condition to determine whether a unit was already
        // evaluated
        Properties unitProperties = propertiesHolder.getProperties(this);
        if (unitProperties == null) {

            LOG.debug("Evaluating unit [{}]", getId());

            // populate with technical attributes accessible using _ as prefix
            propertiesHolder.setProperty(this, "_unit.id", getId().getValue());
            
            unitProperties = propertiesHolder.createPropertiesIfNecessary(this);

            if (!isAbstract()) {
                // evaluate bindings that represent an input for this unit
                for (Capability capability : getRequiredCapabilities()) {
                    Collection<Binding> bindings = profile.findBindings(capability);
                    if (bindings != null) {
                        for (Binding binding : bindings) {
                            binding.evaluate(propertiesHolder, profile);
                        }
                    }
                }
            }
            
            // evaluate the configured attributes
            List<Attribute> unitAttributes = getAttributes();
            if (unitAttributes != null) {
                for (Attribute attribute : unitAttributes) {
                    attribute.evaluate(propertiesHolder, this);
                }
            }

            // evaluate bindings registered to this unit
            if (!isAbstract()) {
                List<Mapping> mappings = getMappings();
                if (mappings != null) {
                    for (Mapping mapping : mappings) {
                        mapping.evaluate(propertiesHolder, profile);
                    }
                }

                // FIXME need to discuss this; need to find strategy (nothing is implicit)
                // this is an implicit mapping of the unit properties to the target capability
                for (Capability capability : getProvidedCapabilities()) {
                    propertiesHolder.createPropertiesIfNecessary(capability);
                    new UnitToCapabilityAttributeMapper(null, null).evaluate(propertiesHolder, capability, profile);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "ConfigurationUnit [id=" + id + ", providedCapabilities=" + providedCapabilities
            + ", requiredCapabilities=" + requiredCapabilities
            + ", attributes=" + attributes + "]";
    }

    @Override
    public String getUniqueId() {
        return getId().getValue();
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // set the units in the provided capabilities
        List<Capability> providedCapabilities = getProvidedCapabilities();
        for (Capability capability : providedCapabilities) {
            capability.setUnit(this);
        }
        filterOverwrittenCapabilities(providedCapabilities);
        
        // set the units in the required capabilities
        List<RequiredCapability> requiredCapabilities = getRequiredCapabilities();
        for (Capability capability : requiredCapabilities) {
            capability.setUnit(this);
        }
        filterOverwrittenCapabilities(requiredCapabilities);

        // set targetCapability of the mappings to the correct Capability instance
        List<Mapping> mappings = getMappings();
        for (Mapping mapping : mappings) {
            mapping.setUnit(this);
        }
        
        // mappings are not filtered; they may overwrite each other when executing the mapping

        for (Assert assertItem : getAsserts()) {
            if (AbstractUnitAssert.class.isInstance(assertItem)) {
                ((AbstractUnitAssert) assertItem).setUnit(this);
            }
        }
        
        // asserts are not filtered as well; currently they lack an id
    }

    protected void filterOverwrittenCapabilities(List<? extends Capability> capabilities) {
        List<Capability> filteredProvidedCapabilities = new ArrayList<>(capabilities);
        Collections.reverse(filteredProvidedCapabilities);
        Set<Id<?>> ids = new HashSet<>();
        for (Capability capability : filteredProvidedCapabilities) {
            if (ids.contains(capability.getId())) {
                capabilities.remove(capability);
            }
            ids.add(capability.getId());
        }
    }

    public List<Assert> getAsserts() {
        return Collections.unmodifiableList(asserts);
    }

    public <T extends Assert> List<T> getAsserts(Class<T> requiredType) {
        final List<T> foundAsserts = new ArrayList<T>();
        for (Assert assertItem : asserts) {
            if (requiredType.isInstance(assertItem)) {
                foundAsserts.add(requiredType.cast(assertItem));
            }
        }
        return foundAsserts;
    }

    public void setAsserts(List<Assert> asserts) {
        this.asserts.clear();
        if (!CollectionUtils.isEmpty(asserts)) {
            this.asserts.addAll(asserts);
        }
    }

}
