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

/**
 * Container to preserve the inheritance information for a CapabilityDefinition.
 * The prefix for inherited attributes is optional
 * 
 * @author Jochen K.
 */
public class CapabilityDefinitionReference {

    private String referencedCapabilityDefId;

    private String prefix;

    public CapabilityDefinitionReference(String referencedCapabilityDefId, String prefix) {
        this.referencedCapabilityDefId = referencedCapabilityDefId;
        this.prefix = prefix;
    }

    public CapabilityDefinitionReference(String referencedCapabilityDefId) {
        this(referencedCapabilityDefId, null);
    }

    public CapabilityDefinitionReference() {
    }

    public String getReferencedCapabilityDefId() {
        return referencedCapabilityDefId;
    }

    public void setReferencedCapabilityDefId(String referencedCapabilityDefId) {
        this.referencedCapabilityDefId = referencedCapabilityDefId;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
