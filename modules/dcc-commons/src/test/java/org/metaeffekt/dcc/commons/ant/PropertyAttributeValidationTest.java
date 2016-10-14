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
package org.metaeffekt.dcc.commons.ant;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.spring.xml.ProfileParser;


public class PropertyAttributeValidationTest {

    @Test
    public void test() throws IOException {
        
        Profile profile = ProfileParser.parse(new File("src/test/resources/upgrade/001/source/xyz-deployment-profile.xml"));
        PropertiesHolder propertiesHolder = profile.createPropertiesHolder(true);
        
        profile.evaluate(propertiesHolder);
        
        Assert.assertTrue(propertiesHolder.isPropertyRelevant("xyz.f"));
        Assert.assertTrue(!propertiesHolder.isPropertyRelevant("xyz.unusedAttribute"));
        
        Assert.assertTrue(!propertiesHolder.isPropertyRelevant("xyz.notInProfile"));
        Assert.assertTrue(!propertiesHolder.isPropertyRelevant("test.special.chars"));
        
    }

}
