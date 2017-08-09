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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.spring.xml.BindingFactoryBean;

public class BindingRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(BindingRegistry.class);

    public static final String NAME = "bindingRegistry";

    private Map<String, Set<String>> prov2Req = new HashMap<>();
    private Map<String, Set<String>> req2Prov = new HashMap<>();

    public static String generateUniqueId(Id<UnitId> unitId, Id<CapabilityId> capabilityId) {
        // FIXME JKO maybe replace return value with own Id Type
        return unitId.getValue() + "#" + capabilityId.getValue();
    }

    public void register(String providedCapabilityId, String requiredCapabilityId) {
        doRegister(prov2Req, providedCapabilityId, requiredCapabilityId);
        doRegister(req2Prov, requiredCapabilityId, providedCapabilityId);
    }

    public boolean hasRequiredCapabilityBindings(String reqCapUniqueId) {
        Set<String> boundProvidedCapabilities = req2Prov.get(reqCapUniqueId);
        return boundProvidedCapabilities != null && !boundProvidedCapabilities.isEmpty();
    }

    public Set<String> providedCapabilitiesBoundTo(String reqCapUniqueId) {
        Set<String> provCabs = req2Prov.get(reqCapUniqueId);
        if (provCabs != null) {
            return Collections.unmodifiableSet(provCabs);
        } else {
            return Collections.emptySet();
        }
    }

    public static BindingRegistry getInstance(ListableBeanFactory beanFactory) {

        if (beanFactory.containsBean(BindingRegistry.NAME)) {
            BindingRegistry bindingRegistry = (BindingRegistry) beanFactory.getBean(BindingRegistry.NAME);
            return  bindingRegistry;
        }

        Map<String, BindingFactoryBean> allBindingFactories = beanFactory.getBeansOfType(BindingFactoryBean.class);

        BindingRegistry bindingRegistry = new BindingRegistry();

        for (BindingFactoryBean bfb : allBindingFactories.values()) {

            String source = generateUniqueId(bfb.getSourceUnit().getId(), bfb.getSourceCapabilityId());
            String target = generateUniqueId(bfb.getTargetUnit().getId(), bfb.getTargetCapabilityId());
            bindingRegistry.register(source, target);

            LOG.debug("Explicit binding found: [{}] -> [{}]", source, target);
        }

        registerWithSpringContext(bindingRegistry, beanFactory);

        return bindingRegistry;
    }

    private void doRegister(Map<String, Set<String>>  map, String key, String value) {

        Set<String> values = map.get(key);

        if (values == null) {
            values = new HashSet<>();
        }

        values.add(value);
        map.put(key, values);
    }

    private static void registerWithSpringContext(BindingRegistry bindingRegistry, ListableBeanFactory beanFactory) {

        if (beanFactory instanceof ConfigurableListableBeanFactory) {
            LOG.debug("Registering bindingRegistry with Spring Application Context");
            ConfigurableListableBeanFactory configurableListableBeanFactory = (ConfigurableListableBeanFactory) beanFactory;
            configurableListableBeanFactory.registerSingleton("bindingRegistry", bindingRegistry);
        }
    }
}
