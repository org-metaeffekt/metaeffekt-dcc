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
package org.metaeffekt.dcc.controller.commands;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.mapping.Binding;
import org.metaeffekt.dcc.commons.mapping.Capability;
import org.metaeffekt.dcc.commons.mapping.CapabilityDefinitionReference;
import org.metaeffekt.dcc.commons.mapping.CommandDefinition;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;

public abstract class AbstractUnitCapabilityAggregateStrategy<T, R extends CapabilityDefinitionReference> {

    private final String iteratorSequenceName;

    private final String iteratorSequenceNameTemplate;

    protected final ExecutionContext executionContext;

    public AbstractUnitCapabilityAggregateStrategy(String iteratorSequenceName,
            String iteratorSequenceNameTemplate, ExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.iteratorSequenceName = iteratorSequenceName;
        this.iteratorSequenceNameTemplate = iteratorSequenceNameTemplate;
    }

    protected abstract R getCapabilityRef(T item);

    protected abstract List<T> getItems(CommandDefinition command);

    public void processCollectInstructions(ConfigurationUnit unit, CommandDefinition command,
            Properties properties, boolean requiresBinding) {
        final List<T> items = getItems(command);
        for (int i = 0; i < items.size(); i++) {
            final T item = items.get(i);
            final R ref = getCapabilityRef(item);

            final Id<CapabilityId> capabilityId =
                Id.createCapabilityId(ref.getReferencedCapabilityDefId());
            if (capabilityId == null) {
                continue;
            }

            final Set<Id<UnitId>> unitsProvidingCapability = new LinkedHashSet<>();
            evaluateProperties(unit.getId(), capabilityId, unitsProvidingCapability,
                    ref.getPrefix(), properties);

            final Collection<Id<UnitId>> ids = collectConformingUnits(unit, item, ref);
            for (Id<UnitId> id : ids) {
                if (requiresBinding) {
                    // we need to infer the capability id from the binding source. The ids 
                    // (source/target) may differ
                    Collection<Binding> bindings = executionContext.getProfile().
                        findBindings(id, unit.getId(), null, capabilityId);
                    
                    // now iterate over all bindings and evaluate the properties
                    // NOTE-KKL: currently we may not support that there are multiple bindings that
                    //   fulfill this criteria are correctly integrated
                    for (Binding binding : bindings) {
                        evaluateProperties(id, binding.getSourceCapability().getId(), unitsProvidingCapability, ref.getPrefix(),
                                properties);
                    }
                } else {
                    evaluateProperties(id, capabilityId, unitsProvidingCapability, ref.getPrefix(),
                        properties);
                }
            }

            String iteratorSequence = serializeSequence(unitsProvidingCapability, ref.getPrefix());

            // For backward compatibility put for the first element without prefix
            if (i == 0) {
                properties.setProperty(iteratorSequenceName, iteratorSequence);
            }

            final String contributionSequenceName =
                String.format(iteratorSequenceNameTemplate, capabilityId.getValue());
            
            // merge with existing value, if already defined (multiple contributions may add to the sequence)
            String existingValue = properties.getProperty(contributionSequenceName);
            if (existingValue != null) {
                iteratorSequence = existingValue + "," + iteratorSequence;
            }
            
            properties.setProperty(contributionSequenceName, iteratorSequence);
        }
    }

    protected void evaluateProperties(Id<UnitId> unitId, Id<CapabilityId> capabilityId,
            Set<Id<UnitId>> unitsProvidingCapability, String prefix, Properties properties) {
        final Profile profile = executionContext.getProfile();
        ConfigurationUnit unit = profile.findUnit(unitId);
        Capability addInfoCapability = unit.findProvidedCapability(capabilityId);

        if (addInfoCapability != null) {
            final String prefixAndSeparator =
                StringUtils.isBlank(prefix) ? "." : "." + prefix.trim() + ".";
            Properties p = executionContext.getPropertiesHolder().getProperties(addInfoCapability);
            for (Map.Entry<Object, Object> entry : p.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();

                key = addInfoCapability.getUnit().getId() + prefixAndSeparator + key;
                properties.setProperty(key, value);

                unitsProvidingCapability.add(addInfoCapability.getUnit().getId());
            }
        }
    }

    private String serializeSequence(Collection<Id<UnitId>> list, String prefix) {
        if (CollectionUtils.isEmpty(list)) {
            return "";
        }

        final String suffix =
                StringUtils.isBlank(prefix) ? "" : "." + prefix.trim();
        
        final StringBuilder sb = new StringBuilder();
        for (Id<UnitId> s : list) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(s.getValue());
            sb.append(suffix);
        }

        return sb.toString();
    }

    protected abstract Collection<Id<UnitId>> collectConformingUnits(ConfigurationUnit unit,
            T item, R ref);
}
