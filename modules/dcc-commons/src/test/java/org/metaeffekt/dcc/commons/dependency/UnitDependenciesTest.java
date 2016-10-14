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
package org.metaeffekt.dcc.commons.dependency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
        Map<Id<UnitId>, List<Id<UnitId>>> upstreamMatrix = new HashMap<>();
        List<Id<UnitId>> u1ups = new LinkedList<>();
        u1ups.add(u2.getId());
        List<Id<UnitId>> u2ups = new LinkedList<>();
        u2ups.add(u3.getId());
        List<Id<UnitId>> u3ups = new LinkedList<>();
        upstreamMatrix.put(u1.getId(), u1ups );
        upstreamMatrix.put(u2.getId(), u2ups );
        upstreamMatrix.put(u3.getId(), u3ups );
        
        Map<Id<UnitId>, List<Id<UnitId>>> downstreamMatrix= new HashMap<>();
        List<Id<UnitId>> u1ds = new LinkedList<>();
        List<Id<UnitId>> u2ds = new LinkedList<>();
        u2ds.add(u1.getId());
        List<Id<UnitId>> u3ds = new LinkedList<>();
        u3ds.add(u2.getId());
        downstreamMatrix.put(u1.getId(), u1ds );
        downstreamMatrix.put(u2.getId(), u2ds );
        downstreamMatrix.put(u3.getId(), u3ds );

        units = new LinkedList<ConfigurationUnit>();
        units.add(u2);
        units.add(u1);
        units.add(u3);

        Map<String, List<Id<UnitId>>> upstreamCapabilityMatrix = new HashMap<>();
        Map<String, List<Id<UnitId>>> downstreamCapabilityMatrix = new HashMap<>();

        List<Id<UnitId>> capUnits = new LinkedList<>();
        capUnits.add(u2.getId());
        upstreamCapabilityMatrix.put("u1#c1", capUnits);

        unitDependencies = new UnitDependencies(upstreamMatrix, downstreamMatrix, upstreamCapabilityMatrix, downstreamCapabilityMatrix);
    }
    

    @Test
    public void testSorting() {

        
        assertEquals(u2, units.get(0));
        assertEquals(u1, units.get(1));
        assertEquals(u3, units.get(2));
        
        unitDependencies.sortUpstream(units);
        
        assertEquals(u1, units.get(0));
        assertEquals(u2, units.get(1));
        assertEquals(u3, units.get(2));
        
        unitDependencies.sortDownstream(units);
        
        assertEquals(u3, units.get(0));
        assertEquals(u2, units.get(1));
        assertEquals(u1, units.get(2));
        
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
