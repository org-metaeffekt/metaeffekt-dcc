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


import org.apache.tools.ant.taskdefs.Delete;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.dcc.commons.dependency.UnitDependencies;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.spring.xml.ProfileParser;
import org.metaeffekt.dcc.controller.commands.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ExternalProfileTest {

    @Ignore
    @Test
    public void testHostInference() throws IOException {
        final File baseFolder = new File("/Volumes/USB/...");
        final File testProfileFile = new File(baseFolder, "profiles/...-deployment-profile.xml");
        Profile profile = ProfileParser.parse(testProfileFile);
        profile.setSolutionDir(baseFolder);
        PropertiesHolder propertiesHolder = profile.createPropertiesHolder(true);
        profile.evaluate(propertiesHolder);

        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProfile(profile);
        executionContext.setTargetBaseDir(new File("/Users/kklein/workspace/temp"));
        executionContext.setSolutionDir(profile.getSolutionDir());


        List<ConfigurationUnit> units = executionContext.getProfile().getUnits(false);
        UnitDependencies unitDependencies = executionContext.getProfile().getUnitDependencies();
        final List<List<ConfigurationUnit>> groupLists = unitDependencies.evaluateDependencyGroups(units);

        for (List<ConfigurationUnit> group : groupLists) {
            System.out.println("Group");
            group.stream().forEach(u -> System.out.println("  " + u.getId()));
        }

        boolean parallel = true;

        deleteDir(new File(baseFolder, "work"));
        deleteDir(executionContext.getTargetBaseDir());

        System.setProperty("dcc.remote.simulation", "true");
        System.setProperty("....simulation", "true");

        new InitializeResourcesCommand(executionContext).execute(true);
        new InitializeCommand(executionContext).execute(true, parallel);

        new VerifyCommand(executionContext).execute(true, parallel);
        new StopCommand(executionContext).execute(true, parallel);
        new InstallCommand(executionContext).execute(true, parallel);
        new ConfigureCommand(executionContext).execute(true, parallel);
        new StartCommand(executionContext).execute(true, parallel);

        deleteDir(new File(baseFolder, "work"));
        deleteDir(executionContext.getTargetBaseDir());
    }

    private void deleteDir(File work) {
        Delete delete = new Delete();
        delete.setDir(work);
        delete.execute();
    }

}
