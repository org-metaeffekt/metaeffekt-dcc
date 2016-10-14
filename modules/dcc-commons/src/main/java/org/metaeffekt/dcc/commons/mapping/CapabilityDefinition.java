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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link CapabilityDefinition} consists of an id that uniquely identifies the 
 * {@link CapabilityDefinition}, a list of attribute keys and an optional list of references
 * {@link CapabilityDefinitionReference} to other {@link CapabilityDefinition} whose attribute keys have been inherited.
 * A {@link CapabilityDefinition} does not define any values.
 * 
 * @author Karsten Klein
 */
public class CapabilityDefinition extends AbstractDocumented {

    private String id;

    private boolean _abstract;

    private List<AttributeKey> attributeKeys = new ArrayList<>();

    private List<CapabilityDefinitionReference> ancestors = new ArrayList<>();
    
    private transient Map<String, AttributeKey> attributeKeyMap;

    public CapabilityDefinition(String id) {
        super(null, null);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isAbstract() {
        return _abstract;
    }

    public void setAbstract(boolean _abstract) {
        this._abstract = _abstract;
    }

    public CapabilityDefinition add(AttributeKey key) {
        attributeKeyMap = null;
        attributeKeys.add(key);
        return this;
    }

    public List<AttributeKey> getAttributeKeys() {
        return attributeKeys;
    }

    public void setAttributeKeys(List<AttributeKey> attributeKeys) {
        attributeKeyMap = null;
        if (attributeKeys == null) {
            this.attributeKeys = new ArrayList<>();
        } else {
            this.attributeKeys = attributeKeys;
        }
    }

    public List<CapabilityDefinitionReference> getAncestors() {
        return ancestors;
    }

    public void setAncestors(List<CapabilityDefinitionReference> ancestors) {
        if (ancestors == null) {
            this.ancestors = new ArrayList<>();
        } else {
            this.ancestors = ancestors;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CapabilityDefinition [");
        if (id!=null) { sb.append(" id=" + id); }
        if (getOrigin()!=null) { sb.append(" origin=" + getOrigin().getPath()); }
        if (attributeKeys!=null) { sb.append(" attributeKeys=" + attributeKeys); }
        if (ancestors!=null) { sb.append(" ancestors=" + ancestors); }
        sb.append(" ]");
        return sb.toString();
    }
    
    public boolean containsAttributeKey(String attributeKey) {
        initializeMapIfNecessary();
        return attributeKeyMap.containsKey(attributeKey);
    }
    
    private void initializeMapIfNecessary() {
        attributeKeyMap = new HashMap<>();
        for (AttributeKey attributeKey : getAttributeKeys()) {
            attributeKeyMap.put(attributeKey.getKey(), attributeKey);
        }

    }

}
