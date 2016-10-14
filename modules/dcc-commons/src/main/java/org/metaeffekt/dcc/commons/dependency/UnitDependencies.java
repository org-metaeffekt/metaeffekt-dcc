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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;

public class UnitDependencies {

    public static final UnitDependencies NO_DEPENDENCIES = new NoDependencies();

    private final Map<Id<UnitId>, List<Id<UnitId>>> upstreamMatrix;
    private final Map<Id<UnitId>, List<Id<UnitId>>> downstreamMatrix;
    private Map<String, List<Id<UnitId>>> upstreamCapabilityMatrix;
    private Map<String, List<Id<UnitId>>> downstreamCapabilityMatrix;

    UnitDependencies(Map<Id<UnitId>, List<Id<UnitId>>> upstreamMatrix, Map<Id<UnitId>, List<Id<UnitId>>> downstreamMatrix,
                     Map<String, List<Id<UnitId>>> upstreamCapabilityMatrix, Map<String, List<Id<UnitId>>> downstreamCapabilityMatrix) {
        this.upstreamMatrix = upstreamMatrix;
        this.downstreamMatrix = downstreamMatrix;
        this.upstreamCapabilityMatrix = upstreamCapabilityMatrix;
        this.downstreamCapabilityMatrix = downstreamCapabilityMatrix;
    }

    public Map<Id<UnitId>, List<Id<UnitId>>> getUpstreamMatrix() {
        return Collections.unmodifiableMap(upstreamMatrix);
    }
    
    public Map<Id<UnitId>, List<Id<UnitId>>> getDownstreamMatrix() {
        return Collections.unmodifiableMap(downstreamMatrix);
    }

    public List<Id<UnitId>> getDirectUpstreamUnits(Id<UnitId> unitId, Id<CapabilityId> capabilityId) {
        String key = createKey(unitId, capabilityId);
        List<Id<UnitId>> result = upstreamCapabilityMatrix.get(key);
        if (result == null) {
            return Collections.emptyList();
        }
        else {
            return Collections.unmodifiableList(result);
        }
    }

    public List<Id<UnitId>> getDirectDownstreamUnits(Id<UnitId> unitId, Id<CapabilityId> capabilityId) {
        String key = createKey(unitId, capabilityId);
        List<Id<UnitId>> result = downstreamCapabilityMatrix.get(key);
        if (result == null) {
            return Collections.emptyList();
        }
        else {
            return Collections.unmodifiableList(result);
        }
    }

    public void sortUpstream(List<ConfigurationUnit> units) {
        sort(units, upstreamMatrix);
    }
    
    public void sortDownstream(List<ConfigurationUnit> units) {
        final Map<Id<UnitId>, ConfigurationUnit> idUnitMap = new HashMap<>();
        final Map<Id<UnitId>, List<Id<UnitId>>> idListMap = new HashMap<>();
        final Map<Id<UnitId>, List<Id<UnitId>>> downstreamMatrix = getDownstreamMatrix();
        
        for (ConfigurationUnit unit : units) {
            idUnitMap.put(unit.getId(), unit);
        }
        
        for (ConfigurationUnit unit : units) {
            List<Id<UnitId>> list = idListMap.get(unit.getId());
            if (list == null) {
                list = new ArrayList<>();
                idListMap.put(unit.getId(), list);
            }

            final List<Id<UnitId>> downstreamList = downstreamMatrix.get(unit.getId());
            if (downstreamList != null) {
                for (Id<UnitId> id : downstreamList) {
                    if (idUnitMap.containsKey(id)) {
                        list.add(id);
                        if (!idListMap.containsKey(id)) {
                            idListMap.put(id, list);
                        }
                    }
                }
            }
        }
        
        // reorder the units based on the ordered lists
        for (List<Id<UnitId>> ids : idListMap.values()) {
            sortIds(ids, downstreamMatrix);
            for (Id<UnitId> id : ids) {
                units.remove(idUnitMap.get(id));
                units.add(idUnitMap.get(id));
            }
        }
        
        sort(units, downstreamMatrix);
    }
    
    public void sortIdsUpstream(List<Id<UnitId>> unitIds) {
        sortIds(unitIds, upstreamMatrix);
    }
    
    public void sortIdsDownstream(List<Id<UnitId>> unitIds) {
        sortIds(unitIds, downstreamMatrix);
    }
    

    private void sortIds(List<Id<UnitId>> units, final Map<Id<UnitId>, List<Id<UnitId>>> matrix) {
        
        Collections.sort(units, new Comparator<Id<UnitId>>() {

            @Override
            public int compare(Id<UnitId> o1, Id<UnitId> o2) {

                if (matrix.get(o1).contains(o2)) {
                    return -1;
                }
                else if (matrix.get(o2).contains(o1)) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
        });
    }
    
    private void sort(List<ConfigurationUnit> units, final Map<Id<UnitId>, List<Id<UnitId>>> matrix) {
        
        Collections.sort(units, new Comparator<ConfigurationUnit>() {

            @Override
            public int compare(ConfigurationUnit o1, ConfigurationUnit o2) {

                if (matrix.get(o1.getId()).contains(o2.getId())) {
                    return -1;
                }
                else if (matrix.get(o2.getId()).contains(o1.getId())) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
        });
    }

    static String createKey(Id<UnitId> unitId, Id<CapabilityId> capabilityId) {
        return unitId.getValue() + "#" + capabilityId.getValue();
    }

    @Override
    public String toString() {
        return "UnitDependencies [upstreamMatrix=" + upstreamMatrix + ", downstreamMatrix="
                + downstreamMatrix + ", upstreamCapabilityMatrix=" + upstreamCapabilityMatrix
                + ", downstreamCapabilityMatrix=" + downstreamCapabilityMatrix + "]";
    }

}
