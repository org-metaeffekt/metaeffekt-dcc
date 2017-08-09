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
package org.metaeffekt.dcc.commons.ant;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;


public class UrlUtilsTest {

    @Test
    public void test() throws IOException {
        final UrlUtils urlUtils = new UrlUtils();
        Assert.assertEquals("x.txt", urlUtils.asRelativePath("test", "test/x.txt"));
        Assert.assertEquals("file:x.txt", urlUtils.asRelativeUrl("/test", "/test/x.txt"));
        Assert.assertEquals("../../../data/james-test/mail/relay-denied", urlUtils.asRelativePath(
            "/dev/workspace/dcc-application-trunk/packages/dcc-james-3/target/opt/app/tomcat-runtime-test/apache-tomcat-7.0.67",
            "/dev/workspace/dcc-application-trunk/packages/dcc-james-3/target/opt/data/james-test/mail/relay-denied"));
        
        Assert.assertEquals("mmc-standard/mmc-solution.properties", 
            urlUtils.asRelativePath(
                "/dev/workspace/mmc-application-trunk/packages/mmc-reference-solution/target/dcc", 
                "/dev/workspace/mmc-application-trunk/packages/mmc-reference-solution/target/dcc/mmc-standard/mmc-solution.properties"));

    }

}
