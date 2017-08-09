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
package org.metaeffekt.dcc.commons.mapping;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

public class Provision {

    private final CapabilityDefinitionReference capabilityRef;

    private final List<ProvisionRestriction> restrictions = new ArrayList<>();

    public Provision(CapabilityDefinitionReference capabilityRef,
            List<ProvisionRestriction> restrictions) {
        Validate.notNull(capabilityRef);
        this.capabilityRef = capabilityRef;

        if (restrictions != null) {
            this.restrictions.addAll(restrictions);
        }
    }

    public CapabilityDefinitionReference getCapabilityRef() {
        return capabilityRef;
    }

    public List<ProvisionRestriction> getRestrictions() {
        return restrictions;
    }

}
