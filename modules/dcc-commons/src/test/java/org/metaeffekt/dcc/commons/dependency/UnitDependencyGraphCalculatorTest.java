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
package org.metaeffekt.dcc.commons.dependency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.exception.CyclicBindingException;
import org.metaeffekt.dcc.commons.mapping.Binding;
import org.metaeffekt.dcc.commons.mapping.Capability;
import org.metaeffekt.dcc.commons.mapping.CapabilityDefinition;
import org.metaeffekt.dcc.commons.mapping.TestProfiles;
import org.metaeffekt.dcc.commons.spring.xml.BindingFactoryBean;

public class UnitDependencyGraphCalculatorTest {

    private UnitDependencyGraphCalculator calculator;
    private Collection<BindingFactoryBean> allBindingFactories;
    
    @Before
    public void prepare() {
        allBindingFactories = new LinkedList<BindingFactoryBean>();
        allBindingFactories.add(toBindingFactoryBean(TestProfiles.BINDING_01));
        allBindingFactories.add(toBindingFactoryBean(TestProfiles.BINDING_02));
        
        calculator = new UnitDependencyGraphCalculator(allBindingFactories);
    }
        
    @Test(expected=CyclicBindingException.class)
    public void testCalculateDependencyCycle() {

        Binding binding = new Binding(new Capability(
                Id.createCapabilityId("c1"), 
                new CapabilityDefinition("cap1"), 
                TestProfiles.CONFIGURATION_UNIT_01), 
                new Capability(
                        Id.createCapabilityId("c1"), 
                        new CapabilityDefinition("cap1"), 
                        TestProfiles.CONFIGURATION_UNIT_03));
        allBindingFactories.add(toBindingFactoryBean(binding));
        calculator = new UnitDependencyGraphCalculator(allBindingFactories);
        calculator.calculate();
    }
    
    @Test
    public void testCalculate() {

        UnitDependencies result = calculator.calculate();
        
        Map<Id<UnitId>, List<Id<UnitId>>> upstreamMatrix = result.getUpstreamMatrix();
        Map<Id<UnitId>, List<Id<UnitId>>> downstreamMatrix = result.getDownstreamMatrix();
        
        assertEquals(3, upstreamMatrix.keySet().size());
        
        List<Id<UnitId>> dependenciesOfUnit1 = upstreamMatrix.get(TestProfiles.CONFIGURATION_UNIT_01.getId());
        assertTrue(dependenciesOfUnit1.contains(TestProfiles.CONFIGURATION_UNIT_02.getId()));
        assertTrue(dependenciesOfUnit1.contains(TestProfiles.CONFIGURATION_UNIT_03.getId()));
        List<Id<UnitId>> dependenciesOfUnit2 = upstreamMatrix.get(TestProfiles.CONFIGURATION_UNIT_02.getId());
        assertTrue(dependenciesOfUnit2.contains(TestProfiles.CONFIGURATION_UNIT_03.getId()));
        List<Id<UnitId>> dependenciesOfUnit3 = upstreamMatrix.get(TestProfiles.CONFIGURATION_UNIT_03.getId());
        assertTrue(dependenciesOfUnit3.isEmpty());
        
        
        assertEquals(3, downstreamMatrix.keySet().size());
        List<Id<UnitId>> downstreamOfUnit1 = downstreamMatrix.get(TestProfiles.CONFIGURATION_UNIT_01.getId());
        assertTrue(downstreamOfUnit1.isEmpty());
        List<Id<UnitId>> downstreamOfUnit2 = downstreamMatrix.get(TestProfiles.CONFIGURATION_UNIT_02.getId());
        assertTrue(downstreamOfUnit2.contains(TestProfiles.CONFIGURATION_UNIT_01.getId()));
        List<Id<UnitId>> downstreamOfUnit3 = downstreamMatrix.get(TestProfiles.CONFIGURATION_UNIT_03.getId());
        assertTrue(downstreamOfUnit3.contains(TestProfiles.CONFIGURATION_UNIT_01.getId()));
        assertTrue(downstreamOfUnit3.contains(TestProfiles.CONFIGURATION_UNIT_02.getId()));

    }

    
    private BindingFactoryBean toBindingFactoryBean(Binding binding) {

        BindingFactoryBean bfb = new BindingFactoryBean();
        bfb.setSourceUnit(binding.getSourceCapability().getUnit());
        bfb.setSourceCapabilityId(binding.getSourceCapability().getId());
        bfb.setTargetUnit(binding.getTargetCapability().getUnit());
        bfb.setTargetCapabilityId(binding.getTargetCapability().getId());
        return bfb;
    }
}
