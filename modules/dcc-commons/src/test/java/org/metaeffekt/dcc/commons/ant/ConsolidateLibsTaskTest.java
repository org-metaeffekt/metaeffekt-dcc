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

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.Project;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.common.kernel.ant.log.LoggingProjectAdapter;

public class ConsolidateLibsTaskTest {

    @Test
    public void test() throws IOException {
        Assert.assertTrue(isNewer("1.0.0", "1.1.0"));
        Assert.assertTrue(isNewer("1.0.0", "1.0.1"));
        Assert.assertTrue(isNewer("1.0rc1", "1.0.0"));
        Assert.assertTrue(isNewer("1.0_rc1", "1.0.0"));
        Assert.assertTrue(isNewer("1.0-rc1", "1.0.0"));
        Assert.assertTrue(isNewer("1.0_M1", "1.0.0"));
        Assert.assertTrue(isNewer("1.0-M1", "1.0.0"));
        Assert.assertTrue(isNewer("1.0_20160101", "1.0.0"));
        Assert.assertTrue(isNewer("1.0.0", "0.1.0-SNAPSHOT"));
        Assert.assertTrue(isNewer("1.0.0", "1-SNAPSHOT"));
        Assert.assertTrue(isNewer("1.45.0", "1-SNAPSHOT"));
        Assert.assertTrue(isNewer("1.0.0", "1.0-SNAPSHOT"));
        Assert.assertTrue(isNewer("STABLE-SNAPSHOT", "0.1.0-SNAPSHOT"));
        Assert.assertTrue(isNewer("1.0.0_45", "1.1.0"));
        Assert.assertTrue(isNewer("1.45.0", "10.45.0"));
        
        Assert.assertFalse(isNewer("1.45.0", "1.45.0"));
    }

    @Test
    public void testMetaData() throws IOException {
        Assert.assertEquals(null, LibUtils.extractFilePattern(""));
        Assert.assertEquals(null, LibUtils.extractFilePattern("The following files have been resolved:"));

        Assert.assertEquals("ae-5.2-SNAPSHOT-runtime.jar", LibUtils.extractFile("org.metaeffekt:ae:jar:runtime:5.2-SNAPSHOT"));
        Assert.assertEquals("jsr305-3.0.1.jar", LibUtils.extractFile("com.google.code.findbugs:jsr305:jar:3.0.1"));

        Assert.assertEquals("ae-commons-security-*-runtime.jar", LibUtils.extractFilePattern("org.metaeffekt:ae-commons-security:jar:runtime:5.2-SNAPSHOT"));
        Assert.assertEquals("jsr305-*.jar", LibUtils.extractFilePattern("com.google.code.findbugs:jsr305:jar:3.0.1"));

        Assert.assertEquals("5.2-SNAPSHOT", LibUtils.extractVersion("org.metaefekt:ae-commons-security:jar:runtime:5.2-SNAPSHOT"));
        Assert.assertEquals("3.0.1", LibUtils.extractVersion("com.google.code.findbugs:jsr305:jar:3.0.1"));
    }
    
    @Ignore
    @Test
    public void testTask_IDM() {
        ConsolidateLibsTask mergeLibsTask = new ConsolidateLibsTask();
        mergeLibsTask.setProject(new LoggingProjectAdapter());
        File baseDir = new File("C:/dev/workspace/ehi-system-integration-trunk/solutions/idm-reference/target/opt/idm/config/idm-war");
        mergeLibsTask.setMetaDataDir(baseDir);
        mergeLibsTask.setLibDir(new File(baseDir, "tmp/artifacts/ehi-integration-template.war/WEB-INF/lib"));
        mergeLibsTask.execute();

    }

    @Ignore
    @Test
    public void testTask_MMC() {
        ConsolidateLibsTask mergeLibsTask = new ConsolidateLibsTask();
        mergeLibsTask.setProject(new Project());
        mergeLibsTask.setMetaDataDir(new File("C:/dev/workspace/mmc-application-trunk/packages/mmc-reference-solution/target/opt/mmc-application/config/mmc-provider-management-war"));
        mergeLibsTask.setLibDir(new File("C:/dev/workspace/mmc-application-trunk/packages/mmc-reference-solution/target/opt/mmc-application/config/mmc-provider-management-war/tmp/artifacts/ehi-integration-template.war/WEB-INF/lib"));
        mergeLibsTask.execute();

    }

    protected boolean isNewer(String referenceVersion, String version) {
        return ConsolidateLibsTask.canonicalizeVersion(referenceVersion).
            compareTo(ConsolidateLibsTask.canonicalizeVersion(version)) < 0;
    }

}
