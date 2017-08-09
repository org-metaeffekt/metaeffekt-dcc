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

public class UniqueAssert extends AbstractUnitAssert implements ProfileEvaluatedAssert {

    private final String value;

    private final String message;

    private final String isEnabledCondition;

    public UniqueAssert(String value, String isEnabledCondition, String message) {
        Validate.notNull(value);

        this.value = value;
        this.isEnabledCondition = isEnabledCondition;
        this.message = message;
    }

    public String getValue() {
        return value;
    }

    public String getMessage() {
        return message;
    }

    public String evaluate(PropertiesHolder propertiesHolder, Profile profile) {
        if (!isEnabled(propertiesHolder, profile)) {
            return null;
        }

        return evaluateProperties(profile, propertiesHolder, value, String.class);
    }

    protected <T> T evaluateProperties(Profile profile, PropertiesHolder propertiesHolder,
            String value, Class<T> type) {
        if (getUnit() != null) {
            return new UnitScopePropertyExpression(propertiesHolder, getUnit(), true)
                    .evaluate(value, type);
        } else {
            return new GlobalPropertyExpression(propertiesHolder, profile)
                    .evaluate(value, type);
        }
    }

    protected boolean isEnabled(PropertiesHolder propertiesHolder, Profile profile) {
        if (Boolean.parseBoolean(isEnabledCondition)) {
            return true;
        }

        final Boolean isEnabled =
            evaluateProperties(profile, propertiesHolder, isEnabledCondition, Boolean.class);

        return Boolean.TRUE.equals(isEnabled);
    }

    @Override
    public String toString() {
        final Id<UnitId> unitId = getUnit() == null ? null : getUnit().getId();
        return "UniqueAssert [value=" + value + ", unit=" + unitId + "]";
    }

}
