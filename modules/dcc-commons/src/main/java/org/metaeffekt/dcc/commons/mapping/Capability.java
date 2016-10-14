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

import org.apache.commons.lang3.StringUtils;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;

/**
 * A {@link Capability} represents a concrete realization of a {@link CapabilityDefinition} within
 * a {@link ConfigurationUnit}.
 * 
 * @author Karsten Klein
 */
public class Capability implements Identifiable {

    private Id<CapabilityId> id;

    private String uniqueId;

    private CapabilityDefinition capabilityDefinition;

    private ConfigurationUnit unit;

    @Deprecated
    public Capability(String id, CapabilityDefinition capabilityDefinition, ConfigurationUnit unit) {
        this(Id.createCapabilityId(id), capabilityDefinition, unit);
    }

    public Capability(Id<CapabilityId> id, CapabilityDefinition capabilityDefinition, ConfigurationUnit unit) {
        this.id = id;
        this.capabilityDefinition = capabilityDefinition;
        this.unit = unit;
    }

    public Id<CapabilityId> getId() {
        return id;
    }

    public void setId(Id<CapabilityId> id) {
        this.id = id;
    }

    public CapabilityDefinition getCapabilityDefinition() {
        return capabilityDefinition;
    }

    public void setCapabilityDefinition(CapabilityDefinition capabilityDefinition) {
        this.capabilityDefinition = capabilityDefinition;
    }

    public ConfigurationUnit getUnit() {
        return unit;
    }

    public void setUnit(ConfigurationUnit unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Capability [");
        sb.append("unique id=" + getUniqueId());
        sb.append("]");
        return sb.toString();
    }

    public String getUniqueId() {
        if (StringUtils.isEmpty(uniqueId) && unit != null) {
            this.uniqueId = unit.getId() + "/" + id;
        }
        return this.uniqueId;
    }

}
