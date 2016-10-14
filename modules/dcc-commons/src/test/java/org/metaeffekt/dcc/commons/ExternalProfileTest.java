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
package org.metaeffekt.dcc.commons;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.spring.xml.ProfileParser;

@Ignore
public class ExternalProfileTest {

    @Test
    public void testCoreLdap() {
        Profile p = ProfileParser.parse(new File("C:/dev/workspace/ehf-trunk/packages/ehf-ldap-core/target/dcc/test-deployment-profile.xml"));
        p.evaluate(new PropertiesHolder());
    }

    @Test
    public void testPd() {
        Profile p = ProfileParser.parse(new File("C:/dev/workspace/hpd-installer-trunk/packages/pd-solution/target/dcc/pd-standard/pd-deployment-profile.xml"));
        final PropertiesHolder propertiesHolder = new PropertiesHolder();
        p.evaluate(propertiesHolder);
        
        ConfigurationUnit unit = p.findUnit(Id.createUnitId("jmx-identity-provider"));
        String value = propertiesHolder.getProperty(unit, "userProvider");
        
        System.out.println(value);
    }

}
