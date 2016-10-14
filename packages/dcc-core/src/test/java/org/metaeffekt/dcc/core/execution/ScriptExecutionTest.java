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
