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

/**
 * Created by i001450 on 17.07.14.
 */
public class AttributeKey implements Documented {

    final private String key;

    final private File origin;

    final private String description;

    final private boolean optional;
    
    final private String defaultValue;

    /**
     * Create a mandatory AttributeKey
     * @param key
     */
    public AttributeKey(String key) {
        this(key, null, null, false, null);
    }

    public AttributeKey(String key, File origin, String description, boolean optional) {
        this(key, origin, description, optional, null);
    }

    public AttributeKey(String key, File origin, String description, boolean optional, String defaultValue) {
        this.key=key;
        this.origin = origin;
        this.description = description;
        this.optional=optional;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public boolean isOptional() {
        return optional;
    }

    public File getOrigin() {
        return origin;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    /**
     * Two AttributeKeys are equal if their keys are equal
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AttributeKey that = (AttributeKey) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }

    @Override
    public String toString() {
        return key;
    }

    public String applyDefaultIfNecessary(final String value) {
        if (value == null && getDefaultValue() != null) {
            return getDefaultValue();
        }
        return value;
    }
    
}
