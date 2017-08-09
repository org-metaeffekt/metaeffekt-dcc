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

public class PrerequisiteAssert extends AbstractUnitAssert implements UnitEvaluatedAssert {

    private final String key;

    private final String expression;

    public PrerequisiteAssert(String key, String expression) {
        this.key = key;
        this.expression = expression;
    }

    public String getKey() {
        return key;
    }

    public String getExpression() {
        return expression;
    }

    public String evaluate(PropertiesHolder propertiesHolder, Profile profile) {
        return evaluateProperties(profile, propertiesHolder, expression);
    }

    protected String evaluateProperties(Profile profile, PropertiesHolder propertiesHolder,
            String value) {
        if (getUnit() != null) {
            return new UnitScopePropertyExpression(propertiesHolder, getUnit(), true)
                    .evaluate(value);
        } else {
            return new GlobalPropertyExpression(propertiesHolder, profile)
                    .evaluate(value);
        }
    }

}
