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
package org.metaeffekt.dcc.commons.spring.xml;

import static org.metaeffekt.dcc.commons.mapping.BindingRegistry.generateUniqueId;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import org.metaeffekt.dcc.commons.mapping.BindingRegistry;
import org.metaeffekt.dcc.commons.mapping.Capability;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.RequiredCapability;

/**
 * @author Alexander D.
 */
public class AutoBindFactoryPostProcessor implements BeanFactoryPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AutoBindFactoryPostProcessor.class);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

       BindingRegistry bindingRegistry = BindingRegistry.getInstance(beanFactory);

        Map<String, Set<ConfigurationUnit>> providedCapabilityRegistry = createProvidedCapabilityRegistry(beanFactory, bindingRegistry);

        Map<String, ConfigurationUnit> allUnits = beanFactory.getBeansOfType(ConfigurationUnit.class);

        for (ConfigurationUnit unitRequiringCapability : allUnits.values()) {

            if (!unitRequiringCapability.isAbstract()) {

                for (RequiredCapability reqCab : unitRequiringCapability.getRequiredCapabilities()) {

                    String reqCabUId = generateUniqueId(unitRequiringCapability.getId(), reqCab.getId());

                    if (!bindingRegistry.hasRequiredCapabilityBindings(reqCabUId)) {

                        String provCapDefId = reqCab.getCapabilityDefinition().getId();

                        Set<ConfigurationUnit> unitsProvidingRequiredCapability = providedCapabilityRegistry.get(provCapDefId);

                        if (unitsProvidingRequiredCapability == null || unitsProvidingRequiredCapability.isEmpty()) {
                            if (!reqCab.isOptional()) {
                                throw new IllegalStateException("Auto-bind could not find a unit providing a capability of type '" +
                                        provCapDefId + "', which is required by '" + unitRequiringCapability.getUniqueId() + "'");
                            }
                        } else if (unitsProvidingRequiredCapability.size() > 1) {
                            throw new IllegalStateException("Auto-bind found an ambiguity: " +
                                    "multiple provided capabilities of type '" + provCapDefId + "' found in " + unitsProvidingRequiredCapability.size()
                                    + " different units " + idsOfUnits(unitsProvidingRequiredCapability));
                        } else if (unitsProvidingRequiredCapability.size() == 1) {
                            ConfigurationUnit unitProvidingRequiredCapability = unitsProvidingRequiredCapability.iterator().next();

                            Capability provCab = unitProvidingRequiredCapability.findProvidedCapabilityWithCapabilityDefinition(provCapDefId);
                            createBinding(beanFactory, unitProvidingRequiredCapability, provCab, unitRequiringCapability, reqCab);
                            bindingRegistry.register(generateUniqueId(unitProvidingRequiredCapability.getId(), provCab.getId()), reqCabUId);
                            LOG.debug("Automatically added binding [{} -> {}]", generateUniqueId(unitProvidingRequiredCapability.getId(), provCab.getId()), reqCabUId);
                        } 
                    }
                }
            }
        }
    }

    private String idsOfUnits(Set<ConfigurationUnit> units) {

        StringBuilder sb = new StringBuilder("[");
        String prefix = "";
        for (ConfigurationUnit unit : units) {
            sb.append(prefix);
            prefix = ",";
            sb.append(unit.getId());
        }

        sb.append("]");

        return sb.toString();
    }

    private void createBinding(ConfigurableListableBeanFactory beanFactory, ConfigurationUnit sourceUnit, Capability sourceCapability, ConfigurationUnit targetUnit, Capability targetCapability) {

        BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;

        BeanDefinitionBuilder bindingBeanBuilder = BeanDefinitionBuilder.genericBeanDefinition(BindingFactoryBean.class);
        bindingBeanBuilder.addPropertyValue("description", createDescription(sourceCapability, sourceUnit, targetCapability, targetUnit));
        bindingBeanBuilder.addPropertyValue("sourceCapabilityId", sourceCapability.getId());
        bindingBeanBuilder.addPropertyReference("sourceUnit", sourceUnit.getId().getValue());
        bindingBeanBuilder.addPropertyValue("targetCapabilityId", targetCapability.getId());
        bindingBeanBuilder.addPropertyReference("targetUnit", targetUnit.getId().getValue());
        bindingBeanBuilder.addPropertyValue("origin", "autoBound");
        bindingBeanBuilder.addPropertyValue("autoBound", true);

        String bindingBeanName = String.format("autoBound%s%s%s%s",sourceUnit.getId(),sourceCapability.getId(),targetUnit.getId(),targetCapability.getId());
        beanDefinitionRegistry.registerBeanDefinition(bindingBeanName, bindingBeanBuilder.getBeanDefinition());
    }

    private String createDescription(Capability sourceCapability, ConfigurationUnit sourceUnit, Capability targetCapability, ConfigurationUnit targetUnit) {
        return String.format("Automatically created binding. Connecting required capability [%s] of unit [%s] to the [%s] capability provided by [%s].",
                sourceCapability.getId(), sourceUnit.getId(), targetCapability.getId(), targetUnit.getId());
    }

    private Map<String, Set<ConfigurationUnit>> createProvidedCapabilityRegistry(ListableBeanFactory beanFactory, BindingRegistry bindingRegistry) {

        Map<String, Set<ConfigurationUnit>> providedCapabilityRegistry = new HashMap<>();

        Map<String, ConfigurationUnit> allUnits = beanFactory.getBeansOfType(ConfigurationUnit.class);

        for (ConfigurationUnit unit : allUnits.values()) {

            if (!unit.isAbstract()) {

                for (Capability provCap : unit.getProvidedCapabilities()) {

                    String provCapDefId = provCap.getCapabilityDefinition().getId();

                    Set<ConfigurationUnit> providingUnits = providedCapabilityRegistry.get(provCapDefId);
                    if (providingUnits == null) {
                        providingUnits = new HashSet<>();
                    }
                    else {
                        //TODO this might be a viable scenario in the future, if we support auto binding by capability ID
                        if (providingUnits.contains(unit)) {
                            throw new IllegalStateException("Auto-bind ambiguity: unit '"+
                                    unit.getId() + "' provides multiple capabilities of type '" + provCapDefId + "'.");
                        }
                    }
                    providingUnits.add(unit);
                    providedCapabilityRegistry.put(provCapDefId, providingUnits);
                }
            }
        }

        return providedCapabilityRegistry;
    }
}
