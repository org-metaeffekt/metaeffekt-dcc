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
package org.metaeffekt.dcc.commons.execution;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;
import org.metaeffekt.dcc.commons.domain.Type.HostName;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;

/**
 * @author Alexander D.
 */
public class ExecutionStateHandlerTest {

    private ExecutionStateHandler executionStateHandler;
    private File targetDir;
    private File emptyTargetDir;
    private File solutionDir;
    
    private Id<HostName> host = Id.createHostName("test-host");
    private Id<DeploymentId> deploymentId = Id.createDeploymentId("deployment-id");

    @Before
    public void prepare() {
        targetDir = new File("target/status-test/");
        emptyTargetDir = new File("target/status-test/empty");
        solutionDir = new File("target/status-test/solution");

        File destZipFile = new File(new File(targetDir, DccConstants.CONFIG_SUB_DIRECTORY), ExecutionStateHandler.ZIP_FILE_NAME);
        FileUtils.deleteQuietly(destZipFile);

        // establish prerequisite file system setup in target dir (without maven)
        Delete delete = new Delete();
        delete.setDir(targetDir);
        delete.execute();
        Copy copy = new Copy();
        copy.setProject(new Project());
        FileSet fileSet = new FileSet();
        fileSet.setDir(new File("src/test/resources/status-test"));
        fileSet.setIncludes("config/**/*, template*/**/*");
        copy.addFileset(fileSet);
        copy.setTodir(targetDir);
        copy.execute();
    }

    @Test
    public void generateStatusResponse() {
        executionStateHandler = new ExecutionStateHandler(targetDir, solutionDir);

        InputStream response = executionStateHandler.consolidateState(deploymentId);
        ZipInputStream zis = new ZipInputStream(response);
        try {
            ZipEntry zipEntry;
            Set<String> result = new HashSet<>();
            while ((zipEntry = zis.getNextEntry()) != null) {
                result.add(zipEntry.getName().trim());
            }

            Assert.assertEquals(2, result.size());
            assertTrue(result.contains("unit1/start.properties"));
            assertTrue(result.contains("unit2/start.properties"));
            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(response);
            IOUtils.closeQuietly(zis);
        }
    }

    @Test
    public void generateStatusResponseNoFiles() {
        executionStateHandler = new ExecutionStateHandler(emptyTargetDir, solutionDir);
        InputStream response = executionStateHandler.consolidateState(deploymentId);
        Assert.assertNull(response);
    }

    @Test
    public void alreadySuccessfullyExecuted() throws IOException {
        FileUtils.copyFileToDirectory(new File(new File(new File(targetDir,
                DccConstants.CONFIG_SUB_DIRECTORY), "unit1"),"start.properties"),
                new File(new File(solutionDir, DccConstants.STATE_CACHE_SUB_DIRECTORY), "unit1"));
        FileUtils.copyFileToDirectory(new File(new File(new File(targetDir,
                DccConstants.CONFIG_SUB_DIRECTORY), "unit2"), "start.properties"),
                new File(new File(solutionDir, DccConstants.STATE_CACHE_SUB_DIRECTORY), "unit2"));

        executionStateHandler = new ExecutionStateHandler(targetDir, solutionDir);
        
        InputStream consolidatedState = executionStateHandler.consolidateState(deploymentId);
        executionStateHandler.updateConsolidatedState(consolidatedState, host, deploymentId);

        assertTrue("unit1.start should have been executed", executionStateHandler.alreadySuccessfullyExecuted(Id.createUnitId("unit1"), Commands.START, host, deploymentId));
        assertTrue("unit2.start should have been executed", executionStateHandler.alreadySuccessfullyExecuted(Id.createUnitId("unit2"), Commands.START, host, deploymentId));
        assertFalse("unit3.start should NOT have been executed", executionStateHandler.alreadySuccessfullyExecuted(Id.createUnitId("unit3"), Commands.START, host, deploymentId));
        assertFalse("unit4.stop should NOT have been executed", executionStateHandler.alreadySuccessfullyExecuted(Id.createUnitId("unit4"), Commands.STOP, host, deploymentId));
    }

    @Test
    public void handleStart() throws IOException {
        executionStateHandler = new ExecutionStateHandler(emptyTargetDir, solutionDir);
        
        final File executedProperties =
            new File(new File(targetDir, "template1"), "stop.properties");
        final File toExecuteProperties =
            new File(new File(targetDir, "template1"), "start.properties");
        File unit6Source = executionStateHandler.getCacheLocation(host, deploymentId);
        File unit6Target = new File(unit6Source, "unit6");
        
        FileUtils.copyFileToDirectory(executedProperties, unit6Target);

        executionStateHandler = new ExecutionStateHandler(targetDir, solutionDir);

        final Id<UnitId> unitId = Id.createUnitId("unit6");
        assertTrue("unit6.stop should have been executed",
                executionStateHandler.alreadySuccessfullyExecuted(unitId, Commands.STOP, host, deploymentId));
        assertFalse("unit6.start should not have been executed",
                executionStateHandler.alreadySuccessfullyExecuted(unitId, Commands.START, host, deploymentId));

        executionStateHandler.persistStateAfterSuccessfulExecution(unitId, Commands.START,
                toExecuteProperties);
        executionStateHandler.updateConsolidatedState(executionStateHandler.consolidateState(deploymentId), host, deploymentId);

        assertFalse("unit6.stop state should have been cleared",
                executionStateHandler.alreadySuccessfullyExecuted(unitId, Commands.STOP, host, deploymentId));
        assertTrue("unit6.start should have been executed",
                executionStateHandler.alreadySuccessfullyExecuted(unitId, Commands.START, host, deploymentId));
    }

    @Test
    public void handleStop() throws IOException {
        executionStateHandler = new ExecutionStateHandler(emptyTargetDir, solutionDir);
        
        final File executedProperties =
            new File(new File(targetDir, "template2"), "start.properties");
        final File toExecuteProperties =
            new File(new File(targetDir, "template2"), "stop.properties");
        FileUtils.copyFileToDirectory(executedProperties, 
                new File(executionStateHandler.getCacheLocation(host, deploymentId), "unit5"));

        executionStateHandler = new ExecutionStateHandler(targetDir, solutionDir);

        final Id<UnitId> unitId = Id.createUnitId("unit5");
        assertTrue("unit5.start should have been executed",
                executionStateHandler.alreadySuccessfullyExecuted(unitId, Commands.START, host, deploymentId));
        assertFalse("unit5.stop should not have been executed",
                executionStateHandler.alreadySuccessfullyExecuted(unitId, Commands.STOP, host, deploymentId));

        executionStateHandler.persistStateAfterSuccessfulExecution(unitId, Commands.STOP,
                toExecuteProperties);
        executionStateHandler.updateConsolidatedState(executionStateHandler.consolidateState(deploymentId), host, deploymentId);

        assertFalse("unit5.start state should have been cleared",
                executionStateHandler.alreadySuccessfullyExecuted(unitId, Commands.START, host, deploymentId));
        assertTrue("unit5.stop should have been executed",
                executionStateHandler.alreadySuccessfullyExecuted(unitId, Commands.STOP, host, deploymentId));
    }

}
