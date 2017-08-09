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
import java.util.Properties;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.PropertyFile;
import org.apache.tools.ant.taskdefs.optional.PropertyFile.Entry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.metaeffekt.core.common.kernel.ant.task.LoadPropertiesTask;

public class PropertyEncodingTest {
    
    private Project project;
    
    @Before
    public void setUp() throws IOException {
        project = new Project();
        project.setBaseDir(new File("."));
    }
    
    @Test
    public void execute() throws IOException {
        final File originalFile = new File("src/test/resources/encoding/test.properties");
        final File antTargetFile = new File("target/test/encoding/test-ant.properties");
        final File javaTargetFile = new File("target/test/encoding/test-java.properties");

        
        String testString = loadPropertyFile(originalFile);
        Assert.assertTrue(testString.endsWith("ÿ\u0152\u0153"));
        
        // write the file
        
        // the ant way
        PropertyFile propertyFile = new PropertyFile();
        propertyFile.setProject(project);
        final Entry entry = propertyFile.createEntry();
        entry.setKey("key");
        entry.setValue(testString);
        final File targetFile = antTargetFile;
        targetFile.getParentFile().mkdirs();
        propertyFile.setFile(targetFile);
        propertyFile.execute();
        
        // load the file and compare
        String testStringAfterAntWrite = loadPropertyFile(antTargetFile);
        
        Assert.assertEquals(testString, testStringAfterAntWrite);

        // the conventional java ways
        Properties p = new Properties();
        p.setProperty("key", testString);
        PropertyUtils.writeToFile(p, javaTargetFile, "test");
        
        // load the java-generated file and compare
        String testStringAfterJavaWrite = loadPropertyFile(javaTargetFile);
        
        Assert.assertEquals(testString, testStringAfterJavaWrite);
    }

    public String loadPropertyFile(final File propertyFile) {
        project.setProperty("key", null);
        LoadPropertiesTask loadPropertiesTask = new LoadPropertiesTask();
        loadPropertiesTask.setProject(project);
        loadPropertiesTask.setRootPropertyFile(propertyFile);
        loadPropertiesTask.execute();
        String testString = project.getProperty("key");
        System.out.println(testString);
        return testString;
    }

}
