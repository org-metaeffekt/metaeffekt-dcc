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
package org.metaeffekt.dcc.commons.spring.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.metaeffekt.dcc.commons.ant.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;
import org.metaeffekt.dcc.commons.domain.Type.ProfileId;
import org.metaeffekt.dcc.commons.mapping.Assert;
import org.metaeffekt.dcc.commons.mapping.Binding;
import org.metaeffekt.dcc.commons.mapping.CapabilityDefinition;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.properties.SortedProperties;

public class ProfileFactoryBean extends AbstractFactoryBean<Profile> implements
        ApplicationContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileFactoryBean.class);

    private Id<ProfileId> profileId;

    private Id<DeploymentId> deploymentId;

    private String profileType;

    private File origin;

    private String profileDescription;

    private String solutionPropertiesPath;

    private String deploymentPropertiesPath;

    private File solutionDir;

    private ApplicationContext appContext;

    public ProfileFactoryBean(Id<ProfileId> profileId, Id<DeploymentId> deploymentId, String profileType,
            String profileDescription, File origin) {
        this.profileId = profileId;
        this.deploymentId = deploymentId;
        this.profileType = profileType;
        this.profileDescription = profileDescription;
        this.origin = origin;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.appContext = applicationContext;
    }

    @Override
    public Class<?> getObjectType() {
        return Profile.class;
    }

    @Override
    protected Profile createInstance() throws Exception {
        Profile profile = new Profile();

        profile.setId(profileId);
        profile.setDeploymentId(deploymentId);
        profile.setType(Profile.Type.valueOf(profileType.toUpperCase(Locale.ENGLISH)));
        profile.setDescription(profileDescription);
        profile.setOrigin(origin);
        Map<String, CapabilityDefinition> capabilityDefinitions =
            appContext.getBeansOfType(CapabilityDefinition.class);
        Map<String, ConfigurationUnit> units = appContext.getBeansOfType(ConfigurationUnit.class);
        Map<String, Binding> bindings = appContext.getBeansOfType(Binding.class);
        Map<String, Assert> asserts = appContext.getBeansOfType(Assert.class);
        profile.setCapabilityDefinitions(new ArrayList<>(capabilityDefinitions.values()));
        profile.setBindings(new ArrayList<>(bindings.values()));
        profile.setAsserts(new ArrayList<>(asserts.values()));
        profile.setUnits(new ArrayList<>(units.values()));
        profile.setSolutionDir(solutionDir);
        if (solutionPropertiesPath != null) {
            final File propertyFile = new File(solutionPropertiesPath);
            profile.setSolutionProperties(loadProperties(propertyFile), propertyFile);
        }
        
        if (deploymentPropertiesPath != null) {
            final File propertyFile = new File(deploymentPropertiesPath);
            profile.setDeploymentProperties(loadProperties(propertyFile), propertyFile);
        }
        return profile;
    }

    public Properties loadProperties(final File propertyFile) {
        Properties properties;
        if (propertyFile.exists() && propertyFile.isFile()) {
            properties = PropertyUtils.loadPropertyFile(propertyFile);
        } else {
            LOG.warn("Cannot find properties file [{}].", propertyFile);
            properties = new SortedProperties();
        }
        return properties;
    }

    public String getSolutionPropertiesPath() {
        return solutionPropertiesPath;
    }

    public void setSolutionPropertiesPath(String solutionPropertiesPath) {
        this.solutionPropertiesPath = solutionPropertiesPath;
    }

    public String getDeploymentPropertiesPath() {
        return deploymentPropertiesPath;
    }

    public void setDeploymentPropertiesPath(String deploymentPropertiesPath) {
        this.deploymentPropertiesPath = deploymentPropertiesPath;
    }

    public File getSolutionDir() {
        return solutionDir;
    }

    public void setSolutionDir(File solutionDir) {
        this.solutionDir = solutionDir;
    }
    
}
