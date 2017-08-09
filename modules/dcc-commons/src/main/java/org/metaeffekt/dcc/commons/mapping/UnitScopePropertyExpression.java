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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.lang3.Validate;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;

/**
 * Supports the following syntax ${capability-id[key]} or ${key}.
 */
public class UnitScopePropertyExpression extends AbstractPropertyExpression {

    private final ConfigurationUnit unit;

    public UnitScopePropertyExpression(PropertiesHolder propertiesHolder, ConfigurationUnit unit) {
        this(propertiesHolder, unit, false);
    }

    public UnitScopePropertyExpression(PropertiesHolder propertiesHolder, ConfigurationUnit unit,
            boolean resolveProvidedCapabilities) {
        super(propertiesHolder, resolveProvidedCapabilities);

        Validate.notNull(unit);
        this.unit = unit;
    }

    @Override
    protected ConfigurationUnit findUnit(Id<UnitId> unitId) {
        return unit;
    }

    @Override
    protected Map<String, ParsedItem> parse(String expression) {
        final Map<String, ParsedItem> items = new LinkedHashMap<String, ParsedItem>();

        final Matcher matcher = REG_EX.matcher(expression);
        while (matcher.find()) {
            final String token = matcher.group();
            final String strippedToken = token.substring(2, token.length() - 1);
            items.put(token, parseSingleToken(strippedToken));
        }

        return items;
    }

    private ParsedItem parseSingleToken(String token) {
        String key = token;
        String capabilityId = null;
        final int index = token.indexOf("[");
        if (index != -1 && token.charAt(token.length() - 1) == ']') {
            capabilityId = token.substring(0, index);
            key = token.substring(index + 1, token.length() - 1);
        }

        return new ParsedItem(unit.getId(), Id.createCapabilityId(capabilityId), key);
    }

}
