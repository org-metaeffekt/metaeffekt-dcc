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

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.metaeffekt.dcc.commons.DccProperties;
import org.metaeffekt.dcc.commons.mapping.AttributeKey;
import org.metaeffekt.dcc.commons.mapping.CapabilityDefinition;
import org.metaeffekt.dcc.commons.mapping.Profile;

/**
 * Test imports different test profiles and ensures that the parser features are working as intended.
 * @author Jochen K.
 */
public class DCCConfigurationBeanDefinitionParserTest {

    private static final String PROFILE_BASE_DIR = "classpath:parser-test";
    private static final String INVALID_PROFILE_BASE_DIR = "classpath:parser-test/invalid";

    private static final Logger LOG = LoggerFactory.getLogger(DCCConfigurationBeanDefinitionParserTest.class);

    private Map<String, String[]> expectationsByDefinitionId;

    @Before
    public void init() {
        expectationsByDefinitionId = new HashMap<>();
        expectationsByDefinitionId.put("Grandparent", new String[]{"name"});
        expectationsByDefinitionId.put("Parent", new String[]{"name", "grandpa.name"});
        expectationsByDefinitionId.put("Child", new String[]{"alias", "name", "grandpa.name", "pops.name"});
        expectationsByDefinitionId.put("FamilyMember", new String[]{"name"});
        expectationsByDefinitionId.put("Family", new String[]{"mom.name", "dad.name", "gran.name", "name"});
    }

    @Test
    public void testValidProfileCapabilityReferences() throws Exception {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                PROFILE_BASE_DIR + File.separator + "dcc-capability-ref.xml")) {

            Profile profile = context.getBean(Profile.class);
            for (CapabilityDefinition definition : profile.getCapabilityDefinitions()) {
                assertDefinition(definition);
            }

        }
    }


    @Test(expected = BeanDefinitionValidationException.class)
    public void testInvalidDuplicateAttributeName() throws Exception {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                INVALID_PROFILE_BASE_DIR + File.separator + "dcc-capability-ref-duplicate-attribute-name.xml")) {
        } catch (BeanDefinitionValidationException up) {
            LOG.debug("Exception occurred as expected !", up);
            throw up;
        }
    }

    @Test
    public void testInvalidFileDisableValidation() throws Exception {
        String orginalValue = System.getProperty(DccProperties.DCC_SYSTEM_PROPERTY_PROFILE_VALIDATION);
        System.setProperty(DccProperties.DCC_SYSTEM_PROPERTY_PROFILE_VALIDATION, "false");
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                INVALID_PROFILE_BASE_DIR + File.separator + "dcc-capability-missing-attribute-def.xml")) {
        } finally {
            System.setProperty(DccProperties.DCC_SYSTEM_PROPERTY_PROFILE_VALIDATION, orginalValue);
        }
    }

    @Test
    public void testValidFileReferencesDCCPredefinedProperty() throws Exception {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                INVALID_PROFILE_BASE_DIR + File.separator + "dcc-capability-ref-predefined-attribute-name.xml")) {
        }
    }

    @Test(expected = BeanDefinitionValidationException.class)
    public void testInvalidCyclicInheritance() throws Exception {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                INVALID_PROFILE_BASE_DIR + File.separator + "dcc-capability-ref-cyclic-inheritance.xml")) {
        } catch (BeanDefinitionValidationException up) {
            LOG.debug("Exception occurred as expected !", up);
            throw up;
        }
    }

    private void assertDefinition(CapabilityDefinition definition) {
        List<AttributeKey> attributeKeys = definition.getAttributeKeys();
        String[] keys = expectationsByDefinitionId.get(definition.getId());

        Assert.assertNotNull("No expectation defined for [" + definition.getId() + "]", keys);

        Assert.assertEquals("Received unexpected number of attribute keys.", keys.length, attributeKeys.size());
        for (String key : keys) {
            List<String> extractedKeys = extractKeys(attributeKeys);
            Assert.assertTrue("Expected to find key [" + key + "] in keys [" + extractedKeys + "]",
                    extractedKeys.contains(key));
        }
    }

    private List<String> extractKeys(List<AttributeKey> attributeKeys) {
        List<String> keys = new LinkedList<>();
        for (AttributeKey attributeKey : attributeKeys) {
            keys.add(attributeKey.getKey());
        }
        return keys;
    }

}
