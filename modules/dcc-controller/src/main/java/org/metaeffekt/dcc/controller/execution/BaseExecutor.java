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
package org.metaeffekt.dcc.controller.execution;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FilterSet;

import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.DccUtils;
import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.PackageId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.execution.ExecutionStateHandler;
import org.metaeffekt.dcc.commons.execution.Executor;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.script.CommandScriptExecutionContext;
import org.metaeffekt.dcc.commons.script.CommandScriptExecutor;
import org.metaeffekt.core.common.kernel.ant.log.LoggingProjectAdapter;

/**
 * @author Alexander D.
 */
public abstract class BaseExecutor implements Executor {
    
    static final String FOLDER_CUSTOM = "custom";
    static final String FOLDER_EXTENSION = "extension";
    static final String FOLDER_EXTERNAL = "external";
    
    static final String[] SOLUTION_ADDON_FOLDERS = 
        new String[] { FOLDER_CUSTOM, FOLDER_EXTENSION, FOLDER_EXTERNAL } ;

    private ExecutionContext executionContext;

    private ExecutionStateHandler executionStateHandler;

    protected BaseExecutor(ExecutionContext executionContext) {
        Validate.notNull(executionContext,
                "Please provide an execution context when creating an Executor.");
        this.executionContext = executionContext;
    }

    @Override
    public void clean() {
        cleanFolders(
            getWorkingTmpDirectory(), 
            getStateCacheDirectory());
    }
    
    @Override
    public abstract void purge();

    protected synchronized void cleanFolders(File... folders) {
        for (File folder : folders) {
            if (folder.exists()) {
                executionStateHandler.deleteFile(folder);
            }
        }
    }

    protected ExecutionContext getExecutionContext() {
        return executionContext;
    }

    protected ExecutionStateHandler getExecutionStateHandler() {
        initializeStateHanderIfRequired();
        return executionStateHandler;
    }

    protected File getWorkingTmpDirectory() {
        final ExecutionContext executionContext = getExecutionContext();
        return DccUtils.workingTmpDir(executionContext.getSolutionDir(), executionContext.getProfile().getDeploymentId());
    }

    protected File getConfigTmpDirectory() {
        final ExecutionContext executionContext = getExecutionContext();
        return DccUtils.workingTmpConfigDir(executionContext.getSolutionDir(), executionContext.getProfile().getDeploymentId());
    }

    protected File getStateCacheDirectory() {
        return DccUtils.workStateBaseDir(getExecutionContext().getSolutionDir());
    }

    private void initializeStateHanderIfRequired() {
        if (this.executionStateHandler == null) {
            this.executionStateHandler = new ExecutionStateHandler(
                executionContext.getTargetDir(), executionContext.getSolutionDir());
        }
    }

    protected void initializeLocalFolders() {
        DccUtils.prepareFoldersForWriting(
                getWorkingTmpDirectory(),
                getStateCacheDirectory(),
                getExecutionContext().getTargetDir());
    }

    protected void executeLocal(Commands command, ConfigurationUnit unit) {
        executeLocal(command, unit, null);
    }
    
    protected void executeLocal(Commands command, ConfigurationUnit unit, File upgradePropertiesFile) {
        logCommand(command, unit);

        final Id<UnitId> unitId = unit.getId();

        final File configTmpDirectory = getConfigTmpDirectory();
        final File executionPropertiesFile = DccUtils.propertyFile(configTmpDirectory, unitId, command);
        final File prerequisitesPropertiesFile = DccUtils.propertyFile(configTmpDirectory, unitId,
                DccConstants.PREREQUISITES_PROPERTIES_FILE_NAME);

        Id<PackageId> packageId = getExecutionContext().getPackageId(unit, command);

        final File solutionDir = getExecutionContext().getSolutionDir();
        final File packageDir = new File(solutionDir, DccConstants.PACKAGES_SUB_DIRECTORY);
        final File targetDir = getExecutionContext().getTargetDir();

        CommandScriptExecutionContext scriptExecutionContext = new CommandScriptExecutionContext();
        scriptExecutionContext.setPackageDir(packageDir);
        scriptExecutionContext.setTargetDir(targetDir);
        scriptExecutionContext.setSolutionDir(solutionDir);
        scriptExecutionContext.setDeploymentId(getExecutionContext().getProfile().getDeploymentId());
        scriptExecutionContext.setWorkingDir(getWorkingTmpDirectory());
        scriptExecutionContext.setUpgradePropertiesFile(upgradePropertiesFile);

        final CommandScriptExecutor scriptExecutor =
            new CommandScriptExecutor(scriptExecutionContext, getExecutionStateHandler());
        scriptExecutor.executeCommand(command, unitId, packageId, executionPropertiesFile,
                prerequisitesPropertiesFile);
    }

    /**
     * GenerateResources command is always executed locally
     */
    @Override
    public void initializeResources(ConfigurationUnit unit) {
        initializeLocalFolders();
        executeLocal(Commands.INITIALIZE_RESOURCES, unit);
    }
    
    /**
     * UpgradeResources command is always executed locally
     */
    @Override
    public void upgradeResources(ConfigurationUnit unit, File upgradePropertiesFile) {
        initializeLocalFolders();
        executeLocal(Commands.UPGRADE_RESOURCES, unit, upgradePropertiesFile);
    }

    @Override
    public void evaluateTemplateResources() {
        final File solutionDir = getExecutionContext().getSolutionDir();
        Validate.notNull(solutionDir);

        String[] includes = new String[SOLUTION_ADDON_FOLDERS.length];
        for (int i = 0; i < includes.length; i++) {
            includes[i] = SOLUTION_ADDON_FOLDERS[i] + "/**/__*__";
        }
        
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(solutionDir);
        directoryScanner.setIncludes(includes);
        directoryScanner.scan();
        String[] files = directoryScanner.getIncludedFiles();
        
        // build project containing all available properties from the profile
        final LoggingProjectAdapter project = new LoggingProjectAdapter();
        PropertiesHolder propertiesHolder = getExecutionContext().getPropertiesHolder();
        final Map<String, Properties> propertiesMap = propertiesHolder.getPropertiesMap();
        final FilterSet filterSet = new FilterSet();
        for (Map.Entry<String, Properties> entry : propertiesMap.entrySet()) {
            Properties p = entry.getValue();
            String identifiable = entry.getKey();
            
            final Enumeration<?> properties = p.propertyNames();
            while (properties.hasMoreElements()) {
                Object object = (Object) properties.nextElement();
                
                StringBuilder key = new StringBuilder();
                key.append(identifiable);
                key.append(".");
                key.append(object);
                
                filterSet.addFilter(key.toString(), String.valueOf(p.getProperty(object.toString())));
            }
        }

        // and now copy the file in filtering mode
        Copy copy = new Copy();
        copy.setProject(project);
        copy.setEncoding("UTF-8");
        copy.setOverwrite(true);
        final FilterSet copyFilterSet = copy.createFilterSet();
        copyFilterSet.setProject(project);
        copyFilterSet.setBeginToken("${");
        copyFilterSet.setEndToken("}");
        copyFilterSet.addConfiguredFilterSet(filterSet);
        
        for (String file : files) {
            File sourceFile = new File(solutionDir, file);
            String filename = sourceFile.getName();
            // cut away the underscores
            filename = filename.substring(2, filename.length() - 2);
            File targetFile = new File(sourceFile.getParent(), filename);
            copy.setFile(sourceFile);
            copy.setTofile(targetFile);
            copy.execute();
        }
    }
    
    protected abstract void logCommand(Commands command, ConfigurationUnit unit);

}
