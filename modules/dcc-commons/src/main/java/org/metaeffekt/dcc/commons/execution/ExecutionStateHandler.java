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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.DccUtils;
import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;
import org.metaeffekt.dcc.commons.domain.Type.HostName;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;

/**
 * The {@link ExecutionStateHandler} manages the state of the executed commands. In general,
 * the DCC retrieves the state from the target hosts as zip and unpacks it to a local state 
 * cache directory.
 * 
 * The state cache differentiates the status from different deployment ids and different 
 * target host. This differentiation was added after version 1.2.2 of the DCC.
 * 
 * It is important to note, that the zip does not include host details (as the zip itself) is
 * host-bound. In the cache nevertheless the host is on level in the folder structure.
 * 
 * @author Alexander D.
 * @author Karsten Klein
 */
public class ExecutionStateHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionStateHandler.class);

    static final String ZIP_FILE_NAME = "consolidated-status.zip";

    private final File configurationDirectory;

    private final File stateCacheDirectory;

    public ExecutionStateHandler(File targetDir, File solutionDir) {
        this.configurationDirectory = new File(targetDir, DccConstants.CONFIG_SUB_DIRECTORY);
        this.stateCacheDirectory = new File(solutionDir, DccConstants.STATE_CACHE_SUB_DIRECTORY);

        DccUtils.prepareFoldersForWriting(configurationDirectory, stateCacheDirectory);
    }

    /**
     * Collect all execution properties files in the ${dcc.solution.dir}/config directory into a zip
     * file and return it as an InputStream
     *
     * @return An InputStream
     */
    public InputStream consolidateState(Id<DeploymentId> deploymentId) {
        Validate.notNull(deploymentId);
        Validate.notNull(deploymentId.getValue());

        LOG.debug("Collecting all execution properties to create consolidated state.");

        // FIXME: this collects also properties in the tmp structures
        Collection<File> allExecutionProperties = Collections.emptyList();
        if (configurationDirectory.exists()) {
            allExecutionProperties = FileUtils.listFiles(configurationDirectory, new String[] { "properties" }, true);
        }

        if (allExecutionProperties.size() > 0) {
            final URI configurationFolderURI = configurationDirectory.toURI();
            try {
                final File targetFile = new File(configurationDirectory, ZIP_FILE_NAME);
                deleteFile(targetFile);
                try (OutputStream out = new FileOutputStream(targetFile);
                        ZipOutputStream zos = new ZipOutputStream(out)) {
                    for (File file : allExecutionProperties) {
                        String name = configurationFolderURI.relativize(file.toURI()).getPath();
                        LOG.debug("Found [{}]", name);
                        if (name.contains("/tmp/") || name.contains("\\tmp\\")) {
                            // FIXME: we should move the tmp folder out of the config folder
                            LOG.debug("Skipping [{}] as it is included in a tmp folder", name);
                        } else {
                            zos.putNextEntry(new ZipEntry(name));
                            try (FileInputStream fis = new FileInputStream(file)) {
                                IOUtils.copy(fis, zos);
                            }
                        }
                    }
                    zos.finish();
                }
                if (targetFile.exists()) {
                    AutoCloseInputStream autoCloseStream =
                        new AutoCloseInputStream(new FileInputStream(targetFile)) {
                        @Override
                        public void close() throws IOException {
                            super.close();
                            targetFile.delete();
                        }
                    };
                    return autoCloseStream;
                }
                
                // no state produced
                return null;
            } catch (IOException e) {
                throw new IllegalStateException("Error while updating execution status:", e);
            }
        }
        return null;
    }

    /**
     * Called by the controller to update the local consolidated state,
     * i.e. collection of execution properties files, in the state cache directory.
     * 
     * @param inputStream An InputStream representing a zip file which contains a bundle of
     *            execution properties files
     * @param host 
     * @param deploymentId 
     */
    public void updateConsolidatedState(InputStream inputStream, Id<HostName> host, Id<DeploymentId> deploymentId) {
        Validate.notNull(deploymentId);
        Validate.notNull(deploymentId.getValue());
        
        LOG.debug("Executing state update.");
        
        File stateDir = getCacheLocation(host, deploymentId);

        // delete
        deleteFile(stateDir);

        // recreate folder
        stateDir.mkdirs();

        if (inputStream != null) {
            try (ZipInputStream zis = new ZipInputStream(inputStream)) {
                ZipEntry zipEntry;
                while ((zipEntry = zis.getNextEntry()) != null) {
                    String filename = zipEntry.getName();
                    LOG.debug("Received [{}]", filename);
                    File file = new File(stateDir, filename);
                    file.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(file);
                    try {
                        final byte[] buf = new byte[8192];
                        int length;
                        while ((length = zis.read(buf, 0, buf.length)) >= 0) {
                            fos.write(buf, 0, length);
                        }
                    } finally {
                        IOUtils.closeQuietly(fos);
                    }
                }
            } catch (IOException e) {
                LOG.error("Unable to update local execution state: ", e.getMessage());
                LOG.debug("Error details:", e);
            }
        }
    }

    /**
     * Deletes the passed in file or folder. In case the operation cannot be performed, waits and retries. Throws
     * an exception in case all attempts to delete fail.
     *
     * @param file The file to delete.
     */
    private void deleteFile(File file) {
        // NOTE: using the dcc from within the eclipse ide on windows it seems that this code breaks, since eclipse is
        // constantly refreshing and indexing the files. Therefore, a retry mechanism was integrated, that is
        // compensate temporary file handles withing the state directory.
        int retries = 40;
        while (file.exists()) {
            try {
                if (file.exists()) {
                    FileUtils.forceDelete(file);
                }
                break;
            } catch (IOException e) {
                retries--;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    // do nothing
                }
                if (retries == 0) {
                    throw new IllegalStateException("Unable to delete file/folder: " + file, e);
                }
            }
        }
    }

    /**
     * Called by the command executor after successful command execution to update the local
     * execution state (cache).
     * 
     * @param executionPropertiesFile
     */
    public void persistStateAfterSuccessfulExecution(Id<? extends Type> id, Commands command,
            File executionPropertiesFile) {

        File targetFolder = configurationDirectory;
        if (id.getType().isAssignableFrom(UnitId.class)) {
            handleStateOfDependendCommands(id.getValue(), command);
        }

        // handle file if exists (some commands do not produce a properties file)
        if (executionPropertiesFile != null && executionPropertiesFile.exists()) {
            // some commands should not persist a state at all
            if (command.persistentState()) {
                try {
                    File targetFile = DccUtils.propertyFileForGenericId(targetFolder, id, command);
                    
                    // NOTE we do not delete/move any file if the source is the target. This
                    //   is for example the case for the agent watchdog, which operates on the start.properties.
                    if (!executionPropertiesFile.getCanonicalPath().equals(targetFile.getCanonicalPath())) {
                        FileUtils.deleteQuietly(targetFile);
                        FileUtils.moveFile(executionPropertiesFile, targetFile);
                        if (!targetFile.exists()) {
                            throw new IllegalStateException(String.format(
                                "Error while updating execution status: Moved execution properties are not in %s",
                                targetFile));
                        }
                    }
                } catch (IOException up) {
                    LOG.error(String.format(
                        "Error while updating execution status: Unable to move execution properties %s to %s",
                        executionPropertiesFile.getAbsolutePath(),
                        configurationDirectory.getAbsolutePath()), up);
                    throw new RuntimeException(up);
                }
            }
        }
    }

    /**
     * Called by the controller to check if the command has already been executed before
     * 
     * @param id
     * @param command
     * @return
     */
    public boolean alreadySuccessfullyExecuted(Id<? extends Type> id, Commands command, Id<HostName> host, Id<DeploymentId> deploymentId) {
        File cacheLocation = getCacheLocation(host, deploymentId);
        File propertiesFile = DccUtils.propertyFileForGenericId(cacheLocation, id, command);
        return propertiesFile.exists();
    }

    public File getCacheLocation(Id<HostName> host, Id<DeploymentId> deploymentId) {
        File context = new File(stateCacheDirectory, deploymentId.getValue());
        if (host != null) {
            // use the host name as sub-folder when given 
            context = new File(context, host.getValue());
        }
        return context;
    }

    public File getStateCacheDirectory() {
        return stateCacheDirectory;
    }

    public File getConfigurationDirectory() {
        return configurationDirectory;
    }

    /**
     * The executions of the some commands may affect the state of other commands.
     * When e.g. executing START for a unit, the state of the STOP command is reset and vice versa.
     * Called by the command executor after successful command execution to reset the state in its
     * local config directory.
     */
    private void handleStateOfDependendCommands(String unitId, Commands commandToBeExecuted) {
        handleStartStop(unitId, commandToBeExecuted, configurationDirectory);
        handleUninstall(unitId, commandToBeExecuted, configurationDirectory);
    }

    /**
     * The executions of the START and STOP commands do not depend on their own previous execution
     * but on that the other one.
     * When executing START for a unit, the state of the STOP command is reset and vice versa.
     */
    private void handleStartStop(String unitId, Commands commandToBeExecuted, File configurationDir) {
        Commands commandToProcess;

        if (commandToBeExecuted == Commands.START) {
            commandToProcess = Commands.STOP;
        } else if (commandToBeExecuted == Commands.STOP) {
            commandToProcess = Commands.START;
        } else {
            return;
        }

        File file = DccUtils.propertyFile(configurationDir, unitId, commandToProcess);
        LOG.debug("Resetting state of [{}] command for unit [{}]", commandToProcess, unitId);
        FileUtils.deleteQuietly(file);
    }

    private void handleUninstall(String unitId, Commands commandToBeExecuted, File stateDir) {
        if (commandToBeExecuted == Commands.UNINSTALL) {
            File file = DccUtils.propertyFile(stateDir, unitId, commandToBeExecuted);
            
            // access the parent folder of the command
            file = file.getParentFile();
            
            LOG.debug("Resetting state for unit [{}]", unitId);

            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot manage state consistently.", ex);
            }
        }
    }

}
