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
package org.metaeffekt.dcc.commons.dependency;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;

/**
 * Represents an empty set of dependencies.
 * 
 * @author Alexander D.
 */
public final class NoDependencies extends UnitDependencies {

    NoDependencies() {
        super(null, null, null, null);
    }


    @Override
    public Map<Id<UnitId>, List<Id<UnitId>>> getUpstreamMatrix() {
        return Collections.emptyMap();
    }

    @Override
    public Map<Id<UnitId>, List<Id<UnitId>>> getDownstreamMatrix() {
        return Collections.emptyMap();
    }

    @Override
    public void sortUpstream(List<ConfigurationUnit> units) {}

    @Override
    public void sortDownstream(List<ConfigurationUnit> units) {}

    @Override
    public void sortIdsUpstream(List<Id<UnitId>> unitIds) {}

    @Override
    public void sortIdsDownstream(List<Id<UnitId>> unitIds) {}
}
