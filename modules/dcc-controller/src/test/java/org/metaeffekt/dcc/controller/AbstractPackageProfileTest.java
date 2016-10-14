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
package org.metaeffekt.dcc.controller;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.WaitFor;
import org.apache.tools.ant.taskdefs.condition.Not;
import org.apache.tools.ant.taskdefs.condition.Socket;
import org.junit.Assert;
import org.junit.Before;

import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.spring.xml.ProfileParser;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;

public class AbstractPackageProfileTest {

    protected final File targetFolder = new File("target");

    protected final File solutionFolder = new File(targetFolder, "dcc");

    protected final File packageFolder = new File(solutionFolder, "packages");

    protected final File destinationFolder = new File(targetFolder, "opt");

    protected String profileName = "test-deployment-profile.xml";

    protected boolean resetState = true;

    protected Profile profile;

    protected ExecutionContext executionContext;

    public void resetState() {
        delete("dcc/work");
        delete("opt");
    }

    public void delete(final String folder) {
        Delete delete = new Delete();
        delete.setProject(new Project());
        delete.setDir(new File("target", folder));
        delete.execute();
    }

    @Before
    public void setUp() {
        final File testProfileFile = new File(solutionFolder, profileName);

        profile = ProfileParser.parse(testProfileFile);
        PropertiesHolder propertiesHolder = profile.createPropertiesHolder(true);
        profile.evaluate(propertiesHolder);

        propertiesHolder.dump();

        executionContext = new ExecutionContext();
        executionContext.setProfile(profile);
        executionContext.setSolutionDir(solutionFolder);
        executionContext.setTargetDir(destinationFolder);

        if (resetState) {
            resetState();
        }
    }

    protected ExecutionContext getExecutionContext() {
        return executionContext;
    }

    protected void waitForPort(boolean toStart, int port, long maxWaitMillis) {
        final Project project = new Project();

        final WaitFor waitFor = new WaitFor();
        waitFor.setProject(project);
        waitFor.setTimeoutProperty("timeoutOccur");

        final Socket socket = new Socket();
        socket.setServer("localhost");
        socket.setPort(port);

        if (toStart) {
            waitFor.add(socket);
        } else {
            final Not not = new Not();
            not.add(socket);

            waitFor.addNot(not);
        }
        waitFor.setMaxWait(maxWaitMillis);
        waitFor.execute();

        Assert.assertNotEquals(
                "Port " + port + " is " + (toStart ? "not started." : " still active."), "true",
                project.getProperty("timeoutOccur"));
    }

}
