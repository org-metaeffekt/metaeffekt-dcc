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
package org.metaeffekt.dcc.commons.dependency;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.exception.CyclicBindingException;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.spring.xml.BindingFactoryBean;

public class UnitDependencyGraphCalculator {

    private Collection<BindingFactoryBean> allBindingFactories;

    public UnitDependencyGraphCalculator(Collection<BindingFactoryBean> allBindingFactories) {
        this.allBindingFactories = new HashSet<BindingFactoryBean>(allBindingFactories);
    }
    
    public UnitDependencies calculate() {
        UnitDependencies unitDependencies = new UnitDependencies();
        for (BindingFactoryBean binding : allBindingFactories) {
            ConfigurationUnit sourceUnit = binding.getSourceUnit();
            ConfigurationUnit targetUnit = binding.getTargetUnit();
            unitDependencies.addBinding(sourceUnit.getId(), binding.getSourceCapabilityId(),
                    targetUnit.getId(), binding.getTargetCapabilityId());
        }
        unitDependencies.resolveTransitiveDependencies();
        return unitDependencies;
    }

}
