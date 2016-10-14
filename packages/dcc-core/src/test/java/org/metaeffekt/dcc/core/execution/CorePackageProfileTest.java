package org.metaeffekt.dcc.core.execution;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import org.metaeffekt.dcc.controller.AbstractPackageProfileTest;
import org.metaeffekt.dcc.controller.commands.InitializeResourcesCommand;
import org.metaeffekt.dcc.controller.commands.PurgeCommand;
import org.metaeffekt.dcc.controller.commands.UninstallCommand;
import org.metaeffekt.dcc.controller.commands.VerifyCommand;

public class CorePackageProfileTest extends AbstractPackageProfileTest {

    @Test
    public void testProfile() {
        try {
            new InitializeResourcesCommand(getExecutionContext()).execute(true);
            new VerifyCommand(getExecutionContext()).execute(true);

            new UninstallCommand(getExecutionContext()).execute(true);
            new PurgeCommand(getExecutionContext()).execute(true);

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
        }
        
        // check everything is deleted after purge
        Assert.assertFalse(new File("target/opt/config").exists());
    }

}
