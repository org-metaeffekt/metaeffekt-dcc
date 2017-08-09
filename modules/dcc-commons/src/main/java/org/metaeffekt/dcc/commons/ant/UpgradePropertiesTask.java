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
package org.metaeffekt.dcc.commons.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.texen.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.DccProperties;
import org.metaeffekt.dcc.commons.ant.wrapper.ProfileWrapper;
import org.metaeffekt.dcc.commons.ant.wrapper.PropertiesWrapper;
import org.metaeffekt.dcc.commons.ant.wrapper.SystemWrapper;
import org.metaeffekt.dcc.commons.ant.wrapper.WrapperContext;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.spring.xml.ProfileParser;

public class UpgradePropertiesTask extends Task {
    
    private static final Logger LOG = LoggerFactory.getLogger(UpgradePropertiesTask.class);

    private static final String PROPERTIES_ENCODING = StandardCharsets.ISO_8859_1.name();

    private File sourceProfileFile;

    private File targetProfileFile;

    private File propertiesTemplateFile;
    
    private File targetPropertiesFile;
    
    @Override
    public void execute() throws BuildException {
        applyTemplate(getProject());
    }

    public void applyTemplate(Project project) {
        System.setProperty(DccProperties.DCC_SYSTEM_PROPERTY_PROFILE_VALIDATION, "false");
        try {

            final Profile sourceProfile = ProfileParser.parse(sourceProfileFile);
            final PropertiesHolder sourceHolder = sourceProfile.createPropertiesHolder(true);
            sourceProfile.evaluate(sourceHolder);
            
            final Profile targetProfile = ProfileParser.parse(targetProfileFile);
            final PropertiesHolder targetHolder = targetProfile.createPropertiesHolder(false);
            targetProfile.evaluate(targetHolder);
    
            final Properties velocityProperties = new Properties();
            velocityProperties.setProperty(Velocity.RESOURCE_LOADER, "file");
            velocityProperties.setProperty("file.resource.loader.class",
                    "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
            velocityProperties.setProperty(Velocity.FILE_RESOURCE_LOADER_CACHE,
                    Boolean.FALSE.toString());
            velocityProperties.setProperty(Velocity.INPUT_ENCODING, PROPERTIES_ENCODING);
            velocityProperties.setProperty(Velocity.OUTPUT_ENCODING, PROPERTIES_ENCODING);
            
            velocityProperties.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS,
                    "org.apache.velocity.runtime.log.NullLogChute");
    
            final String templatePath = propertiesTemplateFile.getParentFile().getAbsolutePath();
            velocityProperties.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, templatePath);
    
            final VelocityEngine velocityEngine = new VelocityEngine(velocityProperties);
    
            final Template template = velocityEngine.getTemplate(propertiesTemplateFile.getName());
            final VelocityContext context = new VelocityContext();
            
            final WrapperContext sourceWrapperContext = new WrapperContext();
            final WrapperContext targetWrapperContext = new WrapperContext();
            
            context.put("source", new ProfileWrapper(sourceWrapperContext, sourceProfile, sourceHolder));
            context.put("target", new ProfileWrapper(targetWrapperContext, targetProfile, targetHolder));
            
            context.put("property", new PropertiesWrapper(sourceWrapperContext, new Properties()));
            context.put("system", new SystemWrapper(sourceWrapperContext));

            context.put("sourceProfileFile", sourceProfileFile);
            context.put("targetProfileFile", targetProfileFile);
            context.put("propertiesTemplateFile", propertiesTemplateFile);
    
            context.put("project", project);
            context.put("StringUtils", StringUtils.class);
            context.put("FileUtil", FileUtil.class);
            
            context.put("separator", DccConstants.PROPERTIES_SEPARATOR);
            
            // legacy 1.2.0 BWC
//            context.put("system", System.getProperties());
            context.put("deployment", sourceProfile.getDeploymentProperties());
            context.put("solution", sourceProfile.getSolutionProperties());
            context.put("profile", sourceHolder);

            targetPropertiesFile.getParentFile().mkdirs();
            try (Writer writer =
                new OutputStreamWriter(new FileOutputStream(targetPropertiesFile), PROPERTIES_ENCODING)) {
                template.merge(context, writer);
                LOG.info("Upgraded file [{}] written successfully.", targetPropertiesFile);
            } catch (IOException e) {
                throw new BuildException(e);
            }
        } finally {
            System.setProperty(DccProperties.DCC_SYSTEM_PROPERTY_PROFILE_VALIDATION, "true");
        }
    }

    public File getSourceProfileFile() {
        return sourceProfileFile;
    }

    public void setSourceProfileFile(File previousProfileFile) {
        this.sourceProfileFile = previousProfileFile;
    }

    public File getPropertiesTemplateFile() {
        return propertiesTemplateFile;
    }

    public void setPropertiesTemplateFile(File propertiesTemplateFile) {
        this.propertiesTemplateFile = propertiesTemplateFile;
    }

    public File getTargetPropertiesFile() {
        return targetPropertiesFile;
    }

    public void setTargetPropertiesFile(File targetPropertiesFile) {
        this.targetPropertiesFile = targetPropertiesFile;
    }

    public File getTargetProfileFile() {
        return targetProfileFile;
    }

    public void setTargetProfileFile(File targetProfileFile) {
        this.targetProfileFile = targetProfileFile;
    }
    
    public static void apply(Project project, File upgradePropertiesFile) {
        Properties upgradeProperties = PropertyUtils.loadEnrichedProperties(project, upgradePropertiesFile);
        
        File baseDir = upgradePropertiesFile.getParentFile();

        File sourceSolutionDir = new File(upgradeProperties.getProperty("source.solution.dir", baseDir.getAbsolutePath()));
        if (!sourceSolutionDir.isAbsolute()) {
            sourceSolutionDir = new File(baseDir, sourceSolutionDir.getPath());
        }

        // the source profile file is anticipated to be relative to the source.solution.dir setting
        File sourceProfileFile = new File(sourceSolutionDir, upgradeProperties.getProperty("source.profile.location"));

        // the target profile file is anticipated to be relative to the upgrade properties (why not the solution dir?)
        File targetProfileFile = new File(upgradeProperties.getProperty("target.profile.location"));
        if (!targetProfileFile.isAbsolute()) {
            targetProfileFile = new File(baseDir, targetProfileFile.getPath());
        }
        
        Validate.isTrue(sourceProfileFile.exists() && sourceProfileFile.isFile(), 
            String.format("The source profile cannot be found at '%s'.", sourceProfileFile.getAbsolutePath()));

        Validate.isTrue(targetProfileFile.exists() && targetProfileFile.isFile(), 
                String.format("The target profile cannot be found at '%s'.", targetProfileFile.getAbsolutePath()));

        final String deploymentPropertiesTemplatePath = upgradeProperties.getProperty("deployment.properties.template");
        final String solutionPropertiesTemplatePath = upgradeProperties.getProperty("solution.properties.template");

        File deploymentPropertiesTemplateFile = deploymentPropertiesTemplatePath != null ? 
            new File(baseDir, deploymentPropertiesTemplatePath) : null;
        File solutionPropertiesTemplateFile = solutionPropertiesTemplatePath != null ? 
            new File(baseDir, solutionPropertiesTemplatePath) : null;
        
        UpgradePropertiesTask task = new UpgradePropertiesTask();
        task.setProject(project);
        task.setSourceProfileFile(sourceProfileFile);
        task.setTargetProfileFile(targetProfileFile);
        task.setPropertiesTemplateFile(deploymentPropertiesTemplateFile);
        
        System.setProperty(DccProperties.DCC_SYSTEM_PROPERTY_PROFILE_VALIDATION, "false");
        final Profile targetProfil;
        try {
            targetProfil = ProfileParser.parse(targetProfileFile);
        } finally {
            System.setProperty(DccProperties.DCC_SYSTEM_PROPERTY_PROFILE_VALIDATION, "true");
        }
        
        executeTask(task, deploymentPropertiesTemplateFile, targetProfil.getDeploymentPropertiesFile(), "deployment");
        executeTask(task, solutionPropertiesTemplateFile, targetProfil.getSolutionPropertiesFile(), "solution");
    }

    private static void executeTask(UpgradePropertiesTask task, final File templateFile, final File targetFile, final String type) {
        if (templateFile != null) {
            Validate.notNull(targetFile, "The target file '%s' is not specified in the profile, but a "
                    + "template is given. Either do not specify a template or make sure the profile "
                    + "references a %s property file.", type, type);
            System.setProperty(DccProperties.DCC_SYSTEM_PROPERTY_PROFILE_VALIDATION, "false");
            try {
                task.setPropertiesTemplateFile(templateFile);
                task.setTargetPropertiesFile(targetFile);
                task.execute();
            } finally {
                System.setProperty(DccProperties.DCC_SYSTEM_PROPERTY_PROFILE_VALIDATION, "true");
            }
        }
    }
}
