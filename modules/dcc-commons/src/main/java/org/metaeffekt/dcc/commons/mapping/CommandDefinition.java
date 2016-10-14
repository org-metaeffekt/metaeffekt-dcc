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
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;

import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.PackageId;

public class CommandDefinition {

    private final Commands command;

    private final List<CapabilityDefinitionReference> capabilities = new ArrayList<>();

    private final List<CapabilityDefinitionExtensionReference> contributions = new ArrayList<>();

    private final List<CapabilityDefinitionExtensionReference> requisitions = new ArrayList<>();

    private final List<Provision> provisions = new ArrayList<>();

    private final Id<PackageId> packageId;

    public CommandDefinition(Commands command, Id<PackageId> packageId) {
        Validate.notNull(command);
        Validate.notNull(packageId);

        this.command = command;
        this.packageId = packageId;
    }

    public Commands getCommandId() {
        return command;
    }

    public void setCapabilities(List<CapabilityDefinitionReference> capabilities) {
        this.capabilities.clear();

        if (!CollectionUtils.isEmpty(capabilities)) {
            this.capabilities.addAll(capabilities);
        }
    }

    public void setContributions(List<CapabilityDefinitionExtensionReference> contributions) {
        this.contributions.clear();

        if (!CollectionUtils.isEmpty(contributions)) {
            this.contributions.addAll(contributions);
        }
    }

    public void setRequisitions(List<CapabilityDefinitionExtensionReference> requisitions) {
        this.requisitions.clear();

        if (!CollectionUtils.isEmpty(requisitions)) {
            this.requisitions.addAll(requisitions);
        }
    }

    public void setProvisions(List<Provision> provisions) {
        this.provisions.clear();

        if (!CollectionUtils.isEmpty(provisions)) {
            this.provisions.addAll(provisions);
        }
    }

    public List<CapabilityDefinitionReference> getCapabilities() {
        return Collections.unmodifiableList(capabilities);
    }

    public List<CapabilityDefinitionExtensionReference> getContributions() {
        return Collections.unmodifiableList(contributions);
    }

    public List<CapabilityDefinitionExtensionReference> getRequisitions() {
        return Collections.unmodifiableList(requisitions);
    }

    public List<Provision> getProvisions() {
        return Collections.unmodifiableList(provisions);
    }

    public Id<PackageId> getPackageId() {
        return packageId;
    }

}
