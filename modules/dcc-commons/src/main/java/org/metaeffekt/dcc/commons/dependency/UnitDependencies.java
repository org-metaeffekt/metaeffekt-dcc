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

import java.util.*;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.exception.CyclicBindingException;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;

public class UnitDependencies {

    private Map<Id<UnitId>, List<Id<UnitId>>> upstreamMatrix;
    private Map<Id<UnitId>, List<Id<UnitId>>> downstreamMatrix;

    private Map<String, List<Id<UnitId>>> upstreamCapabilityMatrix;
    private Map<String, List<Id<UnitId>>> downstreamCapabilityMatrix;

    public UnitDependencies() {
        this.upstreamMatrix = new HashMap<>();
        this.downstreamMatrix = new HashMap<>();
        this.upstreamCapabilityMatrix = new HashMap<>();
        this.downstreamCapabilityMatrix = new HashMap<>();
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
        } else {
            return Collections.unmodifiableList(result);
        }
    }
    
    public void sort(List<ConfigurationUnit> units) {
        final Map<Id<UnitId>, ConfigurationUnit> idUnitMap = new HashMap<>();
        final Map<Id<UnitId>, List<Id<UnitId>>> idListMap = new HashMap<>();
        final Map<Id<UnitId>, List<Id<UnitId>>> downstreamMatrix = getDownstreamMatrix();
        
        for (ConfigurationUnit unit : units) {
            idUnitMap.put(unit.getId(), unit);
        }
        
        for (ConfigurationUnit unit : units) {
            List<Id<UnitId>> list = idListMap.get(unit.getId());
            if (list == null) {
                list = new LinkedList<>();
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
            final List<Id<UnitId>> sortedIds = sortIds(ids, downstreamMatrix);
            for (Id<UnitId> id : sortedIds) {
                units.remove(idUnitMap.get(id));
                units.add(idUnitMap.get(id));
            }
        }
        
        sort(units, downstreamMatrix);
    }

    /**
     * Evaluates the given unitList for groups of {@link ConfigurationUnit}s that can be executed concurrently. The
     * current implementation analyzes the upstream dependencies. The result needs to be executed in the given order
     * on first level. The units in the second level are independent and can be ordered arbitrarily in terms of
     * execution.
     *
     * @param unitList The list of units to analyze.
     * @return A list of lists. The first level must be executed in the given order. Within the second level the units
     *   are independent and can be executed in any order. In particular, these can be executed concurrently.
     */
    public List<List<ConfigurationUnit>> evaluateDependencyGroups(final List<ConfigurationUnit> unitList) {
        final List<List<ConfigurationUnit>> groupList = new LinkedList<>();
        final HashSet<Id<UnitId>> consumed = new HashSet<>();

        // the algorithm requires that the units are sorted (downstream)
        sort(unitList);

        for (int i = 0; i < unitList.size(); i++) {
            final ConfigurationUnit unitA = unitList.get(i);

            // check whether unitA was already processed
            if (consumed.contains(unitA.getId())) {
                continue;
            }

            // create a group for unitA and add it to the result data structure
            final List<ConfigurationUnit> unitGroup = new LinkedList<>();
            unitGroup.add(unitA);
            groupList.add(unitGroup);

            final List<Id<UnitId>> groupIdList = new LinkedList<>();
            groupIdList.add(unitA.getId());

            final List<Id<UnitId>> excludedList = new LinkedList<>();

            // evaluate unitList with respect to unitA; collect those, which are independent of A
            for (int j = i + 1; j < unitList.size(); j++) {
                final ConfigurationUnit unitB = unitList.get(j);

                if (consumed.contains(unitB.getId())) {
                    continue;
                }

                // check whether unit unitB is dependent on unit unitA
                if (dependsOnAny(unitB.getId(), groupIdList)) {
                    // keep; cannot be parallel to unitA
                    excludedList.add(unitB.getId());
                } else {
                    if (dependsOnAny(unitB.getId(), excludedList)) {
                        // skip
                    } else {
                        // unit unitB is independent of unitA and can be parallelized with unitA
                        consumed.add(unitB.getId());
                        unitGroup.add(unitB);
                        groupIdList.add(unitB.getId());
                    }
                }
            }
        }

        // within a group the units are sorted alphanumerically
        for (List<ConfigurationUnit> group : groupList) {
            Collections.sort(group, new Comparator<ConfigurationUnit>() {
                @Override
                public int compare(ConfigurationUnit o1, ConfigurationUnit o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
        }

        return groupList;
    }

    /**
     * Evaluates whether unitId depends on any other unitId in unitIdList.
     *
     * @param unitId
     * @param unitIdList
     * @return Returns {@code true} in case unitId depends on any unitId in unitIdList.
     */
    private boolean dependsOnAny(final Id<UnitId> unitId, final List<Id<UnitId>> unitIdList) {
        for (final Id<UnitId> u: unitIdList) {
            final List<Id<UnitId>> ids = upstreamMatrix.get(unitId);
            if (ids != null && ids.contains(u)) {
                return true;
            }
        }
        return false;
    }

    private int compare(Id<UnitId> o1, Id<UnitId> o2, final Map<Id<UnitId>, List<Id<UnitId>>> matrix) {
        final List<Id<UnitId>> ids1 = matrix.get(o1);
        final List<Id<UnitId>> ids2 = matrix.get(o2);
        if (ids1 != null && ids1.contains(o2)) {
            return -1;
        } else {
            if (ids2 != null && ids2.contains(o1)) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Sort implementation that does not rely on symmetric compare results.
     *
     * @param units The units to be sorted.
     * @param matrix The dependency matrix.
     *
     * @return The sorted list.
     */
    private List<Id<UnitId>> sortIds(List<Id<UnitId>> units, final Map<Id<UnitId>, List<Id<UnitId>>> matrix) {
        final List<Id<UnitId>> ordered = new ArrayList<>();
        for (final Id<UnitId> unitId : units) {
            int index = -1;
            // find position in ordered list
            for (int i = 0; i < ordered.size(); i++) {
                final Id<UnitId> otherUnitId = ordered.get(i);
                final int compare = compare(unitId, otherUnitId, matrix);
                if (compare == -1) {
                    index = i;
                    break;
                } else if (compare == 1) {
                    // keep searching
                } else {
                    // not dependent (on this unit), keep searching
                }
            }
            // in case not other units depends on the unit, just add it at the end.
            if (index == -1) {
                ordered.add(unitId);
            } else {
                // otherwise insert the unit, so that it's dependent units come after
                ordered.add(index, unitId);
            }
        }
        return ordered;
    }
    
    private void sort(List<ConfigurationUnit> units, final Map<Id<UnitId>, List<Id<UnitId>>> matrix) {
        List<ConfigurationUnit> ordered = new ArrayList<>();
        for (ConfigurationUnit unit : units) {
            int index = -1;
            // find position in ordered list
            for (int i = 0; i < ordered.size(); i++) {
                ConfigurationUnit otherUnit = ordered.get(i);
                final int compare = compare(unit.getId(), otherUnit.getId(), matrix);
                if (compare == -1) {
                    index = i;
                    break;
                } else if (compare == 1) {
                    // keep searching
                } else {
                    // not dependent (on this unit), keep searching
                }
            }
            // in case not other units depends on the unit, just add it at the end.
            if (index == -1) {
                ordered.add(unit);
            } else {
                // otherwise insert the unit, so that it's dependent units come after
                ordered.add(index, unit);
            }
        }

        // replace lists internally
        units.clear();
        units.addAll(ordered);
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

    void addDependency(ConfigurationUnit sourceUnit, ConfigurationUnit targetUnitId) {
        addDependency(sourceUnit.getId(), targetUnitId.getId());
    }

    void addDependency(Id<UnitId> sourceUnitId, Id<UnitId> targetUnitId) {
        addDependency(targetUnitId, sourceUnitId, upstreamMatrix);
        addDependency(sourceUnitId, targetUnitId, downstreamMatrix);
    }

    private void addDependency(Id<UnitId> unitB, Id<UnitId> unitA, Map<Id<UnitId>, List<Id<UnitId>>> matrix) {
        List<Id<UnitId>> upstreamList = matrix.get(unitB);
        if (upstreamList == null) {
            upstreamList = new LinkedList<>();
            matrix.put(unitB, upstreamList);
        }
        upstreamList.add(unitA);

        // create also empty list
        upstreamList = matrix.get(unitA);
        if (upstreamList == null) {
            upstreamList = new LinkedList<>();
            matrix.put(unitA, upstreamList);
        }

    }

    void addBinding(Id<UnitId> sourceUnitId, Id<CapabilityId> sourceCapabilityId,
                    Id<UnitId> targetUnitId, Id<CapabilityId> targetCapabilityId) {

        addDependency(sourceUnitId, targetUnitId);

        addToCapabilityMatrix(upstreamCapabilityMatrix, targetUnitId, targetCapabilityId, sourceUnitId);
        addToCapabilityMatrix(downstreamCapabilityMatrix, sourceUnitId, sourceCapabilityId, targetUnitId);
    }

    private void addToCapabilityMatrix(Map<String, List<Id<UnitId>>> matrix, Id<UnitId> targetUnitId, Id<CapabilityId> targetCapabilityId, Id<UnitId> sourceUnitId) {
        String targetKey = UnitDependencies.createKey(targetUnitId, targetCapabilityId);
        if (!matrix.keySet().contains(targetKey)) {
            List<Id<UnitId>> upstreamUnits = new LinkedList<>();
            upstreamUnits.add(sourceUnitId);
            matrix.put(targetKey, upstreamUnits);
        } else {
            List<Id<UnitId>> upstreamUnits = matrix.get(targetKey);
            if (!upstreamUnits.contains(sourceUnitId)) {
                upstreamUnits.add(sourceUnitId);
            }
        }
    }

    public void resolveTransitiveDependencies() {
        resolveTransitiveDependencies(upstreamMatrix);
        resolveTransitiveDependencies(downstreamMatrix);
    }

    private void resolveTransitiveDependencies(final Map<Id<UnitId>, List<Id<UnitId>>> matrix) {
        for (Id<UnitId> unit : matrix.keySet()) {
            for (Id<UnitId> other : matrix.keySet()) {
                if (unit.equals(other)) {continue;}

                List<Id<UnitId>> units = matrix.get(other);
                if (units != null && units.contains(unit)) {
                    List<Id<UnitId>> dependenciesToAdd = matrix.get(unit);
                    for (Id<UnitId> dep : new LinkedList<>(dependenciesToAdd)) {
                        if (!units.contains(dep)) {
                            units.add(dep);
                        }
                    }
                }
            }
        }
        checkForCycle(matrix);
    }

    private void checkForCycle(Map<Id<UnitId>, List<Id<UnitId>>> graph) {
        Set<String> cycles = new HashSet<>();
        for (Id<UnitId> key : graph.keySet()) {
            if (graph.get(key).contains(key)) {
                LinkedList<Id<UnitId>> deps = new LinkedList<>(graph.get(key));
                Collections.sort(deps);
                cycles.add(deps.toString());
            }
        }
        if (!cycles.isEmpty()) {
            throw new CyclicBindingException(cycles);
        }
    }

}
