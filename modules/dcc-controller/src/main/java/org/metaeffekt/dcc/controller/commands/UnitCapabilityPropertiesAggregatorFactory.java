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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Predicate;
import org.metaeffekt.dcc.commons.DccProperties;
import org.metaeffekt.dcc.commons.dependency.UnitDependencies;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.mapping.Capability;
import org.metaeffekt.dcc.commons.mapping.CapabilityDefinitionExtensionReference;
import org.metaeffekt.dcc.commons.mapping.CapabilityDefinitionReference;
import org.metaeffekt.dcc.commons.mapping.CommandDefinition;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.Provision;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;

public class UnitCapabilityPropertiesAggregatorFactory {

    public static AbstractUnitCapabilityAggregateStrategy<Provision, CapabilityDefinitionReference> createProvisionAggregator(
            ExecutionContext executionContext) {
        return new AbstractUnitCapabilityAggregateStrategy<Provision, CapabilityDefinitionReference>(
                DccProperties.DCC_UNIT_PROVISION_ITERATOR_SEQUENCE,
                DccProperties.DCC_UNIT_PROVISION_ITERATOR_SEQUENCE_TEMPLATE,
                executionContext) {

            @Override
            protected List<Provision> getItems(CommandDefinition command) {
                return command.getProvisions();
            }

            @Override
            protected CapabilityDefinitionReference getCapabilityRef(Provision item) {
                return item.getCapabilityRef();
            }

            @Override
            protected Collection<Id<UnitId>> collectConformingUnits(ConfigurationUnit unit,
                    Provision item, CapabilityDefinitionReference ref) {

                final Predicate<ConfigurationUnit> predicate =
                    ProvisionRestrictionPredicateFactory.createPredicate(item.getRestrictions(),
                            unit, executionContext);

                final Id<CapabilityId> capabilityId = Id.createCapabilityId(ref.getReferencedCapabilityDefId());
                final List<ConfigurationUnit> unitsProvidingCapability = new ArrayList<ConfigurationUnit>();
                for (ConfigurationUnit unitItem : executionContext.getProfile().getUnits()) {
                    if (unitItem.findProvidedCapability(capabilityId) != null) {
                        unitsProvidingCapability.add(unitItem);
                    }
                }

                final Set<Id<UnitId>> unitIds = new HashSet<Id<UnitId>>();
                for (ConfigurationUnit unitProvidingCapability : unitsProvidingCapability) {
                    if (predicate.apply(unitProvidingCapability)) {
                        unitIds.add(unitProvidingCapability.getId());
                    }
                }

                return unitIds;
            }
        };
    }

    public static AbstractUnitCapabilityAggregateStrategy<CapabilityDefinitionExtensionReference, CapabilityDefinitionExtensionReference> createContributionAggregator(
            ExecutionContext executionContext) {
        return new AbstractUnitCapabilityAggregateStrategy<CapabilityDefinitionExtensionReference, CapabilityDefinitionExtensionReference>(
                DccProperties.DCC_UNIT_CONTRIBUTION_ITERATOR_SEQUENCE,
                DccProperties.DCC_UNIT_CONTRIBUTION_ITERATOR_SEQUENCE_TEMPLATE,
                executionContext) {

            @Override
            protected List<CapabilityDefinitionExtensionReference> getItems(
                    CommandDefinition command) {
                return command.getContributions();
            }

            @Override
            protected CapabilityDefinitionExtensionReference getCapabilityRef(
                    CapabilityDefinitionExtensionReference item) {
                return item;
            }

            @Override
            protected Collection<Id<UnitId>> collectConformingUnits(ConfigurationUnit unit,
                    CapabilityDefinitionExtensionReference item,
                    CapabilityDefinitionExtensionReference ref) {

                final Id<CapabilityId> boundToCapability =
                    Id.createCapabilityId(item.getBoundToCapabilityId());

                return getDownstreamUnitIds(unit, boundToCapability);
            }

            protected List<Id<UnitId>> getDownstreamUnitIds(ConfigurationUnit unit,
                    Id<CapabilityId> boundToCapabilityId) {
                final List<Id<UnitId>> ids = new LinkedList<>();
                final Profile profile = executionContext.getProfile();
                final UnitDependencies dependencies = profile.getUnitDependencies();
                if (boundToCapabilityId != null) {
                    ids.addAll(dependencies.getDirectDownstreamUnits(unit.getId(),
                            boundToCapabilityId));
                } else {
                    for (Capability providedCapability : unit.getProvidedCapabilities()) {
                        ids.addAll(dependencies.getDirectDownstreamUnits(unit.getId(),
                                providedCapability.getId()));
                    }
                }
                return ids;
            }

        };

    }

    public static AbstractUnitCapabilityAggregateStrategy<CapabilityDefinitionExtensionReference, CapabilityDefinitionExtensionReference> createRequisitionAggregator(
            ExecutionContext executionContext) {
        return new AbstractUnitCapabilityAggregateStrategy<CapabilityDefinitionExtensionReference, CapabilityDefinitionExtensionReference>(
                DccProperties.DCC_UNIT_REQUISITION_ITERATOR_SEQUENCE,
                DccProperties.DCC_UNIT_REQUISITION_ITERATOR_SEQUENCE_TEMPLATE,
                executionContext) {

            @Override
            protected List<CapabilityDefinitionExtensionReference> getItems(
                    CommandDefinition command) {
                return command.getRequisitions();
            }

            @Override
            protected CapabilityDefinitionExtensionReference getCapabilityRef(
                    CapabilityDefinitionExtensionReference item) {
                return item;
            }

            @Override
            protected Collection<Id<UnitId>> collectConformingUnits(ConfigurationUnit unit,
                    CapabilityDefinitionExtensionReference item,
                    CapabilityDefinitionExtensionReference ref) {

                final Id<CapabilityId> boundToCapability =
                    Id.createCapabilityId(item.getBoundToCapabilityId());

                return getUpstreamUnitIds(unit, boundToCapability);
            }

            protected List<Id<UnitId>> getUpstreamUnitIds(ConfigurationUnit unit,
                    Id<CapabilityId> boundToCapabilityId) {
                final List<Id<UnitId>> ids = new LinkedList<>();
                final Profile profile = executionContext.getProfile();
                final UnitDependencies dependencies = profile.getUnitDependencies();
                if (boundToCapabilityId != null) {
                    ids.addAll(dependencies.getDirectUpstreamUnits(unit.getId(),
                            boundToCapabilityId));
                } else {
                    for (Capability providedCapability : unit.getProvidedCapabilities()) {
                        ids.addAll(dependencies.getDirectUpstreamUnits(unit.getId(),
                                providedCapability.getId()));
                    }
                }
                return ids;
            }

        };

    }

}
