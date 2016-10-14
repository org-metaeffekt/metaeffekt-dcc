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
package org.metaeffekt.dcc.commons;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.mapping.Attribute;
import org.metaeffekt.dcc.commons.mapping.Attribute.AttributeType;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.spring.xml.ProfileParser;

public class ProfileTest {

    final File baseFolder = new File("src/test/resources/parser-test/profiles");

    @Test
    public void test() throws IOException {
        final File testProfileFile = new File(baseFolder, "test-deployment-profile.xml");
        Profile profile = ProfileParser.parse(testProfileFile);
        PropertiesHolder propertiesHolder = profile.createPropertiesHolder(true);
        profile.evaluate(propertiesHolder);
        propertiesHolder.dump();

        Assert.assertEquals("test", profile.getDeploymentId().getValue());
        Assert.assertEquals(2, profile.getUnits().size());
        
        Assert.assertEquals("0", propertiesHolder.getProperty("unit-with-capability/test.capability", "key0"));
        Assert.assertEquals("overwrite1", propertiesHolder.getProperty("unit-with-capability/test.capability", "key1"));
        Assert.assertEquals("overwrite2", propertiesHolder.getProperty("unit-with-capability/test.capability", "key2"));
        Assert.assertEquals("default3", propertiesHolder.getProperty("unit-with-capability/test.capability", "key3"));
        Assert.assertEquals("overwrite4", propertiesHolder.getProperty("unit-with-capability/test.capability", "key4"));
        Assert.assertEquals("attribute5", propertiesHolder.getProperty("unit-with-capability/test.capability", "key5"));
    }

    @Test
    public void testCapabilityInheritance() throws IOException {
        Profile profile;
        try (ConfigurableApplicationContext context =
            new ClassPathXmlApplicationContext(
                    "classpath:/parser-test/profiles/inherit-capability-profile.xml")) {
            profile = context.getBean(Profile.class);
        }

        PropertiesHolder propertiesHolder = profile.createPropertiesHolder(true);
        profile.evaluate(propertiesHolder);
        propertiesHolder.dump();

        Assert.assertEquals("inherit-capability-profile", profile.getDeploymentId().getValue());
    }

    @Test
    public void testAttributeExpression() throws IOException {
        Profile profile;
        try (ConfigurableApplicationContext context =
            new ClassPathXmlApplicationContext(
                    "classpath:/parser-test/profiles/attribute-expressions-profile.xml")) {
            profile = context.getBean(Profile.class);
        }

        PropertiesHolder propertiesHolder = profile.createPropertiesHolder(true);
        profile.evaluate(propertiesHolder);
        propertiesHolder.dump();
        Assert.assertEquals("123", propertiesHolder.getProperty("unit2", "expr"));
        Id<UnitId> id = Id.createUnitId("unit2");
        for (ConfigurationUnit unit : profile.getUnits()) {
            if (id.equals(unit.getId())) {
                for (Attribute attribute : unit.getAttributes()) { 
                    Assert.assertEquals(AttributeType.BASIC, attribute.getType());
                    Assert.assertTrue(StringUtils.isNotEmpty(attribute.getDescription()));
                }
            }
        }

        Assert.assertEquals("attribute-expressions-profile", profile.getDeploymentId().getValue());
    }

    @Test(expected = BeanDefinitionValidationException.class)
    public void testFailedAsserts() throws IOException {
        try (ConfigurableApplicationContext context =
            new ClassPathXmlApplicationContext(
                    "classpath:/parser-test/invalid/dcc-asserts.xml")) {
            context.getBean(Profile.class);
        }
    }

}
