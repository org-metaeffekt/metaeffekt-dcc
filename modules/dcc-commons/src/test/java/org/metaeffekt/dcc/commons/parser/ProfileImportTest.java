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


import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.spring.xml.ProfileParser;

import java.io.File;
import java.io.IOException;

public class ProfileImportTest {

    final File baseFolder = new File("src/test/resources/parser-test/import-feature");

    private File testProfileFile = new File(baseFolder, "test-deployment-profile.xml");
    private Profile profile = ProfileParser.parse(testProfileFile);
    
    @Test
    public void test() throws IOException {
        PropertiesHolder propertiesHolder = profile.createPropertiesHolder(true);
        profile.evaluate(propertiesHolder);
        propertiesHolder.dump();

        ConfigurationUnit unit001 = profile.findUnit(Id.createUnitId("unit-001"));

        Assert.assertFalse(unit001.getDescription().contains("${var}"));
    }

}
