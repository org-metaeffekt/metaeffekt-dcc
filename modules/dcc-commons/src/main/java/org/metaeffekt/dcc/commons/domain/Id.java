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
package org.metaeffekt.dcc.commons.domain;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;
import org.metaeffekt.dcc.commons.domain.Type.HostName;
import org.metaeffekt.dcc.commons.domain.Type.PackageId;
import org.metaeffekt.dcc.commons.domain.Type.ProfileId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.core.commons.annotation.Public;

/**
 * @author Alexander D.
 * @author Jochen K.
 */
@Public
public class Id<T extends Type> implements Comparable<Id<T>> {

    private final String value;

    private final Class<T> type;

    public Id(String value, Class<T> type) {
        Validate.notEmpty(value, "The value of an ID must not be empty!");
        Validate.notNull(type);

        this.value = value;
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public Class<T> getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Id))
            return false;

        @SuppressWarnings("unchecked")
        Id<T> other = (Id<T>) o;

        return StringUtils.equals(value, other.getValue());
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int compareTo(Id<T> o) {
        if (o == null) {
            return -1;
        }
        return value.compareTo(o.getValue());
    }

    public static final Id<UnitId> createUnitId(String value) {
        if (value == null)
            return null;
        return new Id<>(value, UnitId.class);
    }

    public static final Id<ProfileId> createProfileId(String value) {
        if (value == null)
            return null;
        return new Id<>(value, ProfileId.class);
    }

    public static final Id<DeploymentId> createDeploymentId(String value) {
        if (value == null)
            return null;
        return new Id<>(value, DeploymentId.class);
    }

    public static final Id<HostName> createHostName(String value) {
        if (value == null)
            return null;
        return new Id<>(value, HostName.class);
    }

    public static final Id<PackageId> createPackageId(String value) {
        if (value == null)
            return null;
        return new Id<>(value, PackageId.class);
    }

    public static final Id<CapabilityId> createCapabilityId(String value) {
        if (value == null)
            return null;
        return new Id<>(value, CapabilityId.class);
    }

}
