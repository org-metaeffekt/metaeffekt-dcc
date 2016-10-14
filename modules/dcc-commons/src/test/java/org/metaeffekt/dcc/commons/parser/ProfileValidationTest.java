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
package org.metaeffekt.dcc.commons.parser;


import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.spring.xml.ProfileParser;

public class ProfileValidationTest {

    @Ignore
    @Test
    public void testPcm() throws IOException {
        final File baseFolder = new File("C:/dev/workspace/ancona-trunk/deliverable/packages/pcm-solution/target/dcc/pcm-standard");
        final File testProfileFile = new File(baseFolder, "pcm-deployment-profile.xml");
        
        Profile profile = ProfileParser.parse(testProfileFile);
        
        PropertiesHolder propertiesHolder = profile.createPropertiesHolder(true);
        profile.evaluate(propertiesHolder);
        propertiesHolder.dump();
    }

    @Ignore
    @Test
    public void testPd() throws IOException {
        final File baseFolder = new File("C:/dev/workspace/hpd-installer-trunk/packages/pd-solution/target/dcc");
        final File testProfileFile = new File(baseFolder, "pd-standard/pd-deployment-profile.xml");
        Profile profile = ProfileParser.parse(testProfileFile);
        PropertiesHolder propertiesHolder = profile.createPropertiesHolder(true);
        profile.evaluate(propertiesHolder);
        propertiesHolder.dump();
        
        Assert.assertEquals("1636", propertiesHolder.getProperties("idm-war/ldap.config").getProperty("port"));
        Assert.assertEquals("ldaps", propertiesHolder.getProperties("idm-war/ldap.config").getProperty("scheme"));

        Assert.assertEquals("2.16.840.1.113883.3.37.4.1.9.204", propertiesHolder.getProperties("tokenservice-bundle/properties").getProperty("sts.tokenfilter.xua.group.codesystem.oid"));
        Assert.assertEquals("2.16.840.1.113883.3.37.4.1.9.204", propertiesHolder.getProperties("tokenservice-bundle/properties").getProperty("sts.tokenservice.xua.group.codesystem.oid"));

    }

}
