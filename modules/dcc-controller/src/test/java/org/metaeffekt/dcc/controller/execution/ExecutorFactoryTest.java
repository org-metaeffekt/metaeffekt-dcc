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

import org.metaeffekt.dcc.commons.execution.Executor;
import org.metaeffekt.dcc.commons.mapping.TestProfiles;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.io.File;


/**
 * Created by i001450 on 01.07.14.
 */
public class ExecutorFactoryTest {

    private ExecutionContext executionContext;

    @Before
    public void prepare() {
        executionContext = new ExecutionContext(new SSLConfiguration("keyStoreLocation",
                "keyStorePassword", "trustStoreLocation", "trustStorePassword"));
        executionContext.setTargetBaseDir(new File("target/dcc-destination"));
        executionContext.setProfile(TestProfiles.QUITECOMLEXPROFILE);
        executionContext.prepareForExecution();
    }

    @Test
    public void executorForLocalhost() {
        executionContext.getPropertiesHolder().setProperty(TestProfiles.PROVIDED_HOST_CAPABILITY, "name", "localhost");
        executionContext.getDeploymentProperties().setProperty(LocalExecutor.LOCAL_DEPLOYMENT_TARGET_DIR, "target/local-destination");

        Executor executor = ExecutorFactory.create(TestProfiles.CONFIGURATION_UNIT_01, executionContext);
        assertEquals(LocalExecutor.class, executor.getClass());
    }

    @Test
    public void executorFor127001() {
        executionContext.getPropertiesHolder().setProperty(TestProfiles.PROVIDED_HOST_CAPABILITY, "name", "127.0.0.1");
        executionContext.getDeploymentProperties().setProperty(LocalExecutor.LOCAL_DEPLOYMENT_TARGET_DIR, "target/local-destination");

        Executor executor = ExecutorFactory.create(TestProfiles.CONFIGURATION_UNIT_01, executionContext);
        assertEquals(LocalExecutor.class, executor.getClass());
    }

    @Test(expected = IllegalStateException.class)
    public void executorForLocalhostNoLocalDestination() {
        executionContext.getPropertiesHolder().setProperty(TestProfiles.PROVIDED_HOST_CAPABILITY, "name", "localhost");
        executionContext.getDeploymentProperties().remove(LocalExecutor.LOCAL_DEPLOYMENT_TARGET_DIR); // IllegalState

        Executor executor = ExecutorFactory.create(TestProfiles.CONFIGURATION_UNIT_01, executionContext);
        assertEquals(LocalExecutor.class, executor.getClass());
    }

    @Test(expected = IllegalStateException.class)
    public void executorFor127001NoLocalDestination() {
        executionContext.getPropertiesHolder().setProperty(TestProfiles.PROVIDED_HOST_CAPABILITY, "name", "127.0.0.1");
        executionContext.getDeploymentProperties().remove(LocalExecutor.LOCAL_DEPLOYMENT_TARGET_DIR); // IllegalState

        Executor executor = ExecutorFactory.create(TestProfiles.CONFIGURATION_UNIT_01, executionContext);
        assertEquals(LocalExecutor.class, executor.getClass());
    }

    @Test
    public void remoteExecutor() {
        executionContext.getPropertiesHolder().setProperty(TestProfiles.PROVIDED_HOST_CAPABILITY, "name", "someHostname");

        Executor executor = ExecutorFactory.create(TestProfiles.CONFIGURATION_UNIT_01, executionContext);
        assertEquals(RemoteExecutor.class, executor.getClass());
    }
}
