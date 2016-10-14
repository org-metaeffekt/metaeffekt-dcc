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
package org.metaeffekt.dcc.commons.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.dependency.UnitDependencies;
import org.metaeffekt.dcc.commons.dependency.UnitDependencyGraphCalculator;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.spring.xml.BindingFactoryBean;

public class TestProfiles {

    
    public static final Profile QUITECOMLEXPROFILE;
    public static final Profile EMPTY_PROFILE;
    public static final ConfigurationUnit CONFIGURATION_UNIT_01;
    public static final ConfigurationUnit CONFIGURATION_UNIT_02;
    public static final ConfigurationUnit CONFIGURATION_UNIT_03;
    public static final ConfigurationUnit HOST;
    public static final Capability PROVIDED_HOST_CAPABILITY;
    public static final Binding BINDING_01;
    public static final Binding BINDING_02;
    public static final Binding HOST_BINDING;
    public static final String PROPERTY_KEY = "foo";
    public static final String PROPERTY_VALUE = "bar";

    
    static {

        CONFIGURATION_UNIT_01 = new ConfigurationUnit("u1");
        CONFIGURATION_UNIT_02 = new ConfigurationUnit("u2");
        CONFIGURATION_UNIT_03 = new ConfigurationUnit("u3");
        HOST = new ConfigurationUnit("host");

        PROVIDED_HOST_CAPABILITY = createHostCapability(HOST);
        List<Capability> providedHostCapabilities = new ArrayList<>();
        providedHostCapabilities.add(PROVIDED_HOST_CAPABILITY);
        HOST.setProvidedCapabilities(providedHostCapabilities);

        Capability requiredHostCapability = createHostCapability(CONFIGURATION_UNIT_03);
        List<Capability> requiredCapabilities = new ArrayList<>();
        requiredCapabilities.add(requiredHostCapability);
        CONFIGURATION_UNIT_03.setProvidedCapabilities(providedHostCapabilities);
        final CommandDefinition commandDefinition = new CommandDefinition(Commands.START, Id.createPackageId("123"));
        commandDefinition.setCapabilities(Arrays.asList(new CapabilityDefinitionReference("hostCapability")));
        CONFIGURATION_UNIT_03.setCommands(Arrays.asList(commandDefinition));


        BINDING_01 = new Binding(new Capability(Id.createCapabilityId("c1"), new CapabilityDefinition("cap1"), CONFIGURATION_UNIT_02), 
                new Capability(Id.createCapabilityId("c1"), new CapabilityDefinition("cap1"), CONFIGURATION_UNIT_01));
        BINDING_02 = new Binding(new Capability(Id.createCapabilityId("c1"), new CapabilityDefinition("cap1"), CONFIGURATION_UNIT_03), 
                new Capability(Id.createCapabilityId("c1"), new CapabilityDefinition("cap1"), CONFIGURATION_UNIT_02));
        HOST_BINDING = new Binding(PROVIDED_HOST_CAPABILITY, requiredHostCapability);

        
        QUITECOMLEXPROFILE = new Profile();
        QUITECOMLEXPROFILE.setDeploymentId(Id.createDeploymentId("quite-complex-profile"));
        QUITECOMLEXPROFILE.add(CONFIGURATION_UNIT_01);
        QUITECOMLEXPROFILE.add(CONFIGURATION_UNIT_02);
        QUITECOMLEXPROFILE.add(CONFIGURATION_UNIT_03);
        QUITECOMLEXPROFILE.add(HOST);

        QUITECOMLEXPROFILE.add(BINDING_01);
        QUITECOMLEXPROFILE.add(BINDING_02);
        QUITECOMLEXPROFILE.add(HOST_BINDING);

        QUITECOMLEXPROFILE.setDeploymentProperties(new Properties(), null);
        QUITECOMLEXPROFILE.setSolutionProperties(new Properties(), null);

        initializeDependencies(QUITECOMLEXPROFILE);

        EMPTY_PROFILE = new Profile();
    }

    private static Capability createHostCapability(ConfigurationUnit unit) {
        CapabilityDefinition hostCapabilityDefinition = new CapabilityDefinition(DccConstants.HOST_CAPABILITY);
        List<AttributeKey> attributeKeys = new ArrayList<>();
        attributeKeys.add(new AttributeKey("name"));
        hostCapabilityDefinition.setAttributeKeys(attributeKeys);
        Capability capability = new Capability(Id.createCapabilityId("hostCapability"), hostCapabilityDefinition, unit);
        return capability;
    }

    private static final void initializeDependencies(Profile profile) {

        Collection<BindingFactoryBean> allBindingFactories = new HashSet<>();
        allBindingFactories.add(convertToFactoryBean(BINDING_01));
        allBindingFactories.add(convertToFactoryBean(BINDING_02));
        allBindingFactories.add(convertToFactoryBean(HOST_BINDING));
        UnitDependencies unitDependencies = new UnitDependencyGraphCalculator(allBindingFactories).calculate();
        profile.setUnitDependencies(unitDependencies);
    }

    private static final BindingFactoryBean convertToFactoryBean(Binding binding) {
        BindingFactoryBean bfb = new BindingFactoryBean();
        bfb.setSourceUnit(binding.getSourceCapability().getUnit());
        bfb.setSourceCapabilityId(binding.getSourceCapability().getId());
        bfb.setTargetUnit(binding.getTargetCapability().getUnit());
        bfb.setTargetCapabilityId(binding.getTargetCapability().getId());
        return bfb;
    }
}
