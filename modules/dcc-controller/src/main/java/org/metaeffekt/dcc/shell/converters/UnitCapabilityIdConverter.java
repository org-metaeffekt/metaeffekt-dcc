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
package org.metaeffekt.dcc.shell.converters;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.stereotype.Component;

import org.metaeffekt.dcc.commons.mapping.CapabilityDefinition;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;

/**
 * @author Douglas B.
 */
@Component
public class UnitCapabilityIdConverter implements Converter<String> {

    public static final String UNIT_IDS_COMPLETION = "unit-ids-completion";
    public static final String CAPABILITY_DEFINITION_IDS_COMPLETION = "capabilityDefinition-ids-completion";
    public static final String DISABLE_STRING_CONVERTER = "disable-string-converter";
    public static final String UNIT_CAPABILITY_ID_CONVERTER = "unit-capability-id-converter";
    public static final String USE_UNIT_ID_COMPLETION = UNIT_CAPABILITY_ID_CONVERTER + ","
            + UNIT_IDS_COMPLETION + "," + DISABLE_STRING_CONVERTER;
    public static final String USE_CAPABILITY_DEFINITION_ID_COMPLETION = UNIT_CAPABILITY_ID_CONVERTER + ","
            + CAPABILITY_DEFINITION_IDS_COMPLETION + "," + DISABLE_STRING_CONVERTER;
    
    private final ExecutionContext executionContext;

    @Autowired
    public UnitCapabilityIdConverter(ExecutionContext executionContext) {
        super();
        this.executionContext = executionContext;
    }


    @Override
    public boolean supports(Class<?> targetType, String optionContext) {
        return String.class.isAssignableFrom(targetType) && optionContext != null 
                && optionContext.contains(DISABLE_STRING_CONVERTER) 
                && optionContext.contains(UNIT_CAPABILITY_ID_CONVERTER);
    }

    @Override
    public String convertFromText(String value, Class<?> targetType, String optionContext) {
        return value;
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType,
            String existingData, String optionContext, MethodTarget target) {
        
        Profile profile = executionContext.getProfile();
        if (profile != null) {
            if (optionContext.contains(UNIT_IDS_COMPLETION)) {
                List<ConfigurationUnit> units = profile.getUnits();
                for (ConfigurationUnit configurationUnit : units) {
                    completions.add(new Completion(configurationUnit.getId().getValue()));
                }
            } else if (optionContext.contains(CAPABILITY_DEFINITION_IDS_COMPLETION)) {
                List<CapabilityDefinition> definitions = profile.getCapabilityDefinitions();
                for (CapabilityDefinition def : definitions) {
                    completions.add(new Completion(def.getId()));
                }
            }
        }
        
        return false;
    }

}
