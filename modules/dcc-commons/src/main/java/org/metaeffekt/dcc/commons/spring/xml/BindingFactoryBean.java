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
package org.metaeffekt.dcc.commons.spring.xml;

import java.io.File;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;
import org.metaeffekt.dcc.commons.mapping.Binding;
import org.metaeffekt.dcc.commons.mapping.Capability;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Documented;

/**
 * {@link FactoryBean} used to create {@link Binding} instances.
 * 
 * @author Douglas B.
 */
public class BindingFactoryBean extends AbstractFactoryBean<Binding> implements Documented {

    private File origin;
    private String description;
    
    private boolean autoBound;
    private ConfigurationUnit sourceUnit;
    private ConfigurationUnit targetUnit;
    private Id<CapabilityId> sourceCapabilityId;
    private Id<CapabilityId> targetCapabilityId;
    
    public BindingFactoryBean() {
        super();
    }
    
    public BindingFactoryBean(BindingFactoryBean original) {
        this();
        this.origin = original.getOrigin();
        this.sourceUnit = original.getSourceUnit();
        this.targetUnit = original.getTargetUnit();
        this.sourceCapabilityId = original.getSourceCapabilityId();
        this.targetCapabilityId = original.getTargetCapabilityId();
    }

    public File getOrigin() {
        return origin;
    }

    public void setOrigin(File origin) {
        this.origin = origin;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAutoBound() {
        return autoBound;
    }

    public void setAutoBound(boolean autoBound) {
        this.autoBound = autoBound;
    }

    public ConfigurationUnit getSourceUnit() {
		return sourceUnit;
	}

	public ConfigurationUnit getTargetUnit() {
		return targetUnit;
	}

	public Id<CapabilityId> getSourceCapabilityId() {
		return sourceCapabilityId;
	}

	public Id<CapabilityId> getTargetCapabilityId() {
		return targetCapabilityId;
	}

	public void setSourceCapabilityId(Id<CapabilityId> sourceCapabilityId) {
        this.sourceCapabilityId = sourceCapabilityId;
    }

    public void setTargetCapabilityId(Id<CapabilityId> targetCapabilityId) {
        this.targetCapabilityId = targetCapabilityId;
    }

    public void setSourceUnit(ConfigurationUnit sourceUnit) {
        this.sourceUnit = sourceUnit;
    }

    public void setTargetUnit(ConfigurationUnit targetUnit) {
        this.targetUnit = targetUnit;
    }

    /**
     * Returns the type that this FactoryBean creates - for this factory the type is always 
     * {@link Binding Binding.class}. 
     */
    @Override
    public Class<?> getObjectType() {
        return Binding.class;
    }

    /**
     * Creates a new {@link Binding} instance based on the injected properties.
     */
    @Override
    protected Binding createInstance() throws Exception {
        Capability sourceCapability = sourceUnit.findProvidedCapability(sourceCapabilityId);
        Capability targetCapability = targetUnit.findRequiredCapability(targetCapabilityId);

        if (sourceCapability == null || targetCapability == null) {
            
            String sourceMissing = sourceCapability == null ? "The source unit does not require the specified capability. " : "";
            String targetMissing = targetCapability == null ? "The target unit does not provide the specified capability. " : "";
            
            throw new BeanCreationException("Invalid Binding definition in ["+getOrigin()+"], " +
                    "sourceUnit=["+sourceUnit.getId()+"], targetUnit=["+targetUnit.getId()+"], " +
                    "sourceCapabilityId=["+sourceCapabilityId+"], targetCapabilityId=["+targetCapabilityId+"]. " +
                    sourceMissing + targetMissing);
        }
        
        Binding binding = new Binding(sourceCapability, targetCapability);
        binding.setOrigin(origin);
        binding.setDescription(description);
        binding.setAutoBound(autoBound);
        return binding;
    }

}
