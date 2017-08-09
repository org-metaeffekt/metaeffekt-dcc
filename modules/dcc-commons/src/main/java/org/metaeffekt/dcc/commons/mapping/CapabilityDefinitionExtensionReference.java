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

public class CapabilityDefinitionExtensionReference extends CapabilityDefinitionReference {

    private String boundToCapabilityId;

    public CapabilityDefinitionExtensionReference(String referencedCapabilityDefId, String prefix,
            String boundToCapabilityId) {
        super(referencedCapabilityDefId, prefix);

        this.boundToCapabilityId = boundToCapabilityId;
    }

    public CapabilityDefinitionExtensionReference(String referencedCapabilityDefId,
            String boundToCapabilityRef) {
        this(referencedCapabilityDefId, null, boundToCapabilityRef);
    }

    public CapabilityDefinitionExtensionReference() {
        super();
    }

    public String getBoundToCapabilityId() {
        return boundToCapabilityId;
    }

    public void setBoundToCapabilityId(String boundToCapabilityId) {
        this.boundToCapabilityId = boundToCapabilityId;
    }

}
