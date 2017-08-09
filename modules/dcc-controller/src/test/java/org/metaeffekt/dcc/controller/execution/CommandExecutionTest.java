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
package org.metaeffekt.dcc.controller.execution;

import java.io.File;
import java.util.Properties;

import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;
import org.junit.Assert;
import org.junit.Test;

import org.metaeffekt.dcc.commons.ant.PropertyUtils;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.spring.xml.ProfileParser;
import org.metaeffekt.core.common.kernel.ant.log.LoggingProjectAdapter;

public class CommandExecutionTest {

    @Test
    public void test() {

        final File destDir = new File("target/test");

        final LoggingProjectAdapter project = new LoggingProjectAdapter();

        Delete delete = new Delete();
        delete.setProject(project);
        delete.setDir(destDir);
        delete.execute();
        
        Copy copy = new Copy();
        copy.setProject(project);
        copy.setOverwrite(true);
        final FileSet set = new FileSet();
        set.setDir(new File("src/test/resources/xyz-solution"));
        set.setIncludes("**/*");
        copy.addFileset(set);
        copy.setTodir(destDir);
        copy.execute();
        
        final File solutionDir = new File("target/test/"); 
        final Profile profile = ProfileParser.parse(new File(solutionDir, "xyz-standard/xyz-deployment-profile.xml"));
        final ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProfile(profile);
        executionContext.setSolutionDir(solutionDir);
        executionContext.prepareForExecution();
        
        LocalExecutor localExecutor = new LocalExecutor(executionContext, true);
        localExecutor.evaluateTemplateResources();
        
        Properties p = PropertyUtils.loadPropertyFile(new File(solutionDir, "external/some-dir/x.properties"));
        Assert.assertEquals("http://wdf-lap-0815:10815", p.getProperty("url"));
    }

}
