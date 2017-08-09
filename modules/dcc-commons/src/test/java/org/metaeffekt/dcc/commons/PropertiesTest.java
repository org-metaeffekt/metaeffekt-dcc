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
package org.metaeffekt.dcc.commons;

import java.io.File;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import org.metaeffekt.dcc.commons.ant.PropertyUtils;


public class PropertiesTest {

    @Test
    public void test() {
        Properties p = PropertyUtils.loadPropertyFile(new File("src/test/resources/sample.properties"));
        
        Assert.assertEquals("1", p.getProperty("test"));
        Assert.assertEquals("2", p.getProperty("test/test"));
        Assert.assertEquals("3", p.getProperty("test/test.test"));
        
        // important: if a property is empty
        Assert.assertEquals("", p.getProperty("empty"));
        Assert.assertEquals(null, p.getProperty("not.included"));
    }

}
