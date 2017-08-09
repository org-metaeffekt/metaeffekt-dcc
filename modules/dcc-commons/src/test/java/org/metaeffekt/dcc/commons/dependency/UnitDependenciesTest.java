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
import static org.junit.Assert.assertNotNull;

import java.util.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.TestProfiles;

public class UnitDependenciesTest {

    private UnitDependencies unitDependencies;
    private List<ConfigurationUnit> units;

    private ConfigurationUnit u1 = TestProfiles.CONFIGURATION_UNIT_01;
    private ConfigurationUnit u2 = TestProfiles.CONFIGURATION_UNIT_02;
    private ConfigurationUnit u3 = TestProfiles.CONFIGURATION_UNIT_03;
    
    
    @Before
    public void prepare() {
        unitDependencies = new UnitDependencies();

        unitDependencies.addBinding(u2.getId(), Id.createCapabilityId("c1"), u1.getId(), Id.createCapabilityId("c1"));
        unitDependencies.addBinding(u3.getId(), Id.createCapabilityId("c3"), u2.getId(), Id.createCapabilityId("c3"));

        units = new LinkedList<ConfigurationUnit>();
        units.add(u2);
        units.add(u1);
        units.add(u3);

        Map<String, List<Id<UnitId>>> upstreamCapabilityMatrix = new HashMap<>();
        Map<String, List<Id<UnitId>>> downstreamCapabilityMatrix = new HashMap<>();
    }

    @Test
    public void testSorting() {
        assertEquals(u2, units.get(0));
        assertEquals(u1, units.get(1));
        assertEquals(u3, units.get(2));
        
        unitDependencies.sort(units);

        assertEquals(u3, units.get(0));
        assertEquals(u2, units.get(1));
        assertEquals(u1, units.get(2));
    }

    @Test
    public void testEvaluateDependencyGroups() {
        ConfigurationUnit unitA = new ConfigurationUnit(Id.createUnitId("A"));
        ConfigurationUnit unitB = new ConfigurationUnit(Id.createUnitId("B"));
        ConfigurationUnit unitC = new ConfigurationUnit(Id.createUnitId("C"));
        ConfigurationUnit unitD = new ConfigurationUnit(Id.createUnitId("D"));
        ConfigurationUnit unitE = new ConfigurationUnit(Id.createUnitId("E"));
        ConfigurationUnit unitF = new ConfigurationUnit(Id.createUnitId("F"));
        ConfigurationUnit unitG = new ConfigurationUnit(Id.createUnitId("G"));

        UnitDependencies unitDependencies = new UnitDependencies();

        unitDependencies.addDependency(unitA, unitB);
        unitDependencies.addDependency(unitA, unitC);
        unitDependencies.addDependency(unitC, unitD);
        unitDependencies.addDependency(unitA, unitE);
        unitDependencies.addDependency(unitE, unitF);

        unitDependencies.resolveTransitiveDependencies();

        List<ConfigurationUnit> allUnits = new LinkedList<>();
        allUnits.add(unitB);
        allUnits.add(unitC);
        allUnits.add(unitE);
        allUnits.add(unitF);
        allUnits.add(unitG);
        allUnits.add(unitA);
        allUnits.add(unitD);

        unitDependencies.sort(allUnits);

        System.out.println("Sorted upstream:");
        for (ConfigurationUnit unit : allUnits) {
            System.out.print(unit.getId() + " ");
        }
        System.out.println();

        final List<List<ConfigurationUnit>> unitGroups = unitDependencies.evaluateDependencyGroups(allUnits);
        System.out.println("(Independent) dependency groups:");
        for (List<ConfigurationUnit> groupList : unitGroups) {
            unitDependencies.sort(groupList);
            for (ConfigurationUnit unit : groupList) {
                System.out.print(unit.getId() + " ");
            }
            System.out.println();
        }

        Assert.assertTrue(unitGroups.get(0).contains(unitA));
        Assert.assertTrue(unitGroups.get(0).contains(unitG));

        Assert.assertTrue(unitGroups.get(1).contains(unitB));
        Assert.assertTrue(unitGroups.get(1).contains(unitC));
        Assert.assertTrue(unitGroups.get(1).contains(unitE));

        Assert.assertTrue(unitGroups.get(2).contains(unitD));
        Assert.assertTrue(unitGroups.get(2).contains(unitF));
    }

    @Test
    public void getDirectUpstreamUnits() {
        List<Id<UnitId>> unitsDependendingOnU1C1 = unitDependencies.getDirectUpstreamUnits(u1.getId(), Id.createCapabilityId("c1"));
        assertNotNull(unitsDependendingOnU1C1);
        assertEquals(1, unitsDependendingOnU1C1.size());

        List<Id<UnitId>> unitsDependendingOnU1C2 = unitDependencies.getDirectUpstreamUnits(u1.getId(), Id.createCapabilityId("c2"));
        assertNotNull(unitsDependendingOnU1C2);
        assertEquals(0, unitsDependendingOnU1C2.size());

        List<Id<UnitId>> unitsDependendingOnU2C1 = unitDependencies.getDirectUpstreamUnits(u2.getId(), Id.createCapabilityId("c1"));
        assertNotNull(unitsDependendingOnU2C1);
        assertEquals(0, unitsDependendingOnU2C1.size());
    }

}
