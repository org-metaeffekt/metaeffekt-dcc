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

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * An {@link Attribute} consists of a key and a value.
 * 
 * @author Karsten Klein
 */
public class Attribute extends AbstractDocumented {

    public enum AttributeType {
        BASIC, EXPERT
    };

    private String key;

    private String value;

    private AttributeType attributeType;

    public Attribute() {
        super(null, null);
    }

    @Deprecated
    public Attribute(String key, String value, String origin) {
        super(new File(origin), null);
        this.key = key;
        this.value = value;
    }

    public Attribute(String key, String value, File origin) {
        super(origin, null);
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public AttributeType getType() {
        return attributeType;
    }

    public void setType(AttributeType attributeType) {
        this.attributeType = attributeType;
    }

    public void evaluate(PropertiesHolder propertiesHolder, ConfigurationUnit unit) {
        final String derivedValue = evaluateToString(propertiesHolder, unit);
        propertiesHolder.setProperty(unit, this.key, derivedValue);
    }

    public String evaluateToString(PropertiesHolder propertiesHolder, ConfigurationUnit unit) {
        // check base properties first (allow overwrite) and use local value as default
        final String evaluatedValue = replaceExpressions(propertiesHolder, unit);
        final String unitKey = getGlobalKey(unit);
        final String value = propertiesHolder.getBaseProperty(unitKey, evaluatedValue);

        return value;
    }

    public String getGlobalKey(ConfigurationUnit unit) {
        return unit.getUniqueId() + "." + key;
    }

    private String replaceExpressions(PropertiesHolder propertiesHolder, ConfigurationUnit unit) {
        if (value == null) {
            return null;
        }

        final UnitScopePropertyExpression expression =
            new UnitScopePropertyExpression(propertiesHolder, unit);
        final String evaluatedValue = expression.evaluate(value, String.class);
        return evaluatedValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Attribute [");
        if (key != null) {
            sb.append(" key=" + key);
        }
        if (value != null) {
            sb.append(" value=" + value);
        }
        sb.append(" ]");
        return sb.toString();
    }

    public static void anticipateOverwrites(List<Attribute> attributes) {
        HashMap<String, Attribute> uniqueKeyMap = new HashMap<>();
        for (Attribute attribute : attributes) {
            uniqueKeyMap.put(attribute.getKey(), attribute);
        }
        attributes.retainAll(uniqueKeyMap.values());
    }

}
