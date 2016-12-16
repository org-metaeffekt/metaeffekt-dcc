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
package org.metaeffekt.dcc.controller.execution;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.spring.xml.ProfileParser;
import org.metaeffekt.dcc.controller.commands.InitializeCommand;
import org.metaeffekt.dcc.controller.commands.VerifyCommand;

import java.io.File;
import java.io.IOException;

public class HostInferenceTest {

    @Ignore
    @Test
    public void testHostInference() throws IOException {
        final File baseFolder = new File("/Volumes/USB/xxx");
        final File testProfileFile = new File(baseFolder, "profiles/xxx-deployment-profile.xml");
        Profile profile = ProfileParser.parse(testProfileFile);
        profile.setSolutionDir(baseFolder);
        PropertiesHolder propertiesHolder = profile.createPropertiesHolder(true);
        profile.evaluate(propertiesHolder);
        propertiesHolder.dump();

        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProfile(profile);
        executionContext.setTargetBaseDir(new File("target/test-solution/host-inference"));
        executionContext.setSolutionDir(profile.getSolutionDir());

        new InitializeCommand(executionContext).execute(true, true);

        new VerifyCommand(executionContext).execute(true, false);
    }

}
