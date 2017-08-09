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

import org.metaeffekt.dcc.commons.DccUtils;

/**
 * Concrete implementation of the {@link AttributeMapper} interface using expressions to derive
 * a single attribute value.
 * 
 * @author Karsten Klein
 */
public class ExpressionAttributeMapper extends AbstractSingleAttributeMapper {

    private final String expression;

    public ExpressionAttributeMapper(String attributeKey, String expression) {
        super(attributeKey);
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    public void evaluate(PropertiesHolder propertiesHolder, Capability targetCapability, Profile profile) {
        final ConfigurationUnit boundUnit = targetCapability.getUnit();
        final UnitScopePropertyExpression expressionEval = new UnitScopePropertyExpression(propertiesHolder, boundUnit);

        // FIXME: refactoring required. Implement strategy to handle overwrites centrally.
        final String attributeKey = getAttributeKey();
        
        // check whether overwrite exists
        final String globalKey = DccUtils.deriveAttributeIdentifier(targetCapability, attributeKey);
        String expression = propertiesHolder.getBaseProperty(globalKey, null);

        // otherwise use the expression value
        if (expression == null) {
            expression = getExpression();
        }
        
        final String expressionvalue = expressionEval.evaluate(expression);
        propertiesHolder.setProperty(targetCapability, attributeKey, expressionvalue);
        
        // NOTE: please note that defaults are not executed on this level. If an expression evaluates to null. 
        //   this result will take priority over the default. Currently the default however is not accessible
        //   from the expression. This was however not yet required in any scenario.
    }

}
