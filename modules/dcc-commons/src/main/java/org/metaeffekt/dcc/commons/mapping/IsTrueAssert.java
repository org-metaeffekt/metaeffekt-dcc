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

import org.apache.commons.lang3.Validate;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;

public class IsTrueAssert extends AbstractUnitAssert implements ProfileEvaluatedAssert {

    private final String value;

    private final String message;

    public IsTrueAssert(String value, String message) {
        Validate.notNull(value);

        this.value = value;
        this.message = message;
    }

    public String getValue() {
        return value;
    }

    public String getMessage() {
        return message;
    }

    public Boolean evaluate(PropertiesHolder propertiesHolder, Profile profile) {
        if (getUnit() != null) {
            return new UnitScopePropertyExpression(propertiesHolder, getUnit(), true)
                    .evaluate(value, Boolean.class);
        } else {
            return new GlobalPropertyExpression(propertiesHolder, profile)
                    .evaluate(value, Boolean.class);
        }
    }

    @Override
    public String toString() {
        final Id<UnitId> unitId = getUnit() == null ? null : getUnit().getId();
        return "IsTrue [value=" + value + ", unit=" + unitId + "]";
    }

}
