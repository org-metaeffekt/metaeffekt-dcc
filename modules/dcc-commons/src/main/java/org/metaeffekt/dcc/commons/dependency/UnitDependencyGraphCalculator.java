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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.exception.CyclicBindingException;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.spring.xml.BindingFactoryBean;

public class UnitDependencyGraphCalculator {

    private Collection<BindingFactoryBean> allBindingFactories;

    public UnitDependencyGraphCalculator(Collection<BindingFactoryBean> allBindingFactories) {
        this.allBindingFactories = new HashSet<BindingFactoryBean>(allBindingFactories);
    }
    
    
    public UnitDependencies calculate() {
        
        Map<Id<UnitId>, List<Id<UnitId>>> upstreamMatrix = new HashMap<>();
        Map<Id<UnitId>, List<Id<UnitId>>> downstreamMatrix = new HashMap<>();

        Map<String, List<Id<UnitId>>> upstreamCapabilityMatrix = new HashMap<>();
        Map<String, List<Id<UnitId>>> downstreamCapabilityMatrix = new HashMap<>();

        for (BindingFactoryBean binding : allBindingFactories) {

            ConfigurationUnit targetUnit = binding.getTargetUnit();
            ConfigurationUnit sourceUnit = binding.getSourceUnit();

            addToMatrix(downstreamMatrix, sourceUnit.getId(), targetUnit.getId());
            addToMatrix(upstreamMatrix, targetUnit.getId(), sourceUnit.getId());

            addToCapabilityMatrix(upstreamCapabilityMatrix, targetUnit.getId(), binding.getTargetCapabilityId(), sourceUnit.getId());
            addToCapabilityMatrix(downstreamCapabilityMatrix, sourceUnit.getId(), binding.getSourceCapabilityId(), targetUnit.getId());
        }
        
        upstreamMatrix = resolveTransitiveDependencies(upstreamMatrix);
        downstreamMatrix = resolveTransitiveDependencies(downstreamMatrix);
        
        return new UnitDependencies(upstreamMatrix, downstreamMatrix, upstreamCapabilityMatrix, downstreamCapabilityMatrix);
        
    }

    private void addToCapabilityMatrix(Map<String, List<Id<UnitId>>> matrix, Id<UnitId> targetUnitId, Id<CapabilityId> targetCapabilityId, Id<UnitId> sourceUnitId) {

        String targetKey = UnitDependencies.createKey(targetUnitId, targetCapabilityId);

        if (!matrix.keySet().contains(targetKey)) {

            List<Id<UnitId>> upstreamUnits = new LinkedList<>();
            upstreamUnits.add(sourceUnitId);
            matrix.put(targetKey, upstreamUnits);
        }

        else {
            List<Id<UnitId>> upstreamUnits = matrix.get(targetKey);
            upstreamUnits.add(sourceUnitId);
        }

    }

    private void addToMatrix(Map<Id<UnitId>, List<Id<UnitId>>> matrix, Id<UnitId> a, Id<UnitId> b) {
        
        if (!matrix.keySet().contains(a)) {
            
            if (!matrix.keySet().contains(b)) {
                matrix.put(b, new LinkedList<Id<UnitId>>());
            }
            
            LinkedList<Id<UnitId>> dependencyList = new LinkedList<>();
            dependencyList.add(b);
            matrix.put(a, dependencyList);
        }
        
        else {
            
            if (!matrix.keySet().contains(b)) {
                matrix.put(b, new LinkedList<Id<UnitId>>());
            }
            
            List<Id<UnitId>> dependencyList = matrix.get(a);
            
            if (!dependencyList.contains(b)) {
                dependencyList.add(b);
            }
            
        }
    }

    private Map<Id<UnitId>,List<Id<UnitId>>> resolveTransitiveDependencies(final Map<Id<UnitId>, List<Id<UnitId>>> graph) {

        Map<Id<UnitId>,List<Id<UnitId>>> copy = deepCopyMap(graph);
        
        for (Id<UnitId> unit : graph.keySet()) {
            
            for (Id<UnitId> other : copy.keySet()) {
                if (unit.equals(other)) {continue;}

                List<Id<UnitId>> deps = copy.get(other);
                if (deps!=null && deps.contains(unit)) {
                    List<Id<UnitId>> dependenciesToAdd = copy.get(unit);
                    
                    for (Id<UnitId> dep : dependenciesToAdd) {
                        if (!deps.contains(dep)) {
                            deps.add(dep);
                        }
                    }
                }
            }
        }
        
        checkForCycle(copy);
        
        return copy;
        
    }
    
    private Map<Id<UnitId>,List<Id<UnitId>>> deepCopyMap(final Map<Id<UnitId>, List<Id<UnitId>>> graph) {

        Map<Id<UnitId>,List<Id<UnitId>>> tmp = new HashMap<>();
        
        for (Id<UnitId> unit : graph.keySet()) {
            tmp.put(unit, new LinkedList<Id<UnitId>>(graph.get(unit)));
        }
                
        return tmp;
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
