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

import java.io.File;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import org.metaeffekt.dcc.commons.domain.Id;

public class ConfigurationMappingCasesTest {

    private static final File ORIGIN = new File("ConfigurationMappingCasesTest.class");

    private static final String DESCRIPTION = "description";

    @Test
    public void testMulitpleRequiredCapabilitiesOfSameDefinition() throws Exception {

        CapabilityDefinition def = new CapabilityDefinition("a").
                add(new AttributeKey("key01", ORIGIN, DESCRIPTION, false)).
                add(new AttributeKey("key02", ORIGIN, DESCRIPTION, false));

        ConfigurationUnit unit = new ConfigurationUnit("test-unit");

        RequiredCapability capabilityR1 = new RequiredCapability(Id.createCapabilityId("a1"), def, unit);
        RequiredCapability capabilityR2 = new RequiredCapability(Id.createCapabilityId("a2"), def, unit);
        RequiredCapability capabilityP = new RequiredCapability(Id.createCapabilityId("ap"), def, unit);

        unit.getRequiredCapabilities().add(capabilityR1);
        unit.getRequiredCapabilities().add(capabilityR2);
        unit.getProvidedCapabilities().add(capabilityP);

        Mapping mapping = new Mapping(capabilityP.getId());
        mapping.add(new ExpressionAttributeMapper("key01", "${a1[key01]}"));
        mapping.add(new ExpressionAttributeMapper("key02", "${a2[key02]}"));
        unit.add(mapping);
        unit.afterPropertiesSet();

        ConfigurationUnit sourceUnit1 = new ConfigurationUnit("source-unit-01");
        sourceUnit1.add(new Attribute("key01", "value_01-01", ORIGIN));
        sourceUnit1.add(new Attribute("key02", "value_01-02", ORIGIN));
        Capability capabilitySource1 = new Capability(Id.createCapabilityId("a"), def, sourceUnit1);
        sourceUnit1.getProvidedCapabilities().add(capabilitySource1);
        sourceUnit1.afterPropertiesSet();

        ConfigurationUnit sourceUnit2 = new ConfigurationUnit("source-unit-02");
        sourceUnit2.add(new Attribute("key01", "value_02-01", ORIGIN));
        sourceUnit2.add(new Attribute("key02", "value_02-02", ORIGIN));
        Capability capabilitySource2 = new Capability(Id.createCapabilityId("a"), def, sourceUnit2);
        sourceUnit2.getProvidedCapabilities().add(capabilitySource2);
        sourceUnit2.afterPropertiesSet();

        Profile p = new Profile();
        p.add(unit);
        p.add(sourceUnit1);
        p.add(sourceUnit2);

        p.add(new Binding(capabilitySource1, capabilityR1));
        p.add(new Binding(capabilitySource2, capabilityR2));
        p.setDeploymentProperties(new Properties(), null);
        p.setSolutionProperties(new Properties(), null);

        PropertiesHolder propertiesHolder = p.createPropertiesHolder(false);
        p.evaluate(propertiesHolder);

        Properties properties = propertiesHolder.getProperties(capabilityP);

        // check cross-mapping key01 for source 01 and key2 from source 02
        Assert.assertEquals("value_01-01", properties.getProperty("key01"));
        Assert.assertEquals("value_02-02", properties.getProperty("key02"));
    }

}
