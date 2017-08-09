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

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;

/**
 * @author Alexander D.
 */
public class RequiredCapability extends Capability {

    private boolean optional;

    private boolean multipleBindingsAllowed = false;

    private boolean identifiesHost = false;

    public RequiredCapability(Id<CapabilityId> id, CapabilityDefinition capabilityDefinition,
            ConfigurationUnit unit) {
        super(id, capabilityDefinition, unit);
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public void setMultipleBindingsAllowed(boolean multipleBindingsAllowed) {
        this.multipleBindingsAllowed = multipleBindingsAllowed;
    }

    public boolean isMultipleBindingsAllowed() {
        return multipleBindingsAllowed;
    }

    public boolean identifiesHost() {
        return identifiesHost;
    }

    public void setIdentifiesHost(boolean identifiesHost) {
        this.identifiesHost = identifiesHost;
    }

}
