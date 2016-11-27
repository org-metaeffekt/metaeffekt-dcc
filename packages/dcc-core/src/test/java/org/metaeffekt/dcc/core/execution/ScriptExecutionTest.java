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
package org.metaeffekt.dcc.core.execution;


import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.script.AbstractScriptExecutionTest;



public class ScriptExecutionTest extends AbstractScriptExecutionTest {

    @Test
    public void test() {
        
        executeScript(Id.createUnitId("TestUnit"), Id.createPackageId("dcc-core-test"), Commands.VERIFY);
        
        File targetLocationBaseDir = new File("target/opt-configure/data/TestUnit");
        
        // check the folder was copied
        Assert.assertTrue(new File(targetLocationBaseDir, "foldertest").exists());
        Assert.assertTrue(new File(targetLocationBaseDir, "foldertest").isDirectory());
        Assert.assertTrue(new File(targetLocationBaseDir, "foldertest/dcc-core-profile.xml").exists());

        // check the file was copied
        Assert.assertTrue(new File(targetLocationBaseDir, "filetest").exists());
        Assert.assertTrue(new File(targetLocationBaseDir, "filetest").isFile());
    }

}
