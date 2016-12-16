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
package org.metaeffekt.dcc.commons.mapping;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.util.CollectionUtils;

import org.metaeffekt.dcc.commons.ant.UrlUtils;
import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.dependency.UnitDependencies;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;
import org.metaeffekt.dcc.commons.domain.Type.ProfileId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;


/**
 * A profile aggregates {@link ConfigurationUnit}s and {@link Binding}s that connect the 
 * {@link ConfigurationUnit}s.
 * 
 * @author Karsten Klein
 */
public class Profile implements Documented {

    // FIXME: add support to register and lookup capability definitions
    
    private Id<ProfileId> id;
    
    private Id<DeploymentId> deploymentId;

    private Map<String, CapabilityDefinition> capabilityDefinitions = new HashMap<>();

    private Map<Id<UnitId>, ConfigurationUnit> units = new HashMap<>();
    
    private List<Binding> bindings = new ArrayList<>();
    
    private String description;

    private File origin;

    private UnitDependencies unitDependencies = new UnitDependencies();

    private final List<Assert> asserts = new ArrayList<Assert>();

    private Type type;
    
    private Properties solutionProperties;
    private Properties deploymentProperties;
    
    private File solutionPropertiesFile;
    private File deploymentPropertiesFile;
    
    private File solutionDir;
    
    public void setUnitDependencies(UnitDependencies unitDependencies) {
        this.unitDependencies = unitDependencies;
    }
    
    public UnitDependencies getUnitDependencies() {
        return unitDependencies;
    }

    public void setUnits(List<ConfigurationUnit> units) {
        this.units.clear();
        if (!CollectionUtils.isEmpty(units)) {
            for (ConfigurationUnit configurationUnit : units) {
                add(configurationUnit);
            }
        }
    }

    public void setCapabilityDefinitions(List<CapabilityDefinition> capabilityDefinitions) {
        this.capabilityDefinitions.clear();
        if (!CollectionUtils.isEmpty(capabilityDefinitions)) {
            for (CapabilityDefinition capabilityDefinition : capabilityDefinitions) {
                this.capabilityDefinitions.put(capabilityDefinition.getId(), capabilityDefinition);
            }
        }
    }

    public Profile add(ConfigurationUnit unit) {
        final ConfigurationUnit old = this.units.put(unit.getId(), unit);
        
        // asserts are only effective if the unit is not abstract
        if (!unit.isAbstract()) {
            if (old != null) {
                asserts.removeAll(old.getAsserts(ProfileEvaluatedAssert.class));
            } else {
                asserts.addAll(unit.getAsserts(ProfileEvaluatedAssert.class));
            }
        }
        return this;
    }

    public void setBindings(List<Binding> bindings) {
        this.bindings.clear();
        if (!CollectionUtils.isEmpty(bindings)) {
            this.bindings.addAll(bindings);
        }
    }

    public Profile add(Binding binding) {
        bindings.add(binding);
        return this;
    }
    
    public PropertiesHolder evaluate(final PropertiesHolder propertiesHolder) {
        for (ConfigurationUnit unit : units.values()) {
            unit.evaluate(propertiesHolder, this);
        }
        
        // NOTE: bindings are processed implicitly; also other units may already be processed
        //   recursively
        
        return propertiesHolder;
    }
    
    public ConfigurationUnit findUnit(Id<UnitId> unitId) {
        return units.get(unitId);
    }

    public CapabilityDefinition findCapabilityDefinition(String capabilityDefinitionId) {
        return capabilityDefinitions.get(capabilityDefinitionId);
    }
    
    public Collection<Binding> findBindings(Capability targetCapability) {
        assert(targetCapability != null);
        List<Binding> matchingBindings = new ArrayList<>();
        for (Binding binding : bindings) {
            if (targetCapability.equals(binding.getTargetCapability())) {
                matchingBindings.add(binding);
            }
        }
        return matchingBindings;
    }

    public Collection<Binding> findBindings(
            Id<UnitId> sourceUnitId, Id<UnitId> targetUnitId, 
            Id<CapabilityId> sourceCapId, Id<CapabilityId> targetCapId) {
        List<Binding> matchingBindings = new ArrayList<>();
        for (Binding binding : bindings) {
            if (sourceUnitId != null) {
                if (!sourceUnitId.equals(binding.getSourceCapability().getUnit().getId())) {
                    continue;
                }
            }
            if (targetUnitId != null) {
                if (!targetUnitId.equals(binding.getTargetCapability().getUnit().getId())) {
                    continue;
                }
            }
            if (sourceCapId != null) {
                if (!sourceCapId.equals(binding.getSourceCapability().getId())) {
                    continue;
                }
            }
            if (targetCapId != null) {
                if (!targetCapId.equals(binding.getTargetCapability().getId())) {
                    continue;
                }
            }
            matchingBindings.add(binding);
        }
        return matchingBindings;
    }

    public List<ConfigurationUnit> findUnitsWithProvidedCapabilityDefinition(String capabilityDefinitionId) {
        List<ConfigurationUnit> units = new ArrayList<>();
        for (ConfigurationUnit unit : this.units.values()) {
            for (Capability capability : unit.getProvidedCapabilities()) {
                if (capabilityDefinitionId.equals(capability.getCapabilityDefinition().getId())) {
                    if (!unit.isAbstract()) {
                        units.add(unit);
                    }
                }
            }
        }
        return units;
    }

    public List<ConfigurationUnit> getUnits() {
        List<ConfigurationUnit> units = new ArrayList<>();
        for (ConfigurationUnit configurationUnit : this.units.values()) {
            if (!configurationUnit.isAbstract()) {
                units.add(configurationUnit);
            }
        }
        return units;
    }
    
    public List<ConfigurationUnit> getUnits(boolean includeAbstractUnits) {
        if (includeAbstractUnits) {
            return new ArrayList<ConfigurationUnit>(units.values());
        }
        return getUnits();
    }

    public List<CapabilityDefinition> getCapabilityDefinitions() {
        return new ArrayList<CapabilityDefinition>(capabilityDefinitions.values());
    }
    
    public List<Binding> getBindings() {
    	return new ArrayList<Binding>(bindings);
    }

    public String getDescription() {
        return description;
    }

    public File getOrigin() {
        return origin;
    }

    public void setOrigin(File origin) {
        this.origin = origin;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Id<ProfileId> getId() {
        return id;
    }

    public void setId(Id<ProfileId> id) {
        this.id = id;
    }

    public enum Type {
        BASE, CONTRIBUTION, SOLUTION, DEPLOYMENT;
    }

    public Properties getSolutionProperties() {
        return solutionProperties;
    }

    public void setSolutionProperties(Properties solutionProperties, File source) {
        this.solutionProperties = solutionProperties;
        this.solutionPropertiesFile = source;
    }

    public Properties getDeploymentProperties() {
        return deploymentProperties;
    }

    public void setDeploymentProperties(Properties deploymentProperties, File source) {
        this.deploymentProperties = deploymentProperties;
        this.deploymentPropertiesFile = source;
    }

    public PropertiesHolder createPropertiesHolder(boolean includeDeploymentProperties) {
        PropertiesHolder propertiesHolder = new PropertiesHolder();
        propertiesHolder.setSolutionProperties(solutionProperties);
        if (includeDeploymentProperties) {
            propertiesHolder.setDeploymentProperties(deploymentProperties);
        }
        return propertiesHolder;
    }

    public Id<DeploymentId> getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(Id<DeploymentId> deploymentId) {
        this.deploymentId = deploymentId;
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

    public File getSolutionPropertiesFile() {
        return solutionPropertiesFile;
    }

    public File getDeploymentPropertiesFile() {
        return deploymentPropertiesFile;
    }
    
    public File getSolutionDir() {
        return solutionDir;
    }

    public void setSolutionDir(File solutionDir) {
        this.solutionDir = solutionDir;
    }

    private static final UrlUtils URL_UTILS = new UrlUtils();
    public String getRelativePath(Documented documented) {
        return getRelativePath(documented.getOrigin());
    }
    public String getRelativePath(File file) {
        File baseDir = solutionDir;
        if (baseDir == null) {
            baseDir = getOrigin();
        }
        try {
            return URL_UTILS.asRelativePath(baseDir, file);
        } catch (IOException e) {
            return file.getPath();
        }
    }

}
