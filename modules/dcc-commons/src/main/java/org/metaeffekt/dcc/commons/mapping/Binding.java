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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link Binding} is capable of connecting two capabilities (usually a provided with a required
 * {@link Capability}) with each other. Please note that the compatibility of the {@link Capability} 
 * (shared {@link CapabilityDefinition}) is not enforced. The implementation however expects that
 * the attribute keys of the target are covered by the source {@link Capability}.
 * 
 * @author Karsten Klein
 */
public class Binding extends AbstractDocumented {
    
    private static final Logger LOG = LoggerFactory.getLogger(Binding.class);

    private String uniqueId;
    private boolean autoBound;

    private final Capability sourceCapability;
    private final Capability targetCapability;

    public Binding(Capability sourceCapability, Capability targetCapability) {
        super(null, null);
        this.sourceCapability = sourceCapability;
        this.targetCapability = targetCapability;
        this.uniqueId = String.format("%s#%s", sourceCapability.getUniqueId(), targetCapability.getUniqueId());
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public boolean isAutoBound() {
        return autoBound;
    }

    public void setAutoBound(boolean autoBound) {
        this.autoBound = autoBound;
    }

    public Capability getSourceCapability() {
        return sourceCapability;
    }

    public Capability getTargetCapability() {
        return targetCapability;
    }

    public void evaluate(PropertiesHolder propertiesHolder, Profile profile) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Evaluating binding [{}] to [{}].", 
                sourceCapability.getUniqueId(), targetCapability.getUniqueId());
        }
        
        // ensure source unit was evaluated
        final ConfigurationUnit sourceUnit = sourceCapability.getUnit();
        sourceUnit.evaluate(propertiesHolder, profile);

        final List<AttributeKey> attributeKeys = targetCapability.
            getCapabilityDefinition().getAttributeKeys();
        
        for (AttributeKey attributeKey : attributeKeys) {
            String value = propertiesHolder.getProperty(sourceCapability, attributeKey.getKey());
            value = attributeKey.applyDefaultIfNecessary(value);
            if (value != null) {
                propertiesHolder.setProperty(targetCapability, attributeKey.getKey(), value);
            }
        }
        
        // populate with technical attributes accessible using _ as prefix
        propertiesHolder.setProperty(targetCapability, "_unit.id", sourceCapability.getUnit().getId().getValue());
        propertiesHolder.setProperty(targetCapability, "_capability.id", sourceCapability.getId().getValue());
        propertiesHolder.setProperty(targetCapability, "_capability.definition", sourceCapability.getCapabilityDefinition().getId());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Binding [");
        sb.append("sourceCapability=");
        sb.append(sourceCapability == null ? null : sourceCapability.getUniqueId());
        sb.append(", targetCapability=");
        sb.append(targetCapability == null ? null : targetCapability.getUniqueId());
        sb.append(", autoBound="+autoBound+"]");
        sb.append("]");
        return sb.toString();
    }
    
}
