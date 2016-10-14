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
import java.io.IOException;
import java.util.Properties;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;
import org.junit.Assert;
import org.junit.Before;

import org.metaeffekt.dcc.commons.DccUtils;
import org.metaeffekt.dcc.commons.ant.ProjectAdapter;
import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.PackageId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.execution.ExecutionStateHandler;

public class AbstractScriptExecutionTest {

    private static File targetDir = new File("target/opt-configure");

    private File dccDir = new File("target", "dcc");

    private File packageDir = new File(dccDir, "packages");

    private File resourcesDir = new File("target/test-classes/config");

    private File antHomeDir = null;

    private CommandScriptExecutionContext scriptExecutionContext;

    private ExecutionStateHandler executionStateHandler;

    @Before
    public void initialize() throws IOException {
        scriptExecutionContext = new CommandScriptExecutionContext();
        scriptExecutionContext.setPackageDir(getPackageDir());
        scriptExecutionContext.setTargetDir(getTargetDir());
        scriptExecutionContext.setAntHomeDir(getAntHomeDir());
        scriptExecutionContext.setSolutionDir(dccDir);
        
        executionStateHandler = new ExecutionStateHandler(getTargetDir(), dccDir);

        // copy properties supporting filtering
        final Project project = new Project();
        Copy copy = new Copy();
        copy.setProject(project);
        copy.setOverwrite(true);
        final FileSet fileSet = new FileSet();
        fileSet.setDir(new File("src/test/resources/config"));
        fileSet.setIncludes("**/*");
        copy.addFileset(fileSet);
        copy.setTodir(getResourcesDir());
        copy.setFiltering(true);

        // enable token replacements
        Properties properties = getConfigurationFilterProperties();
        for (Object key : properties.keySet()) {
            String name = key.toString();
            final String value = properties.getProperty(escapePropertyValue(name));
            copy.createFilterSet().addFilter(escapePropertyValue(name), escapePropertyValue(value));
        }

        copy.execute();
        
    }

    public String escapePropertyValue(String value) {

        final String BS = "\\";
        final String RXBS = BS + BS;
        final String WPC = RXBS + "s";
        value = value.replaceAll("," + WPC + WPC + WPC + "*", ", ");
        value = value.replaceAll(RXBS, BS + BS);
        value = value.replaceAll(RXBS + "=", BS + "=");
        value = value.replaceAll(RXBS + ":", BS + ":");
        value = value.replaceAll(RXBS, "/");
        return value;
    }

    protected Properties getConfigurationFilterProperties() {
        final Properties properties = new Properties();
        properties.putAll(System.getProperties());
        return properties;
    }

    protected void executeScript(Id<UnitId> unitId, Id<PackageId> packageId, Commands command) {
        File propertiesFile = DccUtils.propertyFile(getResourcesDir(), unitId, command);

        Assert.assertTrue(propertiesFile.toString() + " does not exist!", propertiesFile.exists());

        CommandScriptExecutor scriptExecutor =
            new CommandScriptExecutor(scriptExecutionContext, executionStateHandler);
        scriptExecutor.executeCommand(command, unitId, packageId, propertiesFile, null);
    }

    public static void deleteTargetDir() {
        Delete delete = new Delete();
        delete.setProject(new ProjectAdapter());
        delete.setDir(getTargetDir());
        delete.execute();
    }

    public File getDccDir() {
        return dccDir;
    }

    public void setDccDir(File dccDir) {
        this.dccDir = dccDir;
    }

    public static File getTargetDir() {
        return targetDir;
    }

    public static void setTargetDir(File targetDir) {
        AbstractScriptExecutionTest.targetDir = targetDir;
    }

    public File getPackageDir() {
        return packageDir;
    }

    public void setPackageDir(File packageDir) {
        this.packageDir = packageDir;
    }

    public File getResourcesDir() {
        return resourcesDir;
    }

    public void setResourcesDir(File resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    public CommandScriptExecutionContext getScriptExecutionContext() {
        return scriptExecutionContext;
    }

    public void setScriptExecutionContext(CommandScriptExecutionContext scriptExecutionContext) {
        this.scriptExecutionContext = scriptExecutionContext;
    }

    public File getAntHomeDir() {
        return antHomeDir;
    }

    public void setAntHomeDir(File antHomeDir) {
        this.antHomeDir = antHomeDir;
    }

}
