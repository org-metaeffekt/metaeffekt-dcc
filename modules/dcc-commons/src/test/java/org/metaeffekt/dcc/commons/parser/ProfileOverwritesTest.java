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
package org.metaeffekt.dcc.commons.parser;


import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.spring.xml.ProfileParser;

public class ProfileOverwritesTest {

    final File baseFolder = new File("src/test/resources/parser-test/profile-overwrites");

    private File testProfileFile = new File(baseFolder, "overwrite-profile.xml");
    private Profile profile = ProfileParser.parse(testProfileFile);
    
    @Test
    public void test() throws IOException {
        PropertiesHolder propertiesHolder = profile.createPropertiesHolder(true);
        profile.evaluate(propertiesHolder);
        propertiesHolder.dump();
        
        ConfigurationUnit unit = profile.findUnit(Id.createUnitId("unit1"));
        Assert.assertEquals(4, unit.getAttributes().size());

        final String attribute1 = getAttributeAtIndex(unit, 0);
        Assert.assertEquals("key1", attribute1);
        Assert.assertEquals(null, getAttributeValue(unit, attribute1, propertiesHolder));

        final String attribute2 = getAttributeAtIndex(unit, 1);
        Assert.assertEquals("key2", attribute2);
        Assert.assertEquals("overwrite", getAttributeValue(unit, attribute2, propertiesHolder));

        final String attribute3 = getAttributeAtIndex(unit, 2);
        Assert.assertEquals("key3", attribute3);
        Assert.assertEquals("add", getAttributeValue(unit, attribute3, propertiesHolder));
        
        final String attribute4 = getAttributeAtIndex(unit, 3);
        Assert.assertEquals("key4", attribute4);
        Assert.assertEquals("fromFile", getAttributeValue(unit, attribute4, propertiesHolder));
    }

    public String getAttributeAtIndex(ConfigurationUnit unit, final int index) {
        return unit.getAttributes().get(index).getKey();
    }

    public String getAttributeValue(ConfigurationUnit unit, String key, PropertiesHolder propertiesHolder) {
        return propertiesHolder.getProperty(unit, key);
    }
    
    @Test
    public void testOverwriteCapabilities() {
        ConfigurationUnit unit = profile.findUnit(Id.createUnitId("unit1"));
        Assert.assertEquals(1, unit.getProvidedCapabilities().size());
        Assert.assertEquals(1, unit.getRequiredCapabilities().size());
    }

}
