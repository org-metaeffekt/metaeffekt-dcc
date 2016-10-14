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

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;


public class UpgradePropertiesTaskTest {

    @Test
    public void testPropertyAndAttributeValidation() throws IOException {
        final File targetFile = new File("target/test/upgrade/xyz-deployment.properties");

        UpgradePropertiesTask task = new UpgradePropertiesTask();
        task.setProject(new Project());
        task.setSourceProfileFile(new File("src/test/resources/upgrade/001/source/xyz-deployment-profile.xml"));
        task.setTargetProfileFile(new File("src/test/resources/upgrade/001/target/xyz-deployment-profile.xml"));
        task.setPropertiesTemplateFile(new File("src/test/resources/upgrade/001/xyz-deployment.properties.vt"));
        task.setTargetPropertiesFile(targetFile);
        task.execute();
        
        Assert.assertTrue(targetFile.exists());
        Assert.assertTrue(!FileUtils.readFileToString(targetFile).contains("$"));
    }
    
    @Test 
    public void testUpgradeXYZ001() throws IOException {
        Project project = new Project();
        Copy copy = new Copy();
        copy.setProject(project);
        copy.setOverwrite(true);
        FileSet fileSet = new FileSet();
        fileSet.setDir(new File("src/test/resources/upgrade/001"));
        copy.addFileset(fileSet);
        copy.setTodir(new File("target/tmp/upgrade/001"));
        copy.execute();
        
        UpgradePropertiesTask.apply(project, new File("target/tmp/upgrade/001/target/xyz-upgrade.properties"));
        
        final File targetFile = new File("target/tmp/upgrade/001/target/xyz-deployment.properties");
        Assert.assertTrue(targetFile.exists());
        Assert.assertTrue(!FileUtils.readFileToString(targetFile).contains("$"));
        
        assertEquals(
                new File("target/tmp/upgrade/001/target/xyz-deployment.properties"), 
                new File("src/test/resources/upgrade/001/expected/xyz-deployment.properties"));
        
        assertEquals(
                new File("target/tmp/upgrade/001/target/xyz-solution.properties"), 
                new File("src/test/resources/upgrade/001/expected/xyz-solution.properties"));
    }
    
    @Test
    @Ignore
    public void testUpgradeProcedurePcm() throws IOException {
        File projectPath = new File("C:/dev/workspace/ancona-trunk/deliverable/packages/pcm-solution/");
        FileUtils.copyFile(new File(projectPath, "src/test/resources/pcm-standard/pcm-deployment.properties"), new File(projectPath, "target/dcc/pcm-standard/pcm-deployment.properties"));
        UpgradePropertiesTask.apply(new Project(), new File(projectPath, "target/dcc/pcm-standard/pcm-upgrade.properties"));
        
        final File targetFile = new File(projectPath, "target/dcc/pcm-standard/pcm-deployment.properties");
        Assert.assertTrue(targetFile.exists());
        Assert.assertTrue(!FileUtils.readFileToString(targetFile).contains("$"));
    }
    
    @Test
    @Ignore
    public void testUpgradeProcedurePd002() throws IOException {
        File projectPath = new File("C:/dev/workspace/hpd-installer-trunk/deliverable/packages/pd-solution/");
        UpgradePropertiesTask.apply(new Project(), new File(projectPath, "target/dcc/pd-standard/upgrade/pd-upgrade.properties"));
        
        assertEquals(
                new File(projectPath, "target/dcc/pd-standard/pd-deployment.properties"), 
                new File("src/test/resources/upgrade/002/expected/deployment.properties"));

        assertEquals(
                new File(projectPath, "target/dcc/pd-standard/pd-solution.properties"), 
                new File("src/test/resources/upgrade/002/expected/solution.properties"));

    }

    private void assertEquals(File actual, File expected) throws IOException {
        String actualFileContent = FileUtils.readFileToString(actual);
        String expectedFileContent = FileUtils.readFileToString(expected);
        
        // this test has issues with whitespaces on linux 
        String lineSeparator = "(\\r\\n?|\\n){2,}";
        String systemLineSeparator = System.getProperty("line.separator");
        String actualFileContentCondensed = actualFileContent.replaceAll(lineSeparator, systemLineSeparator);
        String expectedFileContentCondensed = expectedFileContent.replaceAll(lineSeparator, systemLineSeparator);
        
        if (!expectedFileContentCondensed.equals(actualFileContentCondensed)) {
            // only if the condensed version is not equal we assert on the content (better IDE integration, maintenance)
            Assert.assertEquals(expectedFileContent, actualFileContent);
        }
        Assert.assertFalse(actualFileContent.contains("MUST NOT APPEAR"));
    }

}
