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
package org.metaeffekt.dcc.commons.script;

import java.io.File;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;

/**
 * The {@link CommandScriptExecutionContext} class aggregates all context information for executing
 * an Ant script.
 * 
 * @author Karsten Klein
 */
public class CommandScriptExecutionContext {

    public CommandScriptExecutionContext() {
    }

    private File packageDir;

    private File solutionDir;

    private File targetDir;

    private File antHomeDir;

    private File workingDir;
    
    private File upgradePropertiesFile;

    private Id<DeploymentId> deploymentId = Id.createDeploymentId("default");

    public File getPackageDir() {
        return packageDir;
    }

    public void setPackageDir(File packageDir) {
        this.packageDir = packageDir;
    }

    public File getTargetDir() {
        return targetDir;
    }

    public void setTargetDir(File targetDir) {
        this.targetDir = targetDir;
    }

    public File getSolutionDir() {
        return solutionDir;
    }

    public void setSolutionDir(File solutionDir) {
        this.solutionDir = solutionDir;
    }

    public File getAntHomeDir() {
        return antHomeDir;
    }

    public void setAntHomeDir(File antHomeDir) {
        this.antHomeDir = antHomeDir;
    }

    public Id<DeploymentId> getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(Id<DeploymentId> deploymentId) {
        this.deploymentId = deploymentId;
    }

    public File getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(File workingDir) {
        this.workingDir = workingDir;
    }

    public File getUpgradePropertiesFile() {
        return upgradePropertiesFile;
    }

    public void setUpgradePropertiesFile(File upgradePropertiesFile) {
        this.upgradePropertiesFile = upgradePropertiesFile;
    }

}
