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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.metaeffekt.dcc.commons.DccUtils;
import org.metaeffekt.dcc.commons.properties.SortedProperties;

/**
 * The {@link PropertiesHolder} manages all {@link Properties} that are evaluated by a profile. It
 * also provides consistent access to the input properties.
 * 
 * @author Karsten Klein
 */
public class PropertiesHolder {
    
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesHolder.class);
    
    // maintains a set of properties, that are relevant in terms of the profile evaluation process 
    private Set<String> relevantProperties = new HashSet<>();

    /**
     * Properties which contain information about the application itself, which are not deployment
     * scenario specific. Normally provided externally.
     */
    private Properties solutionProperties;

    /**
     * Properties which define the deployment setup. Normally these are host names only. Normally
     * provided externally.
     */
    private Properties deploymentProperties;

    /**
     * Map with properties for different identifiable objects.
     */
    private final Map<String, Properties> propertiesMap = new HashMap<String, Properties>();
    
    public Properties getSolutionProperties() {
        return solutionProperties;
    }

    public void setSolutionProperties(Properties solutionProperties) {
        this.solutionProperties = solutionProperties;
    }

    public Properties getDeploymentProperties() {
        return deploymentProperties;
    }

    public void setDeploymentProperties(Properties deploymentProperties) {
        this.deploymentProperties = deploymentProperties;
    }

    public Map<String, Properties> getObjectProperties() {
        return propertiesMap;
    }

    public Properties getProperties(Identifiable identifiable) {
        return getProperties(getIdentifiableStringId(identifiable));
    }

    public Properties getProperties(String id) {
        return getObjectProperties().get(id);
    }

    /**
     * Retrieves a property value from the deployment or solution properties. The deployment 
     * properties have precedence.
     * 
     * @param key The property key.
     * @param defaultValue The property default value to use in case no value can be resolved
     *   from the solution and deployment properties.
     * @return The value corresponding to the property.
     */
    public String getBaseProperty(String key, String defaultValue) {
        String value = null;
        
        if (deploymentProperties != null) {
            value = deploymentProperties.getProperty(key);
        }
        
        if (value == null && solutionProperties != null) {
            value = solutionProperties.getProperty(key);
        }
        
        if (value == null) {
            value = defaultValue;
        }
        
        return value;
    }

    public String getProperty(Identifiable identifiable, String key) {
        return getProperty(getIdentifiableStringId(identifiable), key);
    }

    public String getProperty(String id, String key) {
        return getProperty(id, key, true);
    }

    public String getProperty(Identifiable identifiable, String key, String defaultValue) {
        return getProperty(getIdentifiableStringId(identifiable), key, defaultValue);
    }

    public String getProperty(String id, String key, String defaultValue) {
        String value = getProperty(id, key, true);
        if (value != null) {
            return value;
        } else {
            return defaultValue;
        }
    }

    public String getProperty(Identifiable identifiable, String key, boolean allowFallback) {
        return getProperty(getIdentifiableStringId(identifiable), key, allowFallback);
    }

    public String getProperty(String id, String key, boolean allowFallback) {
        final Properties properties = getProperties(id);
        if (properties != null) {
            String value = properties.getProperty(key);
            if (value == null) {
                if (allowFallback) {
                    String identifyableKey = DccUtils.deriveAttributeIdentifier(id, key);
                    // first check context specific (in the context of the identifiable
                    value = getBaseProperty(identifyableKey, null);
                    if (value == null) {
                        // if not successful check out of context (context-free fallback)
                        value = getBaseProperty(key, null);
                        
                        // mark the property as relevant (result is used)
                        markPropertyAsRelevant(key); 
                    } else {
                        // mark the property as relevant (result is used)
                        markPropertyAsRelevant(identifyableKey);
                    }
                }
            } else {
                // mark the property as relevant (result is used)
                markPropertyAsRelevant(id, key);
            }
            return value;
        }
        return null;
    }

    public void setProperty(Identifiable identifiable, String key, String value) {
        final String uniqueId = getIdentifiableStringId(identifiable);
        final Properties p = createPropertiesIfNecessary(identifiable);
        LOG.trace("Setting property [{}] to value [{}] for object [{}].", key, value, uniqueId);
        if (value == null) {
            p.remove(key);
        } else {
            p.setProperty(key, value);
        }
    }

    private String getIdentifiableStringId(Identifiable identifiable) {
        if (identifiable == null) {
            return null;
        }
        return identifiable.getUniqueId();
    }

    public Properties createPropertiesIfNecessary(Identifiable identifiable) {
        Properties properties = getProperties(identifiable);
        if (properties == null) {
            properties = new SortedProperties();
            propertiesMap.put(getIdentifiableStringId(identifiable), properties);
        }
        return properties;
    }

    public void dump() {
        final Map<String, Properties> map = getObjectProperties();
        for (Map.Entry<String, Properties> entry : map.entrySet()) {
            final String objectId = entry.getKey();
            final Properties properties = entry.getValue();
            LOG.info("Properties for [{}]:", objectId);
            for (Object key : properties.keySet()) {
                String keyString = String.valueOf(key);
                LOG.info("   [{}] = [{}]", keyString, properties.getProperty(keyString));
            }
        }
    }

    public Map<String, Properties> getPropertiesMap() {
        return propertiesMap;
    }

    public void markPropertyAsRelevant(String propertyKey) {
        relevantProperties.add(propertyKey);
    }

    public void markPropertyAsRelevant(Identifiable id, String key) {
        markPropertyAsRelevant(DccUtils.deriveAttributeIdentifier(id, key));
    }

    public void markPropertyAsRelevant(String id, String key) {
        markPropertyAsRelevant(DccUtils.deriveAttributeIdentifier(id, key));
    }

    public boolean isPropertyRelevant(String propertyKey) {
        return relevantProperties.contains(propertyKey);
    }
}
