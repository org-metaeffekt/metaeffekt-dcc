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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;

/**
 * A {@link Mapping} defines the transition between the consumed attributes of a unit to a
 * provided capability. Different {@link AttributeMapper}s support the transition.
 * 
 * @author Karsten Klein
 */
public class Mapping {

    private final Id<CapabilityId> targetCapabilityId;

    private Capability targetCapability;

    private ConfigurationUnit unit;

    private List<AttributeMapper> attributeMappers = new ArrayList<AttributeMapper>();

    public Mapping(Id<CapabilityId> targetCapabilityId) {
        this.targetCapabilityId = targetCapabilityId;
    }

    public Capability getTargetCapability() {
        if (targetCapability == null && targetCapabilityId != null && unit != null) {
            targetCapability = unit.findRequiredCapability(targetCapabilityId);
            if (targetCapability == null) {
                targetCapability = unit.findProvidedCapability(targetCapabilityId);
            }
        }
        return targetCapability;
    }

    void setUnit(ConfigurationUnit unit) {
        this.unit = unit;
    }

    public Id<CapabilityId> getTargetCapabilityId() {
        return targetCapabilityId;
    }

    public Mapping add(AttributeMapper... mappings) {
        for (int i = 0; i < mappings.length; i++) {
            this.attributeMappers.add(mappings[i]);
        }
        return this;
    }

    public void setAttributeMappers(List<AttributeMapper> attributeMappers) {
        this.attributeMappers = attributeMappers;
    }

    public List<AttributeMapper> getAttributeMappers() {
        return attributeMappers;
    }

    protected void evaluate(PropertiesHolder propertiesHolder, Profile profile) {
        // evaluate mappers
        final Capability resolvedTargetCapability = getTargetCapability();
        Validate.isTrue(resolvedTargetCapability != null, "The target capability with id ['%s'] does not exist.", getTargetCapabilityId());
        
        if (!attributeMappers.isEmpty()) {
            for (AttributeMapper mapper : attributeMappers) {
                mapper.evaluate(propertiesHolder, resolvedTargetCapability, profile);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Mapping [");
        if (getTargetCapability() != null)
            sb.append("targetCapability=" + getTargetCapability());
        if (attributeMappers != null)
            sb.append("attributeMappers=" + attributeMappers);
        sb.append("]");
        return sb.toString();
    }

}
