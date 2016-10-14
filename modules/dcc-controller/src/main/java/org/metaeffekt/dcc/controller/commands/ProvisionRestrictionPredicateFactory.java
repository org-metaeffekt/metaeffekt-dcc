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
package org.metaeffekt.dcc.controller.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.HostName;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.HostRestriction;
import org.metaeffekt.dcc.commons.mapping.ProvisionRestriction;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;

public class ProvisionRestrictionPredicateFactory {

    public static Predicate<ConfigurationUnit> createPredicate(
            List<ProvisionRestriction> restrictions,
            ConfigurationUnit commandUnit, ExecutionContext executionContext) {
        final List<Predicate<ConfigurationUnit>> predicates = new ArrayList<>();
        predicates.add(createNotTheSamePredicate(commandUnit));

        if (!CollectionUtils.isEmpty(restrictions)) {
            for (ProvisionRestriction restriction : restrictions) {
                predicates.add(createPredicate(restriction, commandUnit, executionContext));
            }
        }

        return Predicates.and(predicates);
    }

    public static Predicate<ConfigurationUnit> createPredicate(ProvisionRestriction restriction,
            ConfigurationUnit commandUnit, ExecutionContext executionContext) {
        Predicate<ConfigurationUnit> predicate = null;
        if (HostRestriction.class.isInstance(restriction)) {
            predicate = createHostPredicate(commandUnit, executionContext);
        }

        Validate.notNull(predicate, "Unknown restriction type.");
        return predicate;
    }

    private static Predicate<ConfigurationUnit> createNotTheSamePredicate(
            final ConfigurationUnit commandUnit) {
        final Predicate<ConfigurationUnit> predicate = new Predicate<ConfigurationUnit>() {

            private Id<UnitId> unitId = commandUnit.getId();

            @Override
            public boolean apply(ConfigurationUnit item) {
                return !unitId.equals(item.getId());
            }

        };

        return predicate;

    }

    private static Predicate<ConfigurationUnit> createHostPredicate(
            final ConfigurationUnit commandUnit, final ExecutionContext executionContext) {
        final Predicate<ConfigurationUnit> predicate = new Predicate<ConfigurationUnit>() {

            private Id<UnitId> commandUnitId = commandUnit.getId();

            private Id<HostName> hostName = executionContext.getHostForUnit(commandUnitId);
            private Id<UnitId> hostUnitId = executionContext.getHostUnitForUnit(commandUnitId);
            
            // FIXME: currently this cannot be influenced; compare logically is the fixed default.
            private boolean compareHostByName = false;
            
            @Override
            public boolean apply(ConfigurationUnit item) {
                if (compareHostByName) {
                    // this matches the host by name (physical host).
                    final Id<HostName> itemHostName = executionContext.getHostForUnit(item.getId());
                    return hostName.equals(itemHostName);
                } else {
                    // this compares on the unit level (logical hosts)
                    final Id<UnitId> itemHostUnitId = executionContext.getHostUnitForUnit(item.getId());
                    return hostUnitId.equals(itemHostUnitId);
                }
            }
        };

        return predicate;
    }

}
