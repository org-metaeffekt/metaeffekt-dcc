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
package org.metaeffekt.dcc.commons.ant.wrapper;

import org.metaeffekt.dcc.commons.DccUtils;
import org.metaeffekt.dcc.commons.mapping.Identifiable;

public class PropertyWrapper extends AbstractPropertyBasedWrapper {
    private final String key;
    private String value;
    private final Identifiable identifiable;
    
    public PropertyWrapper(WrapperContext wrapperContext, Identifiable identifiable, String key, String value) {
        super(wrapperContext);
        this.identifiable = identifiable;
        this.key = key;
        this.value = value;
    }
    public String key() {
        return key;
    }
    public String value() {
        return value;
    }
    public Identifiable identifiable() {
        return identifiable;
    }
    public String property() {
        if (identifiable != null) {
            String attributeIdentifier = DccUtils.deriveAttributeIdentifier(identifiable, key);
            return renderProperty(attributeIdentifier, value, false);
        } else {
            return renderProperty(key, value, false);
        }
    }
    
    public String conditionalProperty(ProfileWrapper targetProfileWrapper) {
        boolean comment = false;
        PropertyWrapper propertyWrapper = targetProfileWrapper.get(identifiable, key, null);
        String targetDefaultValue = propertyWrapper == null ? null : propertyWrapper.value();
        if (targetDefaultValue != null) {
            if (targetDefaultValue.equals(value)) {
                comment = true;
            }
        }
        if (identifiable != null) {
            return renderProperty(DccUtils.deriveAttributeIdentifier(identifiable, key), value, comment);
        } else {
            return renderProperty(key, value, comment);
        }
    }
    
    public PropertyWrapper overwrite(String value) {
        this.value = value;
        return this;
    }
    
    @Override
    public String toString() {
        return escapeValue(value);
    }
    
    public void touch() {
        getWrapperContext().touch(DccUtils.deriveAttributeIdentifier(identifiable, key));
        getWrapperContext().getDerivedDeploymentProperties().put(key, value);
    }
}