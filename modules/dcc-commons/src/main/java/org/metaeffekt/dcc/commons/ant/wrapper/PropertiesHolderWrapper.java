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

import org.metaeffekt.dcc.commons.mapping.Identifiable;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;

public abstract class PropertiesHolderWrapper extends AbstractPropertyBasedWrapper {

    private final PropertiesHolder propertiesHolder;

    public PropertiesHolderWrapper(WrapperContext wrapperContext, PropertiesHolder p) {
        super(wrapperContext);
        this.propertiesHolder = p;
    }

    public PropertyWrapper get(Identifiable identifiable, String key, String defaultValue) {
        return new PropertyWrapper(getWrapperContext(), identifiable, key,
                propertiesHolder.getProperty(identifiable, key, defaultValue));
    }

    public PropertyWrapper get(String key, String defaultValue) {
        return new PropertyWrapper(getWrapperContext(), null, key, propertiesHolder.getProperty(key, defaultValue));
    }

    public PropertyWrapper get(String key) {
        return get(key, null);
    }

    protected PropertiesHolder getPropertiesHolder() {
        return propertiesHolder;
    }

}
