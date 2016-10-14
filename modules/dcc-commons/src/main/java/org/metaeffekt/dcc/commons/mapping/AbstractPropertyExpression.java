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

import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.EnvironmentAccessor;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;

public abstract class AbstractPropertyExpression {

    private static final StandardEnvironment STANDARD_ENVIRONMENT = new StandardEnvironment();

    private static final Logger LOG = LoggerFactory.getLogger(AbstractPropertyExpression.class);

    protected final static Pattern REG_EX = Pattern.compile("\\$\\{([^\\}]*)\\}");

    private final PropertiesHolder propertiesHolder;

    private final boolean resolveProvidedCapabilities;

    public AbstractPropertyExpression(PropertiesHolder propertiesHolder,
            boolean resolveProvidedCapabilities) {

        Validate.notNull(propertiesHolder);
        this.propertiesHolder = propertiesHolder;
        this.resolveProvidedCapabilities = resolveProvidedCapabilities;
    }

    public String evaluate(String expression) {
        return evaluate(expression, String.class);
    }

    public <T> T evaluate(String expression, Class<T> type) {
        String value = expression;

        final Map<String, ParsedItem> itemsToBeEvaluated = parse(expression);
        for (Map.Entry<String, ParsedItem> item : itemsToBeEvaluated.entrySet()) {
            final String evaluatedValue = evaluate(item.getValue());
            if (evaluatedValue != null) {
                value = value.replace(item.getKey(), evaluatedValue);
            }
        }

        return evaluateExpression(expression, value, type);
    }

    protected String evaluate(ParsedItem parsedItem) {
        String value = null;
        final ConfigurationUnit unit = findUnit(parsedItem.unitId);
        if (unit != null && parsedItem.key != null) {
            if (parsedItem.capabilityId != null) {
                Capability capability = unit.findRequiredCapability(parsedItem.capabilityId);
                if (capability != null) {
                    value = propertiesHolder.getProperty(capability, parsedItem.key, false);
                } else if (resolveProvidedCapabilities) {
                    capability = unit.findProvidedCapability(parsedItem.capabilityId);
                    if (capability != null) {
                        value = propertiesHolder.getProperty(capability, parsedItem.key, false);
                    }
                }
            }
            if (value == null) {
                // mark the property taken from the unit as relevant
                value = propertiesHolder.getProperty(unit, parsedItem.key);
                propertiesHolder.markPropertyAsRelevant(unit, parsedItem.key);
            }
        }

        return value;
    }

    protected abstract ConfigurationUnit findUnit(Id<UnitId> unitId);

    protected abstract Map<String, ParsedItem> parse(String expression);

    @SuppressWarnings("unchecked")
    protected <T> T evaluateExpression(String originalExpression, String value, Class<T> type) {
        final ExpressionParser parser = new SpelExpressionParser();

        try {
            final StandardEvaluationContext context = new StandardEvaluationContext();
            context.setRootObject(propertiesHolder);
            context.addPropertyAccessor(new EnvironmentAccessor());
            context.setVariable(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    STANDARD_ENVIRONMENT);
            context.setVariable(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
                    STANDARD_ENVIRONMENT);

            final Expression expression = parser.parseExpression(value, PARSER_CONTEXT);

            return expression.getValue(context, type);
        } catch (ParseException | EvaluationException e) {
            LOG.debug(String.format(
                    "Failed to evaluate expression %s, and with replaced properties %s",
                    originalExpression, value), e);
        }

        if (type == String.class) {
            return (T) value;
        }

        return null;
    }

    protected static class ParsedItem {

        public final Id<UnitId> unitId;

        public final Id<CapabilityId> capabilityId;

        public final String key;

        public ParsedItem(String unitId, String capabilityId, String key) {
            this(Id.createUnitId(unitId), Id.createCapabilityId(capabilityId), key);
        }

        public ParsedItem(Id<UnitId> unitId, Id<CapabilityId> capabilityId, String key) {
            this.unitId = unitId;
            this.capabilityId = capabilityId;
            this.key = key;
        }

    }

    protected static final ParserContext PARSER_CONTEXT = new ParserContext() {

        @Override
        public boolean isTemplate() {
            return true;
        }

        @Override
        public String getExpressionSuffix() {
            return "}";
        }

        @Override
        public String getExpressionPrefix() {
            return "%{";
        }
    };

}
