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
