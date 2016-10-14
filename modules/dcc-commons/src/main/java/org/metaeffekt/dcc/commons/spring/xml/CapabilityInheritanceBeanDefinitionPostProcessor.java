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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;

import org.metaeffekt.dcc.commons.mapping.AttributeKey;
import org.metaeffekt.dcc.commons.mapping.CapabilityDefinition;
import org.metaeffekt.dcc.commons.mapping.CapabilityDefinitionReference;

/**
 * BeanDefinitionPostProcessor which processes all {@link CapabilityDefinitionReference}s and enhances the
 * attribute key list of the respective {@link CapabilityDefinition}.
 *
 * @author Jochen K.
 */
public class CapabilityInheritanceBeanDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor {

    List<String> definitionRefsInProgress = new ArrayList<>();


    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        List<String> definitionNamesToProcess = new ArrayList<>();
        String[] beanDefinitionNames = registry.getBeanDefinitionNames();
        for(String definitionName : beanDefinitionNames) {
            if (StringUtils.startsWith(definitionName, DCCConfigurationBeanDefinitionParser.CAPABILITY_DEFINITION_NAME_PREFIX)) {
                definitionNamesToProcess.add(definitionName);
                // enrich BeanDefinition
                enrichBeanDefinition(registry, definitionName);
            }
        }

        for(String definitionNameToProcess : definitionNamesToProcess) {
            processInheritance(registry, definitionNameToProcess);
        }
    }

    @SuppressWarnings("unchecked")
    private List<AttributeKey> processInheritance(BeanDefinitionRegistry registry, String definitionName) {
        BeanDefinition definition = registry.getBeanDefinition(definitionName);
        List<AttributeKey> inheritedKeys = null;
        if (definition != null) {
            // explicitly grab a reference to the List from the BeanDefinition
            inheritedKeys = (List<AttributeKey>) definition.getPropertyValues()
                    .getPropertyValue(DCCConfigurationBeanDefinitionParser.PROPERTY_CAPABILITY_DEF_ATTRIBUTE_KEYS).getValue();

            List<CapabilityDefinitionReference> ancestorsAttributeReference =
                    (List<CapabilityDefinitionReference>) definition.getAttribute(
                            DCCConfigurationBeanDefinitionParser.PROPERTY_CAPABILITY_DEF_ANCESTORS);

            if (!ancestorsAttributeReference.isEmpty()) {
                List<CapabilityDefinitionReference> ancestors = new ArrayList<>();
                ancestors.addAll(ancestorsAttributeReference);
                for(CapabilityDefinitionReference ancestor : ancestors) {
                    // recursive call to get additional attribute keys
                    String uniqueDefinitionRefId = createUniqueAncestorReferenceId(definitionName, ancestor);
                    if (!definitionRefsInProgress.contains(uniqueDefinitionRefId)) {
                        definitionRefsInProgress.add(uniqueDefinitionRefId);
                        List<AttributeKey> ancestorsAttributeKeys = processInheritance(registry,
                                DCCConfigurationBeanDefinitionParser.createCapabilityDefBeanName(
                                        ancestor.getReferencedCapabilityDefId()));
                        for (AttributeKey ancestorAttributeKey : ancestorsAttributeKeys) {
                            AttributeKey localAttributeKey = prefixAttributeKeyIfAppropriate(ancestorAttributeKey, ancestor.getPrefix());
                            if (inheritedKeys.contains(localAttributeKey)) {
                                throw new BeanDefinitionValidationException(
                                        "Duplicate attribute key ["+localAttributeKey+
                                                "] detected while processing CapabilityDefinitionReference ["
                                                +uniqueDefinitionRefId+"].");
                            }
                            // add to attributeKeys - List is a reference to BeanDefinition
                            inheritedKeys.add(localAttributeKey);
                        }
                        definitionRefsInProgress.remove(uniqueDefinitionRefId);
                    } else {
                        throw new BeanDefinitionValidationException("Cyclic inheritance definition detected. "
                                + "DefinitionRefsInProgress (BeanDefinitionName:ReferencePrefix#ReferenceCapabilityId) "
                                + "[" + definitionRefsInProgress + "]");
                    }
                    ancestorsAttributeReference.remove(ancestor);
                }
            }
        }

        return (inheritedKeys != null) ? inheritedKeys : Collections.<AttributeKey>emptyList();
    }

    @SuppressWarnings("unchecked")
    private void enrichBeanDefinition(BeanDefinitionRegistry registry, String definitionName) {
        BeanDefinition definition = registry.getBeanDefinition(definitionName);
        List<CapabilityDefinitionReference> ancestors = new ArrayList<>();
        ancestors.addAll((List<CapabilityDefinitionReference>) definition.getPropertyValues()
                .getPropertyValue(DCCConfigurationBeanDefinitionParser.PROPERTY_CAPABILITY_DEF_ANCESTORS).getValue());
        definition.setAttribute(DCCConfigurationBeanDefinitionParser.PROPERTY_CAPABILITY_DEF_ANCESTORS, ancestors);
    }

    private AttributeKey prefixAttributeKeyIfAppropriate(AttributeKey attributeKey, String prefix) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotEmpty(prefix.trim())) {
            builder.append(prefix);
            builder.append(".");
        }
        builder.append(attributeKey.getKey());
        return new AttributeKey(builder.toString(), 
                attributeKey.getOrigin(), attributeKey.getDescription(), 
                attributeKey.isOptional(), attributeKey.getDefaultValue());
    }

    private String createUniqueAncestorReferenceId(String definitionName, CapabilityDefinitionReference ancestor) {
        StringBuilder builder = new StringBuilder(definitionName);
        builder.append(":");
        builder.append(ancestor.getPrefix());
        builder.append("#");
        builder.append(ancestor.getReferencedCapabilityDefId());
        return builder.toString();
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing to do for us ...
    }
}
