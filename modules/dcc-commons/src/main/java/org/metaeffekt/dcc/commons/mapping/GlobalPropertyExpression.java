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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.lang3.tuple.ImmutablePair;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;

/**
 * Supports the following syntax ${unit-id[capability-id[key]]} or ${unit-id[key]}.
 */
public class GlobalPropertyExpression extends AbstractPropertyExpression {

    private final Profile profile;

    public GlobalPropertyExpression(PropertiesHolder propertiesHolder, Profile profile) {
        super(propertiesHolder, true);

        this.profile = profile;
    }

    @Override
    protected ConfigurationUnit findUnit(Id<UnitId> unitId) {
        return profile.findUnit(unitId);
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
        String key = null;
        String capabilityId = null;
        String unitId = token;
        if (isArraySyntax(token)) {
            final ImmutablePair<String, String> unitPair = parseTokenWithSquareBrackets(token);
            unitId = unitPair.left;
            key = unitPair.right;

            if (isArraySyntax(unitPair.right)) {
                final ImmutablePair<String, String> capabilityKeyPair =
                    parseTokenWithSquareBrackets(unitPair.right);
                capabilityId = capabilityKeyPair.left;
                key = capabilityKeyPair.right;
            }
        }

        return new ParsedItem(Id.createUnitId(unitId), Id.createCapabilityId(capabilityId), key);
    }

    private boolean isArraySyntax(String token) {
        return token.indexOf("[") != -1 && token.charAt(token.length() - 1) == ']';
    }

    private ImmutablePair<String, String> parseTokenWithSquareBrackets(String token) {
        String left = token;
        String right = null;
        final int index = token.indexOf("[");
        if (index != -1 && token.charAt(token.length() - 1) == ']') {
            left = token.substring(0, index);
            right = token.substring(index + 1, token.length() - 1);
        }

        return new ImmutablePair<String, String>(left, right);
    }

}
