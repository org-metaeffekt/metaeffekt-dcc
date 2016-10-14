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

import org.springframework.util.StringUtils;

import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.DccUtils;


/**
 * Concrete implementation of the {@link AttributeMapper} interface propagating unit-level 
 * properties to properties of the target capability.
 * 
 * @author Karsten Klein
 */
public class UnitToCapabilityAttributeMapper implements AttributeMapper {

    private final String sourcePrefix;
    private final String targetPrefix;

    public UnitToCapabilityAttributeMapper(String sourcePrefix, String targetPrefix) {
        this.sourcePrefix = sourcePrefix;
        this.targetPrefix = targetPrefix;
    }

    public void evaluate(PropertiesHolder propertiesHolder, Capability targetCapability, Profile profile) {
        final ConfigurationUnit boundUnit = targetCapability.getUnit();
        
        final List<AttributeKey> attributeKeys = targetCapability.
            getCapabilityDefinition().getAttributeKeys();

        for (AttributeKey attributeKey : attributeKeys) {
            // only evaluate in case no property was already derived
            String key = attributeKey.getKey();
            String sourceKey = key;
            if (StringUtils.hasText(targetPrefix)) {
                if (sourceKey.indexOf(targetPrefix + DccConstants.SEPARATOR_UNIT_KEY) == 0) {
                    sourceKey = sourceKey.substring(targetPrefix.length() + 1);
                }
            }
            if (StringUtils.hasText(sourcePrefix)) {
                sourceKey = sourcePrefix + DccConstants.SEPARATOR_UNIT_KEY + sourceKey;
            }
            

            // attempt base properties supporting overwrites
            // FIXME: coupling to evaluation in ExpressionAttributeMapper
            final String globalKey = DccUtils.deriveAttributeIdentifier(targetCapability, attributeKey.getKey());
            String value = propertiesHolder.getBaseProperty(globalKey, null);
            propertiesHolder.markPropertyAsRelevant(targetCapability, sourceKey);

            if (value == null) {
                if (propertiesHolder.getProperty(targetCapability, key, false) == null) {
                    propertiesHolder.markPropertyAsRelevant(boundUnit, sourceKey);
                    value = propertiesHolder.getProperty(boundUnit, sourceKey);
                    value = attributeKey.applyDefaultIfNecessary(value);
                }
            }

            // FIXME: applying the default here will cause no expressions being evaluated.
            // this is not yet fully understood
            
            if (value != null) {
                propertiesHolder.setProperty(targetCapability, key, value);
            }

        }
    }
    
    public String getSourcePrefix() {
        return sourcePrefix;
    }
    
    public String getTargetPrefix() {
        return targetPrefix;
    }
}
