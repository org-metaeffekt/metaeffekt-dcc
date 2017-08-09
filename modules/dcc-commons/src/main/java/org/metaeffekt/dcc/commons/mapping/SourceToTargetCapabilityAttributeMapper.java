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

import java.util.List;

import org.springframework.util.StringUtils;

import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;


/**
 * Concrete implementation of the {@link AttributeMapper} interface propagating all attributes
 * of a source capability to a target capability.
 * 
 * @author Karsten Klein
 */
public class SourceToTargetCapabilityAttributeMapper implements AttributeMapper {

    private final Id<CapabilityId> sourceCapabilityId;
    
    private final String sourcePrefix;
    private final String targetPrefix;

    public SourceToTargetCapabilityAttributeMapper(Id<CapabilityId> sourceCapabilityId,
            String sourcePrefix, String targetPrefix) {
        this.sourceCapabilityId = sourceCapabilityId;
        this.sourcePrefix = sourcePrefix;
        this.targetPrefix = targetPrefix;
    }

    public void evaluate(PropertiesHolder propertiesHolder, Capability targetCapability, Profile profile) {
        Capability sourceCapability = targetCapability.getUnit().findRequiredCapability(sourceCapabilityId);
        if (sourceCapability == null) {
            throw new IllegalStateException(String.format(
                "No source capability with capabilityId [%s] found for target capability [%s], while trying to map attributes from source to target.",
                sourceCapabilityId, targetCapability.getUniqueId()));
        }

        // ensure source unit was evaluated
        sourceCapability.getUnit().evaluate(propertiesHolder, profile);

        final CapabilityDefinition sourceCapabilityDefinition = sourceCapability.getCapabilityDefinition();
        final CapabilityDefinition targetCapabilityDefinition = targetCapability.getCapabilityDefinition();
        final List<AttributeKey> attributeKeys = targetCapabilityDefinition.getAttributeKeys();
        
        for (AttributeKey attributeKey : attributeKeys) {
            String sourceKey = attributeKey.getKey();
            if (StringUtils.hasText(targetPrefix)) {
                if (sourceKey.indexOf(targetPrefix + DccConstants.SEPARATOR_UNIT_KEY) == 0) {
                    sourceKey = sourceKey.substring(targetPrefix.length() + 1);
                }
            }
            if (StringUtils.hasText(sourcePrefix)) {
                sourceKey = sourcePrefix + DccConstants.SEPARATOR_UNIT_KEY + sourceKey;
            }
            if (sourceCapabilityDefinition.containsAttributeKey(sourceKey)) {
                String value = propertiesHolder.getProperty(sourceCapability, sourceKey);
                if (value == null) {
                    value = propertiesHolder.getProperty(targetCapability.getUnit(), sourceKey);
                }
                value = attributeKey.applyDefaultIfNecessary(value);
                if (value != null) {
                    propertiesHolder.setProperty(targetCapability, attributeKey.getKey(), value);
                }
            }
        }
    }

    public Id<CapabilityId> getSourceCapabilityId() {
        return sourceCapabilityId;
    }

    public String getSourcePrefix() {
        return sourcePrefix;
    }

    public String getTargetPrefix() {
        return targetPrefix;
    }

}
