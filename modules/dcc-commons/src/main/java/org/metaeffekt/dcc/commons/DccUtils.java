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
package org.metaeffekt.dcc.commons;

import java.io.File;

import org.apache.commons.lang3.Validate;

import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;
import org.metaeffekt.dcc.commons.domain.Type.HostName;
import org.metaeffekt.dcc.commons.domain.Type.PackageId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.mapping.Identifiable;
import org.metaeffekt.core.commons.annotation.Public;

@Public
public final class DccUtils {

    private DccUtils() {
        // do nothing
    }

    public static File workingTmpDir(File solutionDir, Id<DeploymentId> deploymentId) {
        Validate.notNull(solutionDir);
        Validate.notNull(deploymentId);
        return new File(new File(solutionDir, DccConstants.TMP_SUB_DIRECTORY), deploymentId.getValue());
    }

    public static File workingTmpConfigDir(File solutionDir, Id<DeploymentId> deploymentId) {
        // currently these is the same as the workingTmpDir. A substructure is created anyways
        return workingTmpDir(solutionDir, deploymentId);
    }

    public static File propertyFile(File baseFolder, Id<UnitId> unitId, Commands command) {
        Validate.notNull(command);

        return propertyFile(baseFolder, unitId, propertyFileName(command));
    }

    public static File propertyFile(File baseFolder, String unitId, Commands command) {
        return propertyFile(baseFolder, Id.createUnitId(unitId), command);
    }

    @SuppressWarnings("unchecked")
    public static File propertyFileForGenericId(File baseFolder, Id<? extends Type> id,
            Commands command) {
        
        // assuming that baseFolder is the config folder. The folder may use
        // descriminators for the individual contexts (local state, remote host
        // specific)
        
        Validate.notNull(command);

        if (id.getType().isAssignableFrom(UnitId.class)) {
            return propertyFile(baseFolder, (Id<UnitId>) id, command);
        } else if (id.getType().isAssignableFrom(HostName.class)  || 
                id.getType().isAssignableFrom(DeploymentId.class)) {
            // backward compatibility (hostname dropped from file, since already included in baseFolder)
            final File file = new File(baseFolder, id.getValue() + "." + propertyFileName(command));
            if (file.exists()) {
                return file;
            }

            return new File(baseFolder, propertyFileName(command));
        } else {
            throw new IllegalArgumentException("Provided id [" + id + "] must be either a host name or a unit id.");
        }
    }

    public static File propertyFile(File baseFolder, Id<UnitId> unitId, String propertyFileName) {
        Validate.notNull(baseFolder);
        Validate.notNull(unitId);

        final File destFolder = new File(baseFolder, unitId.getValue());
        prepareFoldersForWriting(destFolder);

        return new File(destFolder, propertyFileName);
    }

    public static String propertyFileName(Commands command) {
        return propertyFileName(command.toString());
    }

    public static String propertyFileName(String nameWithoutExtension) {
        return String.format("%s.properties", nameWithoutExtension);
    }

    public static File getCommandScriptFile(File packageDir, Id<PackageId> packageId,
            Commands command) {
        final File executionBaseDir = new File(packageDir, packageId.getValue());

        return new File(new File(executionBaseDir, DccConstants.SCRIPTS_SUB_DIRECTORY), command
                + DccConstants.SCRIPT_EXTENSION);
    }

    public static File getPreCommandScriptFile(File solutionDir, Id<UnitId> unitId, Commands command) {
        return getHookScriptFile(solutionDir, unitId, command, "pre-");
    }

    public static File getPostCommandScriptFile(File solutionDir, Id<UnitId> unitId,
            Commands command) {
        return getHookScriptFile(solutionDir, unitId, command, "post-");
    }

    public static File getInsteadOfCommandScriptFile(File solutionDir, Id<UnitId> unitId,
            Commands command) {
        return getHookScriptFile(solutionDir, unitId, command, null);
    }

    private static File getHookScriptFile(File solutionDir, Id<UnitId> unitId, Commands command,
            String prefix) {
        final File executionBaseDir =
            new File(new File(solutionDir, DccConstants.HOOKS_SUB_DIRECTORY), unitId.getValue());

        final StringBuilder fileName = new StringBuilder();
        if (prefix != null) {
            fileName.append(prefix);
        }
        fileName.append(command);
        fileName.append(DccConstants.SCRIPT_EXTENSION);

        return new File(executionBaseDir, fileName.toString());
    }

    public static void prepareFoldersForWriting(File... folders) {
        for (File file : folders) {
            if (!file.exists()) {
                file.mkdirs();
                file.setWritable(true);
                file.setExecutable(true);
            } else if (!file.isDirectory()) {
                throw new IllegalStateException(String.format(
                    "Cannot create directory [%s] because a file with the same name already exists!", file));
            } else if (!file.canWrite()) {
                file.setWritable(true);
                if (!file.canWrite()) {
                    throw new IllegalStateException(String.format("[%s] is not writable!", file));
                }
            }
        }
    }
    
    public static final String deriveAttributeIdentifier(String uniqueContextId, String key) {
        return uniqueContextId + DccConstants.SEPARATOR_UNIT_KEY + key;
    }
    
    public static final String deriveAttributeIdentifier(Identifiable identifiable, String key) {
        return deriveAttributeIdentifier(identifiable.getUniqueId(), key);
    }

}
