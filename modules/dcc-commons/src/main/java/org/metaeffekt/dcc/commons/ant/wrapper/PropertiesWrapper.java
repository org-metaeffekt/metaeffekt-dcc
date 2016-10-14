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

import java.util.Properties;

import org.metaeffekt.dcc.commons.mapping.Identifiable;

public class PropertiesWrapper extends AbstractPropertyBasedWrapper {
    final Properties p;
    public PropertiesWrapper(WrapperContext wrapperContext, Properties p) {
        super(wrapperContext);
        this.p = p;
    }
    public PropertyWrapper get(String key, Identifiable identifiable, String defaultValue) {
        return new PropertyWrapper(getWrapperContext(), identifiable, key, p.getProperty(key, defaultValue));
    }
    public PropertyWrapper get(String key) {
        return get(key, null, null);
    }
    public PropertyWrapper get(String key, Identifiable identifiable) {
        return get(key, identifiable, null);
    }
    public Properties getProperties() {
        return p;
    }
}