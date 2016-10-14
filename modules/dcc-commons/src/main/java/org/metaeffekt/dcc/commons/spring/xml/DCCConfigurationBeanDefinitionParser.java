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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.DccProperties;
import org.metaeffekt.dcc.commons.ant.PropertyUtils;
import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;
import org.metaeffekt.dcc.commons.domain.Type.PackageId;
import org.metaeffekt.dcc.commons.mapping.Attribute;
import org.metaeffekt.dcc.commons.mapping.Attribute.AttributeType;
import org.metaeffekt.dcc.commons.mapping.AttributeKey;
import org.metaeffekt.dcc.commons.mapping.AttributeMapper;
import org.metaeffekt.dcc.commons.mapping.Capability;
import org.metaeffekt.dcc.commons.mapping.CapabilityDefinition;
import org.metaeffekt.dcc.commons.mapping.CapabilityDefinitionExtensionReference;
import org.metaeffekt.dcc.commons.mapping.CapabilityDefinitionReference;
import org.metaeffekt.dcc.commons.mapping.CommandDefinition;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.ExpressionAttributeMapper;
import org.metaeffekt.dcc.commons.mapping.HostRestriction;
import org.metaeffekt.dcc.commons.mapping.IsTrueAssert;
import org.metaeffekt.dcc.commons.mapping.Mapping;
import org.metaeffekt.dcc.commons.mapping.PrerequisiteAssert;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.Provision;
import org.metaeffekt.dcc.commons.mapping.ProvisionRestriction;
import org.metaeffekt.dcc.commons.mapping.RequiredCapability;
import org.metaeffekt.dcc.commons.mapping.SourceToTargetCapabilityAttributeMapper;
import org.metaeffekt.dcc.commons.mapping.UniqueAssert;
import org.metaeffekt.dcc.commons.mapping.UnitToCapabilityAttributeMapper;

public class DCCConfigurationBeanDefinitionParser extends AbstractBeanDefinitionParser {

    static final String THE_ONE_TRUE_PROFILE_BEAN_NAME = Profile.class.getName();

    static final String ELEMENT_ID = "id";
    static final String ELEMENT_DEPLOYMENT_ID = "deployment-id";
    static final String ELEMENT_TYPE = "type";
    static final String ELEMENT_DESCRIPTION = "description";
    static final String ELEMENT_UNIT_ATTRIBUTES_ATTRIBUTE = "attribute";
    static final String ELEMENT_UNIT_ATTRIBUTES = "attributes";
    static final String ELEMENT_PROPERTIES = "properties";
    static final String ELEMENT_PROPS_LOCATION_ATTRIBUTE = "location";

    // XML elements
    static final String ELEMENT_IF = "if";
    static final String ELEMENT_IMPORT = "import";
    static final String ELEMENT_PROFILE = "profile";
    static final String ELEMENT_AUTO_BIND = "auto-bind";
    static final String ELEMENT_CAPABILITY_DEF = "capability-definition";
    static final String ELEMENT_INHERIT_DEF = "inherit";

    static final String ELEMENT_ATTRIBUTE_KEY = "attribute-key";
    static final String ELEMENT_UNIT = "unit";
    static final String ELEMENT_UNIT_REQUIRED_CAPABILITY = "required-capability";
    static final String ELEMENT_UNIT_PROVIDED_CAPABILITY = "provided-capability";
    static final String ELEMENT_UNIT_COMMAND = "command";
    static final String ELEMENT_ASSERTS = "asserts";

    static final String ELEMENT_UNIT_MAPPINGS = "mappings";
    static final String ELEMENT_BINDING = "binding";
    static final String ELEMENT_BINDING_TARGET = "target";

    static final String ELEMENT_BINDING_SOURCE = "source";
    // XML attributes
    static final String ATTRIBUTE_KEY_OPTIONAL = "optional";
    static final String ATTRIBUTE_KEY_DEFAULT = "default";
    static final String ATTRIBUTE_KEY_MULTIPLE_BINDINGS = "multipleBindingsAllowed";
    static final String ATTRIBUTE_KEY_IDENTIFIES_HOST = "identifiesHost";
    static final String ATTRIBUTE_IDENTIFIES_HOST = "identifiesHost";

    static final String ATTRIBUTE_ATTRIBUTE_KEY_KEY = "key";
    static final String ATTRIBUTE_UNIT_EXTENDS = "extends";
    static final String ATTRIBUTE_UNIT_ABSTRACT = "abstract";
    static final String ATTRIBUTE_UNIT_TYPE = "type";
    static final String ATTRIBUTE_CAPABILITY_DEFINITION_REF = "definitionRef";

    static final String ATTRIBUTE_PREFIX = "prefix";

    // Bean/Java properties
    static final String PROPERTY_ORIGIN = "origin";
    static final String PROPERTY_UNIT_PARENT_ID = "parentId";
    static final String PROPERTY_UNIT_ABSTRACT = "abstract";
    static final String PROPERTY_UNIT_PROVIDED_CAPABILITIES = "providedCapabilities";
    static final String PROPERTY_UNIT_REQUIRED_CAPABILITIES = "requiredCapabilities";
    static final String PROPERTY_CAPABILITY_DEF_ATTRIBUTE_KEYS = "attributeKeys";
    static final String PROPERTY_CAPABILITY_DEF_ANCESTORS = "ancestors";
    static final String PROPERTY_UNIT_MAPPINGS = ELEMENT_UNIT_MAPPINGS;
    static final String PROPERTY_UNIT_COMMANDS = "commands";
    static final String PROPERTY_UNIT_ASSERTS = "asserts";
    static final String PROPERTY_UNIT_ATTRIBUTES = ELEMENT_UNIT_ATTRIBUTES;
    static final String PROPERTY_DESCRIPTION = "description";
    static final String PROPERTY_CONTRIBUTIONS = "contributions";
    static final String PROPERTY_CAPABILITIES = "capabilities";
    static final String PROPERTY_REQUISITIONS = "requisitions";
    static final String PROPERTY_PROVISIONS = "provisions";
    static final String PROPERTY_DEPLOYMENT_PROPERTIES_PATH = "deploymentPropertiesPath";
    static final String PROPERTY_SOLUTION_PROPERTIES_PATH = "solutionPropertiesPath";
    static final String PROPERTY_SOLUTION_DIR = "solutionDir";

    static final String CAPABILITY_DEFINITION_NAME_PREFIX = "capabilityDefinition#";

    public static final IfBeanDefinitionParser IF_BEAN_DEFINITION_PARSER = new IfBeanDefinitionParser();
    public static final ImportBeanDefinitionParser IMPORT_BEAN_DEFINITION_PARSER = new ImportBeanDefinitionParser();

    @Override
    protected AbstractBeanDefinition parseInternal(Element profileElement, ParserContext parserContext) {
        
        BeanDefinitionRegistry registry = parserContext.getRegistry();
        
        registerProcessedImportsResettingBeanFactoryPostProcessor(registry);
        registerAutoBindBeanFactoryPostProcessorIfRequired(profileElement, registry);
        registerDependencyGraphCalculatingBeanFactoryPostProcessor(registry);
        registerProfileValidationBeanFactoryPostProcessor(registry);
        registerCapabilityInheritancePostProcessor(registry);

        String profileElementId = extractProfileId(profileElement);
        String deploymentId = extractDeploymentId(profileElement, parserContext);
        String profileElementDescription = extractDescription(profileElement);
        String solutionPropertiesPath = extractSolutionPropertiesPath(profileElement, parserContext);
        String deploymentPropertiesPath = extractDeploymentPropertiesPath(profileElement, parserContext);
        File origin = determineOrigin(profileElementId, parserContext);
        String profileType = extractType(profileElement);
        
        if ("deployment".equalsIgnoreCase(profileType)) {
            if (deploymentId == null) {
                // fallback to profile id
                deploymentId = profileElementId;
            }
        }
        
        // evaluate path
        if (solutionPropertiesPath != null || deploymentPropertiesPath != null) {
            try {
                // derive path from currently parsed file
                File resourceFile = parserContext.getReaderContext().getResource().getFile();
                File parentFile = resourceFile.getParentFile();
                
                // apply to solution properties if required
                if (!StringUtils.isEmpty(solutionPropertiesPath)) {
                    solutionPropertiesPath = new File(parentFile, solutionPropertiesPath).getPath();
                }
                if (!StringUtils.isEmpty(deploymentPropertiesPath)) {
                    deploymentPropertiesPath = new File(parentFile, deploymentPropertiesPath).getPath();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Cannot resolve path for profile properties.", e);
            }
        }

        deploymentId = overwriteDeploymentIdFromDeploymentProperties(deploymentPropertiesPath, deploymentId);
        
        registerProfileFactoryBeanIfRequired(profileElementId, deploymentId, profileType, profileElementDescription, 
                origin, registry, 
                solutionPropertiesPath, deploymentPropertiesPath,
                parserContext);
        
        parseImportsAndIfs(profileElement, parserContext);
        parseAndRegisterCapabilityDefinitions(profileElement, origin, registry, parserContext);

        parseAndRegisterUnits(profileElement, origin, registry, parserContext);
        parseAndRegisterBindings(profileElement, origin, registry, parserContext);
        parseAndRegisterGlobalAsserts(profileElement, registry);

        return null;
    }

    private String overwriteDeploymentIdFromDeploymentProperties(String deploymentPropertiesPath, String deploymentId) {
        if (deploymentPropertiesPath != null) {
            File deploymentPropertiesFile = new File(deploymentPropertiesPath);
            if (deploymentPropertiesFile.exists()) {
                Properties p = PropertyUtils.loadPropertyFile(deploymentPropertiesFile);
                deploymentId = p.getProperty(DccProperties.DCC_DEPLOYMENT_ID, deploymentId);
            }
        }
        return deploymentId;
    }

    private void parseAndRegisterGlobalAsserts(Element profileElement, BeanDefinitionRegistry registry) {
        final ManagedList<AbstractBeanDefinition> asserts = parseAsserts(profileElement);
        for (AbstractBeanDefinition def : asserts) {
            //TODO generate unique name
            registry.registerBeanDefinition(UUID.randomUUID().toString(), def);
        }
    }

    private boolean deploymentProfile(Element profileElement) {
        return Profile.Type.DEPLOYMENT.name().equalsIgnoreCase(extractType(profileElement));
    }

    private boolean autoBindIsEnabled(Element profileElement) {
        Element autoBindElement = DomUtils.getChildElementByTagName(profileElement, ELEMENT_AUTO_BIND);
        return (autoBindElement != null);
    }


    private String extractType(Element profileElement) {
        Element concreteTypeElement = profileTypeElement(profileElement);
        if (concreteTypeElement != null) {
            return concreteTypeElement.getNodeName();
        }
        return Profile.Type.BASE.name();
    }

    private String extractProfileId(Element profileElement) {
        String profileId = DomUtils.getChildElementValueByTagName(profileElement, ELEMENT_ID);
        if (StringUtils.isEmpty(profileId)) {
            throw new BeanCreationException("A profile must have an <id> element with a profile id as value");
        }
        return profileId;
    }

    private String extractDeploymentPropertiesPath(Element profileElement, ParserContext parserContext) {
        if (Profile.Type.DEPLOYMENT.name().equalsIgnoreCase(extractType(profileElement))) {
            Element profileTypeElement = profileTypeElement(profileElement);
            return extractAttributeFromOptionalElement(profileTypeElement, parserContext, ELEMENT_PROPERTIES, ELEMENT_PROPS_LOCATION_ATTRIBUTE);
        }
        return null;
    }

    private Element profileTypeElement(Element profileElement) {
        Element profileTypeElement = DomUtils.getChildElementByTagName(profileElement, ELEMENT_TYPE);
        if (profileTypeElement != null) {
            Element concreteTypeElement = DomUtils.getChildElements(profileTypeElement).get(0);
            return concreteTypeElement;
        }
        return null;
    }

    private String extractSolutionPropertiesPath(Element profileElement, ParserContext parserContext) {
        if (Profile.Type.SOLUTION.name().equalsIgnoreCase(extractType(profileElement))) {
            Element profileTypeElement = profileTypeElement(profileElement);
            return extractAttributeFromOptionalElement(profileTypeElement, parserContext, ELEMENT_PROPERTIES, ELEMENT_PROPS_LOCATION_ATTRIBUTE);
        }
        return null;
    }

    private String extractDeploymentId(Element profileElement, ParserContext parserContext) {
        Element element = profileTypeElement(profileElement);
        if (element != null) {
            final Element deploymentIdElem = DomUtils.getChildElementByTagName(element, ELEMENT_ID);
            if (deploymentIdElem != null) {
                String value = deploymentIdElem.getTextContent();
                if (!StringUtils.isBlank(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private String extractAttributeFromOptionalElement(Element parentElement, ParserContext parserContext, String elementName, String attributeName) {
        Element childElement = DomUtils.getChildElementByTagName(parentElement, elementName);
        return childElement != null ? childElement.getAttribute(attributeName) : StringUtils.EMPTY;
    }

    private String extractDescription(Element element) {
        String description = DomUtils.getChildElementValueByTagName(element, ELEMENT_DESCRIPTION);
        return description!=null?description:"";
    }

    private void registerProcessedImportsResettingBeanFactoryPostProcessor(BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(ProcessedImportsResettingBeanFactoryPostProcessor.class);
        registry.registerBeanDefinition("processedImportsResettingBeanFactoryPostProcessor",
                builder.getBeanDefinition());
    }

    private void registerDependencyGraphCalculatingBeanFactoryPostProcessor(BeanDefinitionRegistry registry) {
    	BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(DependencyGraphCalculatingBeanFactoryPostProcessor.class);
        registry.registerBeanDefinition("dependencyGraphCalculatingBeanFactoryPostProcessor",
                builder.getBeanDefinition());
    }

    private void registerProfileValidationBeanFactoryPostProcessor(BeanDefinitionRegistry registry) {
    	BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(ProfileValidationBeanFactoryPostProcessor.class);
        registry.registerBeanDefinition("profileValidationBeanFactoryPostProcessor",
                builder.getBeanDefinition());
    }

    private void registerAutoBindBeanFactoryPostProcessorIfRequired(Element profileElement, BeanDefinitionRegistry registry) {

        if (deploymentProfile(profileElement) && autoBindIsEnabled(profileElement)) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder
                    .genericBeanDefinition(AutoBindFactoryPostProcessor.class);
            registry.registerBeanDefinition("autoBindBeanFactoryPostProcessor",
                    builder.getBeanDefinition());
        }
    }

    private void registerCapabilityInheritancePostProcessor(BeanDefinitionRegistry registry) {
    	BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(CapabilityInheritanceBeanDefinitionPostProcessor.class);
        registry.registerBeanDefinition("capabilityInheritanceBeanDefinitionPostProcessor",
                builder.getBeanDefinition());
    }

    private void registerProfileFactoryBeanIfRequired(String profileId, String deploymentId, String profileType, String profileDescription,
          File origin, BeanDefinitionRegistry registry, String solutionPropertiesPath, String deploymentPropertiesPath, ParserContext parserContext) {
        if (!registry.containsBeanDefinition(THE_ONE_TRUE_PROFILE_BEAN_NAME)) {
            BeanDefinitionBuilder profileFactoryBuilder = BeanDefinitionBuilder.rootBeanDefinition(ProfileFactoryBean.class);
            profileFactoryBuilder.addConstructorArgValue(Id.createProfileId(profileId));
            profileFactoryBuilder.addConstructorArgValue(Id.createDeploymentId(deploymentId));
            profileFactoryBuilder.addConstructorArgValue(profileType);
            profileFactoryBuilder.addConstructorArgValue(profileDescription);
            profileFactoryBuilder.addConstructorArgValue(origin);
            profileFactoryBuilder.addPropertyValue(PROPERTY_SOLUTION_PROPERTIES_PATH, solutionPropertiesPath);
            profileFactoryBuilder.addPropertyValue(PROPERTY_DEPLOYMENT_PROPERTIES_PATH, deploymentPropertiesPath);
            
            final ResourceLoader resourceLoader = parserContext.getReaderContext().getResourceLoader();
            if (resourceLoader instanceof ProfileApplicationContext) {
                File solutionDir = ((ProfileApplicationContext) resourceLoader).getSolutionDir();
                profileFactoryBuilder.addPropertyValue(PROPERTY_SOLUTION_DIR, solutionDir);
            }
            
            registry.registerBeanDefinition(THE_ONE_TRUE_PROFILE_BEAN_NAME, profileFactoryBuilder.getBeanDefinition());
            
            RootBeanDefinition factoryBean = (RootBeanDefinition) registry.getBeanDefinition(THE_ONE_TRUE_PROFILE_BEAN_NAME);
            factoryBean.setAttribute("builder", profileFactoryBuilder);
        } else {
            RootBeanDefinition factoryBean = (RootBeanDefinition) registry.getBeanDefinition(THE_ONE_TRUE_PROFILE_BEAN_NAME);
            BeanDefinitionBuilder builder = (BeanDefinitionBuilder) factoryBean.getAttribute("builder");
            if (solutionPropertiesPath != null) {
                builder.addPropertyValue(PROPERTY_SOLUTION_PROPERTIES_PATH, solutionPropertiesPath);
            }
            if (deploymentPropertiesPath != null) {
                builder.addPropertyValue(PROPERTY_DEPLOYMENT_PROPERTIES_PATH, deploymentPropertiesPath);
            }
        }
    }
    
    private void parseImportsAndIfs(Element profileElement, ParserContext parserContext) {
        List<Element> importElements = DomUtils.getChildElementsByTagName(profileElement, ELEMENT_IMPORT);
        for (Element singleImportElement : importElements) {
            IMPORT_BEAN_DEFINITION_PARSER.parse(singleImportElement, parserContext);
        }
        
        List<Element> ifElements = DomUtils.getChildElementsByTagName(profileElement, ELEMENT_IF);
        for (Element ifElement : ifElements) {
            IF_BEAN_DEFINITION_PARSER.parse(ifElement, parserContext);
        }
    }

    private void parseAndRegisterCapabilityDefinitions(Element profileElement, File origin, BeanDefinitionRegistry registry, ParserContext parserContext) {
        List<Element> capabilityDefinitionElements = DomUtils.getChildElementsByTagName(profileElement, ELEMENT_CAPABILITY_DEF);
        for (Element capabilityDefElement : capabilityDefinitionElements) {
            parseAndRegisterSingleCapabilityDefinition(capabilityDefElement, origin, parserContext, registry);
        }
    }

    private void parseAndRegisterSingleCapabilityDefinition(
            Element capabilityDefElement, File origin, ParserContext parserContext, BeanDefinitionRegistry registry) {

        String capabilityDefId = extractMandatoryIdAttributeFromElement(capabilityDefElement, "CapabilityDefinition");

        String description = extractDescription(capabilityDefElement);

        String abstractString = capabilityDefElement.getAttribute(PROPERTY_UNIT_ABSTRACT);
        boolean _abstract = StringUtils.equals("true", abstractString)?true:false;

        List<CapabilityDefinitionReference> ancestors = parseInheritDefinitionElements(capabilityDefElement);
        ManagedList<AttributeKey> attributeKeysList = parseAttributeKeyElements(capabilityDefElement, parserContext);

        BeanDefinitionBuilder capabilityBeanDefBuilder = BeanDefinitionBuilder.rootBeanDefinition(CapabilityDefinition.class);
        capabilityBeanDefBuilder.addConstructorArgValue(capabilityDefId);
        capabilityBeanDefBuilder.addPropertyValue(PROPERTY_ORIGIN, origin);
        capabilityBeanDefBuilder.addPropertyValue(PROPERTY_DESCRIPTION, description);
        capabilityBeanDefBuilder.addPropertyValue(PROPERTY_CAPABILITY_DEF_ANCESTORS, ancestors);
        capabilityBeanDefBuilder.addPropertyValue(PROPERTY_CAPABILITY_DEF_ATTRIBUTE_KEYS, attributeKeysList);
        capabilityBeanDefBuilder.addPropertyValue(PROPERTY_UNIT_ABSTRACT, _abstract);

        registry.registerBeanDefinition(createCapabilityDefBeanName(capabilityDefId), capabilityBeanDefBuilder.getBeanDefinition());
    }

    private String extractMandatoryIdAttributeFromElement(Element element, String elementName) {
        String idAttribute = element.getAttribute(ID_ATTRIBUTE);
        if (StringUtils.isBlank(idAttribute)) {
            throw new BeanCreationException(String.format("%s is defined with a blank ID.", elementName));
        }
        return idAttribute;
    }

    private List<CapabilityDefinitionReference> parseInheritDefinitionElements(Element capabilityDefElement) {

        Set<CapabilityDefinitionReference> distinctAncestors = new HashSet<>();

        List<Element> inheritElements = DomUtils.getChildElementsByTagName(capabilityDefElement, ELEMENT_INHERIT_DEF);
        for (Element inheritElement : inheritElements) {
            String referencedCapabilityId = inheritElement.getAttribute(ATTRIBUTE_CAPABILITY_DEFINITION_REF);
            String prefix = inheritElement.getAttribute(ATTRIBUTE_PREFIX);

            CapabilityDefinitionReference capRef = new CapabilityDefinitionReference(referencedCapabilityId, prefix);
            distinctAncestors.add(capRef);
        }

        List<CapabilityDefinitionReference> ancestors = new ArrayList<>(distinctAncestors.size());
        ancestors.addAll(distinctAncestors);
        return ancestors;
    }
    
    private ManagedList<AttributeKey> parseAttributeKeyElements(Element capabilityDefElement, ParserContext parserContext) {

        Set<AttributeKey> attributeKeys = new HashSet<>();

        List<Element> attributeKeyElements = DomUtils.getChildElementsByTagName(capabilityDefElement, ELEMENT_ATTRIBUTE_KEY);
        for (Element attributeKey : attributeKeyElements) {
            String key = attributeKey.getAttribute(ATTRIBUTE_ATTRIBUTE_KEY_KEY);
            File origin = determineOrigin(key, parserContext);
            String description = extractDescription(attributeKey);
            String defaultValue = attributeKey.getAttribute(ATTRIBUTE_KEY_DEFAULT);
            if (StringUtils.isEmpty(defaultValue)) {
                defaultValue = null;
            }
            boolean optional = Boolean.valueOf(attributeKey.getAttribute(ATTRIBUTE_KEY_OPTIONAL));
            if (StringUtils.isNotBlank(key)) {
                attributeKeys.add(new AttributeKey(key, origin, description, optional, defaultValue));
            }
        }
        ManagedList<AttributeKey> attributeKeysList = new ManagedList<>(attributeKeys.size());
        attributeKeysList.addAll(attributeKeys);
        return attributeKeysList;
    }

    static String createCapabilityDefBeanName(String capabilityDefId) {
        return CAPABILITY_DEFINITION_NAME_PREFIX + capabilityDefId;
    }
    
    private void parseAndRegisterUnits(Element profileElement, File origin, BeanDefinitionRegistry registry, ParserContext parserContext) {

        List<Element> unitElements = DomUtils.getChildElementsByTagName(profileElement, ELEMENT_UNIT);
        for (Element unitElement : unitElements) {
            // first retrieve all XML attributes of the Unit element
            String unitId = extractMandatoryIdAttributeFromElement(unitElement, "Unit");
            @SuppressWarnings("unused") String unitType = unitElement.getAttribute(ATTRIBUTE_UNIT_TYPE);  // TODO: currently not used as there is nowhere to set this on a ConfigurationUnit 
            boolean unitIsAbstract = Boolean.valueOf(unitElement.getAttribute(ATTRIBUTE_UNIT_ABSTRACT));
            String unitExtends = unitElement.getAttribute(ATTRIBUTE_UNIT_EXTENDS);

            ManagedList<AbstractBeanDefinition> requiredCapabilityReferences = parseRequiredCapabilities(unitElement, unitId, registry);
            ManagedList<AbstractBeanDefinition> providedCapabilityReferences = parseProvidedCapabilities(unitElement, unitId, registry);
            ManagedList<Attribute> attributes = parseAttributesElement(unitElement, parserContext);
            ManagedList<AbstractBeanDefinition> mappings = parseMappingsElement(unitElement, unitId);
            ManagedList<AbstractBeanDefinition> commands = parseCommands(unitElement, unitId);
            ManagedList<AbstractBeanDefinition> asserts = parseAsserts(unitElement);

            BeanDefinitionBuilder unitBeanDefBuilder = null;
            if (StringUtils.isNotBlank(unitExtends)) {
                unitBeanDefBuilder = BeanDefinitionBuilder.childBeanDefinition(unitExtends);
                unitBeanDefBuilder.getRawBeanDefinition().setBeanClass(ConfigurationUnit.class);
                requiredCapabilityReferences.setMergeEnabled(true);
                providedCapabilityReferences.setMergeEnabled(true);
                attributes.setMergeEnabled(true);
                mappings.setMergeEnabled(true);
                commands.setMergeEnabled(true);
                asserts.setMergeEnabled(true);

                unitBeanDefBuilder.addPropertyValue(PROPERTY_UNIT_PARENT_ID, unitExtends);
            } else {
                unitBeanDefBuilder = BeanDefinitionBuilder.rootBeanDefinition(ConfigurationUnit.class);
            }

            String description = extractDescription(unitElement);

            unitBeanDefBuilder.setAbstract(false);
            unitBeanDefBuilder.addPropertyValue(PROPERTY_ORIGIN, origin);
            unitBeanDefBuilder.addPropertyValue(PROPERTY_DESCRIPTION, description);
            unitBeanDefBuilder.addPropertyValue(PROPERTY_UNIT_ABSTRACT, unitIsAbstract);
            unitBeanDefBuilder.addPropertyValue(PROPERTY_UNIT_PROVIDED_CAPABILITIES, providedCapabilityReferences);
            unitBeanDefBuilder.addPropertyValue(PROPERTY_UNIT_REQUIRED_CAPABILITIES, requiredCapabilityReferences);
            unitBeanDefBuilder.addPropertyValue(PROPERTY_UNIT_ATTRIBUTES, attributes);
            unitBeanDefBuilder.addPropertyValue(PROPERTY_UNIT_MAPPINGS, mappings);
            unitBeanDefBuilder.addPropertyValue(PROPERTY_UNIT_COMMANDS, commands);
            unitBeanDefBuilder.addPropertyValue(PROPERTY_UNIT_ASSERTS, asserts);

            unitBeanDefBuilder.addConstructorArgValue(unitId);
            RuntimeBeanReference unitBeanRef = new RuntimeBeanReference(unitId);
            unitBeanRef.setSource(parserContext.getReaderContext().extractSource(unitElement));

            registry.registerBeanDefinition(unitId, unitBeanDefBuilder.getBeanDefinition());
        }
        
    }

    private ManagedList<AbstractBeanDefinition> parseAsserts(Element parentElement) {
        final ManagedList<AbstractBeanDefinition> asserts = new ManagedList<>();
        final List<Element> assertsElements =
            DomUtils.getChildElementsByTagName(parentElement, ELEMENT_ASSERTS);

        for (Element assertsElement : assertsElements) {
            parseUniqueAsserts(asserts, assertsElement);
            parsePrerequisitesAsserts(asserts, assertsElement);
            parseIsTrueAsserts(asserts, assertsElement);
        }

        return asserts;
    }

    private void parsePrerequisitesAsserts(final ManagedList<AbstractBeanDefinition> asserts,
            Element assertsElement) {
        final List<Element> uniqueAsserts =
            DomUtils.getChildElementsByTagName(assertsElement, "prerequisite");

        for (Element uniqueAssert : uniqueAsserts) {
            final BeanDefinitionBuilder assertBeanDefBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(PrerequisiteAssert.class);
            assertBeanDefBuilder.addConstructorArgValue(uniqueAssert.getAttribute("key"));
            assertBeanDefBuilder.addConstructorArgValue(uniqueAssert.getAttribute("value"));
            asserts.add(assertBeanDefBuilder.getBeanDefinition());
        }
    }

    private void parseUniqueAsserts(final ManagedList<AbstractBeanDefinition> asserts,
            Element assertsElement) {
        final List<Element> uniqueAsserts =
                DomUtils.getChildElementsByTagName(assertsElement, "unique");
        
        for (Element uniqueAssert : uniqueAsserts) {
            final BeanDefinitionBuilder assertBeanDefBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(UniqueAssert.class);
            assertBeanDefBuilder.addConstructorArgValue(uniqueAssert.getAttribute("value"));
            assertBeanDefBuilder.addConstructorArgValue(uniqueAssert.getAttribute("enabled"));
            assertBeanDefBuilder.addConstructorArgValue(uniqueAssert.getAttribute("message"));
            asserts.add(assertBeanDefBuilder.getBeanDefinition());
        }
    }

    private void parseIsTrueAsserts(final ManagedList<AbstractBeanDefinition> asserts,
            Element assertsElement) {
        final List<Element> uniqueAsserts =
                DomUtils.getChildElementsByTagName(assertsElement, "is-true");
        
        for (Element uniqueAssert : uniqueAsserts) {
            final BeanDefinitionBuilder assertBeanDefBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(IsTrueAssert.class);
            assertBeanDefBuilder.addConstructorArgValue(uniqueAssert.getAttribute("value"));
            assertBeanDefBuilder.addConstructorArgValue(uniqueAssert.getAttribute("message"));
            asserts.add(assertBeanDefBuilder.getBeanDefinition());
        }
    }

    private ManagedList<AbstractBeanDefinition> parseCommands(Element unitElement, String unitId) {
        ManagedList<AbstractBeanDefinition> commands = new ManagedList<>();
        List<Element> commandElements = DomUtils.getChildElementsByTagName(unitElement, ELEMENT_UNIT_COMMAND);

        if (!commandElements.isEmpty()) {

            String defaultPackageRef = unitElement.getAttribute(DccConstants.PACKAGE);
            Id<PackageId> defaultPackageId = null;
            if (StringUtils.isNotEmpty(defaultPackageRef)) {
                defaultPackageId = Id.createPackageId(defaultPackageRef);
            }

            for (Element commandElement : commandElements) {
                final String commandTypeRef = commandElement.getAttribute(ATTRIBUTE_UNIT_TYPE);
                final Commands commandType = Commands.parseConfigurableCommand(commandTypeRef);
                if (commandType == null) {
                    throw new BeanCreationException(String.format("Unsupported command type [%s].",
                            commandTypeRef));
                }

                final Id<PackageId> packageId = parsePackageReferenceForCommand(defaultPackageId, commandElement, unitId);

                final BeanDefinitionBuilder commandBeanDefBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(CommandDefinition.class);
                commandBeanDefBuilder.addConstructorArgValue(commandType);
                commandBeanDefBuilder.addConstructorArgValue(packageId);

                final List<CapabilityDefinitionReference> capabilities =
                    parseCommandCapabilities(commandElement);
                final List<CapabilityDefinitionExtensionReference> contributions =
                    parseCommandContributions(commandElement);
                final List<CapabilityDefinitionExtensionReference> requisitions =
                    parseCommandRequisitions(commandElement);
                final List<Provision> provisions = parseCommandProvisions(commandElement);

                commandBeanDefBuilder.addPropertyValue(PROPERTY_CAPABILITIES, capabilities);
                commandBeanDefBuilder.addPropertyValue(PROPERTY_CONTRIBUTIONS, contributions);
                commandBeanDefBuilder.addPropertyValue(PROPERTY_REQUISITIONS, requisitions);
                commandBeanDefBuilder.addPropertyValue(PROPERTY_PROVISIONS, provisions);

                commands.add(commandBeanDefBuilder.getBeanDefinition());
            }
        }

        return commands;
    }

    private Id<PackageId> parsePackageReferenceForCommand(Id<PackageId> defaultPackageId, Element commandElement, String unitId) {
        final Element packageElement = DomUtils.getChildElementByTagName(commandElement, DccConstants.PACKAGE);
        final String textContent = packageElement != null ? packageElement.getTextContent() : null;
        final String attributeContent = commandElement.getAttribute("package");
        final boolean isTextContentEmpty = StringUtils.isEmpty(textContent);
        final boolean isAttributeEmpty = StringUtils.isEmpty(attributeContent);

        if (!isTextContentEmpty && !isAttributeEmpty) {
            throw new BeanCreationException(
                    String.format(
                            "Command should either specify sub element package [%s] or attribute package [%s].",
                            textContent, attributeContent));
        }

        Id<PackageId> packageId = defaultPackageId;
        if (!isAttributeEmpty) {
            packageId = Id.createPackageId(attributeContent);
        } else if (!isTextContentEmpty) {
            packageId = Id.createPackageId(textContent);
        }

        if (packageId == null) {
            throw new BeanCreationException(
                    String.format("No package reference defined for command [%s] of unit [%s]",
                            commandElement.getAttribute("type"), unitId));
        }

        return packageId;
    }

    private List<CapabilityDefinitionReference> parseCommandCapabilities(Element commandElement) {
        final List<CapabilityDefinitionReference> parsedReferences = new ArrayList<>();

        final List<Element> matchedElements =
            DomUtils.getChildElementsByTagName(commandElement, DccConstants.CAPABILITY);
        for (Element element : matchedElements) {
            final CapabilityDefinitionReference ref =
                parseCapabilityDefinitionReferenceFromElement(new CapabilityDefinitionReference(),
                        element, "id", DccConstants.CAPABILITY);
            if (ref != null) {
                parsedReferences.add(ref);
            }
        }

        return parsedReferences;
    }

    private <T extends CapabilityDefinitionReference> T parseCapabilityDefinitionReferenceFromElement(
            T template,
            Element element, String idColumnName, String elementName) {
        final String textContent = element.getTextContent();
        final String id = element.getAttribute(idColumnName);
        final boolean isTextContentEmpty = StringUtils.isEmpty(textContent);
        final boolean isIdAttributeEmpty = StringUtils.isEmpty(id);

        if (!isTextContentEmpty && !isIdAttributeEmpty) {
            throw new BeanCreationException(String.format(
                    "%s should either specify text content [%s] or attribute %s [%s].",
                    elementName, textContent, idColumnName, id));
        } else if (isTextContentEmpty && isIdAttributeEmpty) {
            return null;
        }

        final String capabilityRef = isIdAttributeEmpty ? textContent : id;

        final String prefixText = element.getAttribute("prefix");
        final String prefix = StringUtils.isEmpty(prefixText) ? null : prefixText;

        template.setPrefix(prefix);
        template.setReferencedCapabilityDefId(capabilityRef);

        return template;
    }

    private List<CapabilityDefinitionExtensionReference> parseCommandContributions(
            Element commandElement) {
        final List<CapabilityDefinitionExtensionReference> parsedReferences = new ArrayList<>();

        final List<Element> matchedElements =
            DomUtils.getChildElementsByTagName(commandElement, DccConstants.CONTRIBUTION);
        for (Element element : matchedElements) {
            final CapabilityDefinitionExtensionReference ref =
                parseCapabilityDefinitionExtensionReferences(element, DccConstants.CONTRIBUTION);
            if (ref != null) {
                parsedReferences.add(ref);
            }
        }

        return parsedReferences;
    }

    private List<Provision> parseCommandProvisions(
            Element commandElement) {
        final List<Provision> parsedReferences = new ArrayList<>();

        final List<Element> matchedElements =
            DomUtils.getChildElementsByTagName(commandElement, DccConstants.PROVISION);
        for (Element element : matchedElements) {
            final CapabilityDefinitionExtensionReference ref =
                parseCapabilityDefinitionReferenceFromElement(
                        new CapabilityDefinitionExtensionReference(), 
                        element, 
                        "capabilityId",
                        DccConstants.PROVISION);

            final List<ProvisionRestriction> restrictions = parseProvisionRestrictions(element);
            
            if (ref != null) {
                parsedReferences.add(new Provision(ref, restrictions));
            }
        }

        return parsedReferences;
    }

    private List<ProvisionRestriction> parseProvisionRestrictions(Element element) {
        final List<ProvisionRestriction> restrictions = new ArrayList<>();
        final Element restrictionsElement =
            DomUtils.getChildElementByTagName(element, "restrictions");
        if (restrictionsElement != null) {
            final Element hostBoundRestriction =
                DomUtils.getChildElementByTagName(restrictionsElement, "host-bound");
            if (hostBoundRestriction != null) {
                restrictions.add(new HostRestriction());
            }
            //TODO add additional restrictions
        }

        return restrictions;
    }

    private List<CapabilityDefinitionExtensionReference> parseCommandRequisitions(
            Element commandElement) {
        final List<CapabilityDefinitionExtensionReference> parsedReferences = new ArrayList<>();
        
        final List<Element> matchedElements =
                DomUtils.getChildElementsByTagName(commandElement, DccConstants.REQUISITION);
        for (Element element : matchedElements) {
            final CapabilityDefinitionExtensionReference ref =
                    parseCapabilityDefinitionExtensionReferences(element, DccConstants.REQUISITION);
            if (ref != null) {
                if (StringUtils.isEmpty(ref.getBoundToCapabilityId())) {
                    // Only valid for requisitions
                    ref.setBoundToCapabilityId(ref.getReferencedCapabilityDefId());
                }
                
                parsedReferences.add(ref);
            }
        }
        
        return parsedReferences;
    }

    private CapabilityDefinitionExtensionReference parseCapabilityDefinitionExtensionReferences(
            Element element, String elementName) {
        final CapabilityDefinitionExtensionReference ref =
            parseCapabilityDefinitionReferenceFromElement(
                    new CapabilityDefinitionExtensionReference(), element, "capabilityId",
                    elementName);

        if (ref == null) {
            return null;
        }

        final String boundToCapabilityRefText = element.getAttribute("boundTo");
        final String boundToCapabilityRef =
            StringUtils.isEmpty(boundToCapabilityRefText) ? null : boundToCapabilityRefText;

        ref.setBoundToCapabilityId(boundToCapabilityRef);

        return ref;
    }

    private ManagedList<AbstractBeanDefinition> parseMappingsElement(Element unitElement, String unitId) {
        ManagedList<AbstractBeanDefinition> mappings = new ManagedList<>();
        Element mappingsElement = DomUtils.getChildElementByTagName(unitElement, ELEMENT_UNIT_MAPPINGS);
        if (mappingsElement != null) {
            List<Element> mappingElements = DomUtils.getChildElementsByTagName(mappingsElement, "mapping");
            for (Element mappingElement : mappingElements) {
                String targetCapabilityId = mappingElement.getAttribute("targetCapabilityId");

                BeanDefinitionBuilder mappingBeanDefBuilder = BeanDefinitionBuilder.genericBeanDefinition(Mapping.class);
                mappingBeanDefBuilder.addConstructorArgValue(Id.createCapabilityId(targetCapabilityId));

                ManagedList<AttributeMapper> mappers = new ManagedList<>();

                Element mapUnitAttributesElement = DomUtils.getChildElementByTagName(mappingElement, "map-unit-attributes");
                if (mapUnitAttributesElement != null) {
                    String sourcePrefix = mapUnitAttributesElement.getAttribute("sourcePrefix");
                    String targetPrefix = mapUnitAttributesElement.getAttribute("targetPrefix");
                    UnitToCapabilityAttributeMapper mapper = new UnitToCapabilityAttributeMapper(sourcePrefix, targetPrefix);
                    mappers.add(mapper);
                }

                // extract map alls
                List<Element> mapAllElements = DomUtils.getChildElementsByTagName(mappingElement, "map-all");
                for (Element mapAllElement : mapAllElements) {
                    Id<CapabilityId> sourceCapabilityId =
                        Id.createCapabilityId(mapAllElement.getAttribute("sourceCapabilityId"));
                    String sourcePrefix = mapAllElement.getAttribute("sourcePrefix");
                    String targetPrefix = mapAllElement.getAttribute("targetPrefix");
                    SourceToTargetCapabilityAttributeMapper mapper =
                        new SourceToTargetCapabilityAttributeMapper(sourceCapabilityId, sourcePrefix, targetPrefix);
                    mappers.add(mapper);
                }
                
                // extract expressions
                List<Element> expressionElements = DomUtils.getChildElementsByTagName(mappingElement, "expression");
                for (Element expressionElement : expressionElements) {
                    String attributeKey = expressionElement.getAttribute("attributeKey");
                    String value = parseValue(expressionElement);
                    ExpressionAttributeMapper mapper = new ExpressionAttributeMapper(attributeKey, value);
                    mappers.add(mapper);
                }

                mappingBeanDefBuilder.addPropertyValue("attributeMappers", mappers);
                
                mappingBeanDefBuilder.setScope(BeanDefinition.SCOPE_PROTOTYPE);

                mappings.add(mappingBeanDefBuilder.getBeanDefinition().cloneBeanDefinition());
            }
        }
        return mappings;
    }

    private ManagedList<Attribute> parseAttributesElement(Element unitElement, ParserContext parserContext) {
        ManagedList<Attribute> attributes = new ManagedList<>();
        Element attributesElement = DomUtils.getChildElementByTagName(unitElement, ELEMENT_UNIT_ATTRIBUTES);
        if (attributesElement != null) {
            List<Element> attributeElements = DomUtils.getChildElementsByTagName(attributesElement, ELEMENT_UNIT_ATTRIBUTES_ATTRIBUTE);
            for (Element attributeElement : attributeElements) {
                String key = attributeElement.getAttribute("key");
                String description = attributeElement.getAttribute("description");
                String typeString = attributeElement.getAttribute("type");
                AttributeType type = null;
                if (!StringUtils.isEmpty(typeString)) {
                    type = AttributeType.valueOf(typeString.toUpperCase(Locale.ENGLISH));
                }
                String value = parseValue(attributeElement);
                File origin = determineOrigin(key, parserContext);

                Attribute att = new Attribute(key, value, origin);
                att.setType(type);
                att.setDescription(description);
                attributes.add(att);
            }
        }
        return attributes;
    }

    private String parseString(Element element, String name, String defaultValue) {
        String result = defaultValue;
        if (element.hasAttribute(name)) {
            result = element.getAttribute(name);
            
            // if the value does not exist of blanks (intentionally)
            if (!StringUtils.isBlank(result)) {
                // we trim...
                result = result.trim();
            }
        }
        
        return result;
    }
    
    private String parseValue(Element elementWithValueAttribute) {
        String result = parseString(elementWithValueAttribute, "value", null);

        if (result != null) {
            // in any case we remove multiple spaces
            result = result.replaceAll("  *", " "); // FIXME: value could also be password !
        }
        return result;
    }

    private ManagedList<AbstractBeanDefinition> parseRequiredCapabilities(Element unitElement, String unitId, BeanDefinitionRegistry registry) {

        ManagedList<AbstractBeanDefinition> reqCapReferences = new ManagedList<>();
        List<Element> reqCapElements = DomUtils.getChildElementsByTagName(unitElement, ELEMENT_UNIT_REQUIRED_CAPABILITY);

        for (Element reqCapElement : reqCapElements) {
            String capabilityId = extractMandatoryIdAttributeFromElement(reqCapElement, "Capability");
            boolean optionalFlag = Boolean.valueOf(reqCapElement.getAttribute(ATTRIBUTE_KEY_OPTIONAL));
            boolean multipleBindingsAllowedFlag = Boolean.valueOf(reqCapElement.getAttribute(ATTRIBUTE_KEY_MULTIPLE_BINDINGS));
            boolean identifiesHost = Boolean.valueOf(reqCapElement.getAttribute(ATTRIBUTE_KEY_IDENTIFIES_HOST));

            BeanDefinitionBuilder capabilityBeanDefBuilder = createGenericCapabilityBuilder(capabilityId, reqCapElement, RequiredCapability.class);
            capabilityBeanDefBuilder.addPropertyValue(ATTRIBUTE_KEY_OPTIONAL, optionalFlag);
            capabilityBeanDefBuilder.addPropertyValue(ATTRIBUTE_KEY_MULTIPLE_BINDINGS, multipleBindingsAllowedFlag);
            capabilityBeanDefBuilder.addPropertyValue(ATTRIBUTE_IDENTIFIES_HOST, identifiesHost);

            String capabilityBeanName = generateCapabilityBeanName(unitId, capabilityId);
            registry.registerBeanDefinition(capabilityBeanName, capabilityBeanDefBuilder.getBeanDefinition());

            reqCapReferences.add(capabilityBeanDefBuilder.getBeanDefinition());
        }
        return reqCapReferences;

    }

    private ManagedList<AbstractBeanDefinition> parseProvidedCapabilities(Element unitElement, String unitId, BeanDefinitionRegistry registry) {

        ManagedList<AbstractBeanDefinition> provCapReferences = new ManagedList<>();
        List<Element> provCapElements = DomUtils.getChildElementsByTagName(unitElement, ELEMENT_UNIT_PROVIDED_CAPABILITY);
        
        for (Element provCapElement : provCapElements) {
            String capabilityId = extractMandatoryIdAttributeFromElement(provCapElement, "Capability");
            BeanDefinitionBuilder capabilityBeanDefBuilder = createGenericCapabilityBuilder(capabilityId, provCapElement, Capability.class);
            String capabilityBeanName = generateCapabilityBeanName(unitId, capabilityId);
            registry.registerBeanDefinition(capabilityBeanName, capabilityBeanDefBuilder.getBeanDefinition());
            provCapReferences.add(capabilityBeanDefBuilder.getBeanDefinition());
        }
        return provCapReferences;
    }

    private BeanDefinitionBuilder createGenericCapabilityBuilder(String capabilityId, Element capElement, Class<? extends Capability> type) {

        String capabilityDefinitionRef = capElement.getAttribute(ATTRIBUTE_CAPABILITY_DEFINITION_REF);
        
        // convention: if the capabilityDefinitionRef matches the capabilityId it can be omitted
        if (StringUtils.isEmpty(capabilityDefinitionRef)) {
            capabilityDefinitionRef = capabilityId;
        }
        String capabilityDefRef = createCapabilityDefBeanName(capabilityDefinitionRef);

        BeanDefinitionBuilder capabilityBeanDefBuilder = BeanDefinitionBuilder.genericBeanDefinition(type);
        capabilityBeanDefBuilder.addConstructorArgValue(Id.createCapabilityId(capabilityId));
        capabilityBeanDefBuilder.addConstructorArgReference(capabilityDefRef);
        capabilityBeanDefBuilder.addConstructorArgValue(null);

        // force spring to create several instances to not use shared capabilities across different units
        capabilityBeanDefBuilder.setScope(BeanDefinition.SCOPE_PROTOTYPE);

        return capabilityBeanDefBuilder;
    }

    private String generateCapabilityBeanName(String unitId, String capabilityId) {
        return String.format("%s#%s", unitId, capabilityId);
    }

    private void parseAndRegisterBindings(Element profileElement, File origin, BeanDefinitionRegistry registry,
            ParserContext parserContext) {
        
        List<Element> bindingElements = DomUtils.getChildElementsByTagName(profileElement, ELEMENT_BINDING);
        for (Element bindingElement : bindingElements) {
            Element sourceElement = DomUtils.getChildElementByTagName(bindingElement, ELEMENT_BINDING_SOURCE);
            Element targetElement = DomUtils.getChildElementByTagName(bindingElement, ELEMENT_BINDING_TARGET);

            String sourceUnitRef = sourceElement.getAttribute("unitRef");
            String sourceCapabilityId = sourceElement.getAttribute("capabilityId");
            String targetUnitRef = targetElement.getAttribute("unitRef");
            String targetCapabilityId = targetElement.getAttribute("capabilityId");
            
            // convention: if target capabilityId is omitted then use the source capability id
            if (StringUtils.isEmpty(targetCapabilityId)) {
                targetCapabilityId = sourceCapabilityId;
            }

            String description = extractDescription(bindingElement);

            BeanDefinitionBuilder bindingBeanBuilder = BeanDefinitionBuilder.genericBeanDefinition(BindingFactoryBean.class);
            bindingBeanBuilder.addPropertyValue(PROPERTY_ORIGIN, origin);
            bindingBeanBuilder.addPropertyValue(PROPERTY_DESCRIPTION, description);
            bindingBeanBuilder.addPropertyValue("sourceCapabilityId", Id.createCapabilityId(sourceCapabilityId));
            bindingBeanBuilder.addPropertyReference("sourceUnit", sourceUnitRef);
            bindingBeanBuilder.addPropertyValue("targetCapabilityId", Id.createCapabilityId(targetCapabilityId));
            bindingBeanBuilder.addPropertyReference("targetUnit", targetUnitRef);
            
            String bindingBeanName = parserContext.getReaderContext().generateBeanName(bindingBeanBuilder.getBeanDefinition());
            registry.registerBeanDefinition(bindingBeanName, bindingBeanBuilder.getBeanDefinition());
            
            RuntimeBeanReference beanRef = new RuntimeBeanReference(bindingBeanName);
            beanRef.setSource(parserContext.extractSource(bindingElement));

        }
    }

    private File determineOrigin(String elementId, ParserContext parserContext) {
        try {
            return parserContext.getReaderContext().getResource().getFile();
        } catch (IOException e) {
            throw new BeanCreationException(String.format("Cannot determine origin of [%s].",
                    elementId), e);
        }
    }
}
