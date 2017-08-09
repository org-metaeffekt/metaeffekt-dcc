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
package org.metaeffekt.dcc.commons.ant.wrapper;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Identifiable;

public class WrapperContext {

    private final Set<String> touchedIdentifiers = new HashSet<>();
    
    private final Properties derivedDeploymentProperties = new Properties();
    
    public void touch(Identifiable identifiable) {
        touch(identifiable.getUniqueId());
    }
    
    public void touch(String attributeId) {
        touchedIdentifiers.add(attributeId);
    }

    public boolean touched(Identifiable identifiable) {
        return touched(identifiable.getUniqueId());
    }
    
    public boolean touched(String attributeId) {
        return touchedIdentifiers.contains(attributeId);
    }

    public Set<String> getTouchedIdentifiers() {
        return touchedIdentifiers;
    }

    public Properties getDerivedDeploymentProperties() {
        return derivedDeploymentProperties;
    }

    public void untouch(String identifier) {
        touchedIdentifiers.remove(identifier);
    }

    public void untouch(ConfigurationUnit unit) {
        untouch(unit.getUniqueId());
    }
    
}
