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
package org.metaeffekt.dcc.commons.spring.xml;

import static org.metaeffekt.dcc.commons.mapping.BindingRegistry.generateUniqueId;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;

import org.metaeffekt.dcc.commons.DccProperties;
import org.metaeffekt.dcc.commons.DccUtils;
import org.metaeffekt.dcc.commons.mapping.Attribute;
import org.metaeffekt.dcc.commons.mapping.AttributeKey;
import org.metaeffekt.dcc.commons.mapping.Binding;
import org.metaeffekt.dcc.commons.mapping.BindingRegistry;
import org.metaeffekt.dcc.commons.mapping.Capability;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.IsTrueAssert;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.mapping.RequiredCapability;
import org.metaeffekt.dcc.commons.mapping.UniqueAssert;

/**
 * Proudly crafted by i001450 on 18.07.14.
 */
public class ProfileValidationBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    
    private static final String INDENT = System.lineSeparator() + "    ";

    private static final Logger LOG = LoggerFactory.getLogger(ProfileValidationBeanFactoryPostProcessor.class);

    private static final Pattern PREDEFINED_PROPERTIES_REGEX = Pattern.compile("\\$\\{dcc\\..+\\}");

    private BindingRegistry bindingRegistry;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        Profile profile = (Profile) beanFactory.getBean(
            DCCConfigurationBeanDefinitionParser.THE_ONE_TRUE_PROFILE_BEAN_NAME);

        if (Profile.Type.DEPLOYMENT == profile.getType()) {

            // evaluate the profile (including deployment properties)
            final PropertiesHolder propertiesHolder = profile.createPropertiesHolder(true);
            profile.evaluate(propertiesHolder);
            
            this.bindingRegistry = BindingRegistry.getInstance(beanFactory);

            List<String> messages = new LinkedList<>();

            for (ConfigurationUnit unit : profile.getUnits()) {
                // TODO validate if all mappings are well-formed
                validateProvidedCapabilities(unit, profile, propertiesHolder, messages);
                validateRequiredCapabilities(unit, profile, messages);
            }

            messages.addAll(checkAsserts(profile, propertiesHolder));

            // checking for unused attributes and properties (only when validation is on)
            if (Boolean.parseBoolean(System.getProperty(DccProperties.DCC_SYSTEM_PROPERTY_PROFILE_VALIDATION, "true"))) {
                
                boolean hasUnusedAttributes = false;
                for (ConfigurationUnit unit : profile.getUnits(false)) {
                    for (Attribute attribute : unit.getAttributes()) {
                        String attributeId = DccUtils.deriveAttributeIdentifier(unit, attribute.getKey());
                        if (!propertiesHolder.isPropertyRelevant(attributeId)) {
                            LOG.warn("Unused attribute [{}] in unit [{}].", 
                                attribute.getKey(), unit.getId());
                            hasUnusedAttributes = true;
                        }
                    }
                }
                if (hasUnusedAttributes) {
                    LOG.warn("Unused attributes are not evaluated and may indicate issues within the profile definition.");
                    LOG.warn("Please review the profile with id '{}'.", profile.getId());
                }
                
                // check on solution property usage
                checkForUnusedProperties("solution", profile.getSolutionProperties(), 
                    propertiesHolder, DccProperties.DCC_SOLUTION_PROPERTIES_WHITELIST, profile.getSolutionPropertiesFile(), profile);
                checkForUnusedProperties("deployment", profile.getDeploymentProperties(), 
                    propertiesHolder, DccProperties.DCC_DEPLOYMENT_PROPERTIES_WHITELIST, profile.getDeploymentPropertiesFile(), profile);

                checkForUnresolvedVariables(profile, propertiesHolder);
            }
            
            Collections.sort(messages);
            StringBuilder messageBuilder = new StringBuilder();
            int i = 1;
            for (String string : messages) {
                messageBuilder.append(System.lineSeparator());
                messageBuilder.append(System.lineSeparator());
                
                messageBuilder.append(i++);
                messageBuilder.append(") ");
                
                messageBuilder.append(string.replace("${INDENT}", INDENT));
            }
            messageBuilder.append(System.lineSeparator());

            if (messages.size() > 0) {
                String errorMessage =
                    String.format("%d validation error(s) occurred: %s", messages.size(), messageBuilder.toString());
                LOG.error(errorMessage);
                if (Boolean.parseBoolean(System.getProperty(DccProperties.DCC_SYSTEM_PROPERTY_PROFILE_VALIDATION, "true"))) {
                    throw new BeanDefinitionValidationException("Stopping profile evaluation due to validation errors.");
                }
            }
        }

    }

    private boolean checkForUnusedProperties(String context, Properties referenceProperties, 
            PropertiesHolder propertiesHolder, String[] propertiesWhitelist, File propertiesFile, Profile profile) {
        boolean hasUnusedProperties = false;
        if (referenceProperties != null) {
            for (Object key : referenceProperties.keySet()) {
                if (Arrays.binarySearch(propertiesWhitelist, key) < 0) {
                    if (!propertiesHolder.isPropertyRelevant(key.toString())) {
                        LOG.warn("Unused [{}] property: [{}].", context, key);
                        hasUnusedProperties = true;
                    }
                }
            }
        }
        
        if (hasUnusedProperties) {
            LOG.warn("Unused properties may indicate issues within the configuration.");
            LOG.warn("Please review the {} properties in file '{}'.", context, propertiesFile != null ? 
                profile.getRelativePath(propertiesFile) : "<no file available>");
        }
        
        return hasUnusedProperties;
    }

    private boolean checkForUnresolvedVariables(Profile profile, PropertiesHolder propertiesHolder) {
        boolean hasUnresolvedVariables = false;
        
        // check the properties for a unit/capability does not contain any foreign unresolved placeholders
        for (ConfigurationUnit unit : profile.getUnits(false)) {
            Properties p = propertiesHolder.getProperties(unit);
            hasUnresolvedVariables |= checkUnresolvedVariables(String.format("unit [%s]", unit.getId()), p);
            for (Capability capability : unit.getProvidedCapabilities()) {
                p = propertiesHolder.getProperties(capability);
                hasUnresolvedVariables |= checkUnresolvedVariables(String.format("provided capability [%s] in unit [%s]", 
                    capability.getId(), unit.getId()), p);
            }
        }

        if (hasUnresolvedVariables) {
            LOG.warn("Unresolved variables may indicate issues within the configuration.");
            LOG.warn("Please review the profile with id '{}'.", profile.getId());
        }

        return hasUnresolvedVariables;
    }

    protected boolean checkUnresolvedVariables(String context, Properties p) {
        Enumeration<?> enumeration = p.propertyNames();
        boolean hasUnresolvedVariables = false;
        while (enumeration.hasMoreElements()) {
            Object key = enumeration.nextElement();
            String value = p.getProperty(key.toString());   
            value = value.replace("${dcc.", "{");
            if (value.contains("${")) {
                if (!hasUnresolvedVariables) {
                    LOG.warn("The properties derived for {} contains unresolved variables:", context);
                    hasUnresolvedVariables = true;
                }
                LOG.warn("  Value of key [{}]: {}", key, value);
            }
        }
        return hasUnresolvedVariables;
    }

    private List<String> checkAsserts(final Profile profile, final PropertiesHolder propertiesHolder) {
        final List<String> messages = new ArrayList<>();

        checkIsTrueAsserts(profile, messages, propertiesHolder);
        checkUniqueAsserts(profile, messages, propertiesHolder);

        return messages;
    }

    private void checkIsTrueAsserts(Profile profile, final List<String> messages,
            final PropertiesHolder propertiesHolder) {
        for (IsTrueAssert isTrueAssert : profile.getAsserts(IsTrueAssert.class)) {
            final Boolean result = isTrueAssert.evaluate(propertiesHolder, profile);
            if (result == null) {
                continue;
            }
            if (!Boolean.TRUE.equals(result)) {
                final StringBuilder errorMessage = new StringBuilder();
                if (!StringUtils.isEmpty(isTrueAssert.getMessage())) {
                    errorMessage.append(isTrueAssert.getMessage());
                    errorMessage.append(" ");
                }
                errorMessage.append("IsTrue assertion failed, value expression evaluated to false: [%s]"
                        + "${INDENT} Unit [%s].");
                messages.add(String.format(errorMessage.toString(), isTrueAssert.getValue(), isTrueAssert.getUnit()));
            }
        }
    }

    private void checkUniqueAsserts(Profile profile, final List<String> messages,
            final PropertiesHolder propertiesHolder) {
        final Map<String, UniqueAssert> uniqueAsserts = new HashMap<>();
        for (UniqueAssert uniqueAssert : profile.getAsserts(UniqueAssert.class)) {
            final String value = uniqueAssert.evaluate(propertiesHolder, profile);

            // Asserts which are evaluated to empty string or null are ignored
            if (StringUtils.isEmpty(value)) {
                continue;
            }

            final String key = value.toLowerCase(Locale.ENGLISH);
            final UniqueAssert matchedAssert = uniqueAsserts.get(key);
            if (matchedAssert != null) {
                final StringBuilder errorMessage = new StringBuilder();
                if (!StringUtils.isEmpty(uniqueAssert.getMessage())) {
                    errorMessage.append(uniqueAssert.getMessage());
                }
                if (!StringUtils.isEmpty(matchedAssert.getMessage())
                        && !matchedAssert.getMessage().equals(uniqueAssert.getMessage())) {
                    errorMessage.append(matchedAssert.getMessage());
                }
                if (errorMessage.length() > 0) {
                    errorMessage.append(" ");
                }
                errorMessage.append("Unique assertion failed. [%s] is not unique."
                        + "${INDENT} Unique assertion: [%s]"
                        + "${INDENT} Matched assertion: [%s].");
                messages.add(String.format(errorMessage.toString(), value, uniqueAssert, matchedAssert));
            } else {
                uniqueAsserts.put(key, uniqueAssert);
            }
        }
    }

    private void validateProvidedCapabilities(ConfigurationUnit unit, Profile profile, PropertiesHolder propertiesHolder,
            List<String> messages) {
        for (Capability provCab : unit.getProvidedCapabilities()) {

            if (provCab.getCapabilityDefinition().isAbstract()) {
                messages.add(String.format(
                    "Provided capability [%s] references an abstract capability definition."
                    + "${INDENT} Capability definition: [%s]"
                    + "${INDENT} Profile: [%s]",
                    provCab.getUniqueId(), provCab.getCapabilityDefinition().getId(),
                    profile.getRelativePath(provCab.getCapabilityDefinition())));
            }
            else {
                for (AttributeKey attributeKey : provCab.getCapabilityDefinition()
                        .getAttributeKeys()) {
                    if (!attributeKey.isOptional()) {
                        String value = propertiesHolder.getProperty(provCab, attributeKey.getKey());

                        // the value may contain a non-resolved variable.
                        if (value != null && !StringUtils.isBlank(value)) {
                            if (!PREDEFINED_PROPERTIES_REGEX.matcher(value).matches()) {
                                value = value.replaceAll("\\$\\{.*\\}", "");
                                if (StringUtils.isBlank(value)) {
                                    value = null;
                                }
                            }
                        }

                        if (value == null) {
                            messages.add(String.format(
                                "No value defined for mandatory attribute [%s]"
                                + "${INDENT} Provided capability: [%s]"
                                + "${INDENT} Profile: [%s]",
                                attributeKey.getKey(), provCab.getUniqueId(),
                                profile.getRelativePath(unit)));
                        }
                    }
                }
            }
        }
    }

    private void validateRequiredCapabilities(ConfigurationUnit unit, Profile profile,
            List<String> messages) {

        for (Capability cap : unit.getRequiredCapabilities()) {

            RequiredCapability reqCap = (RequiredCapability) cap;

            if (reqCap.getCapabilityDefinition().isAbstract()) {
                messages.add(String.format(
                    "Required capability [%s] references an abstract capability definition."
                    + "${INDENT} Capability definition: [%s]"
                    + "${INDENT} Profile: [%s].",
                    reqCap.getUniqueId(), reqCap.getCapabilityDefinition().getId(),
                    profile.getRelativePath(reqCap.getCapabilityDefinition())));
            }

            if (!reqCap.isMultipleBindingsAllowed()) {
                Set<String> provCapsBoundToThis =
                    bindingRegistry.providedCapabilitiesBoundTo(generateUniqueId(unit.getId(),
                            reqCap.getId()));
                if (provCapsBoundToThis.size() > 1) {
                    messages.add(String.format(
                        "Multiple bindings are not allowed for required capability [%s].",
                        reqCap.getUniqueId()));
                }
            }

            if (!reqCap.isOptional()) {

                if (!isBound(reqCap, profile)) {
                    messages.add(String.format(
                        "No binding definition for required capability [%s].",
                        reqCap.getUniqueId()));
                }
            }
        }
    }

    private boolean isBound(Capability reqCab, Profile profile) {

        for (Binding binding : profile.getBindings()) {

            if (reqCab.getUniqueId().equals(binding.getTargetCapability().getUniqueId())) {
                return true;
            }
        }
        return false;
    }
}
