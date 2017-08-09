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
package org.metaeffekt.dcc.docgenerator;

import static org.apache.commons.lang3.StringEscapeUtils.escapeXml11;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.metaeffekt.dcc.commons.DccProperties;
import org.metaeffekt.dcc.commons.ant.UrlUtils;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.ProfileId;
import org.metaeffekt.dcc.commons.mapping.Attribute;
import org.metaeffekt.dcc.commons.mapping.AttributeKey;
import org.metaeffekt.dcc.commons.mapping.Binding;
import org.metaeffekt.dcc.commons.mapping.Capability;
import org.metaeffekt.dcc.commons.mapping.CapabilityDefinition;
import org.metaeffekt.dcc.commons.mapping.CapabilityDefinitionReference;
import org.metaeffekt.dcc.commons.mapping.CommandDefinition;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.spring.xml.ProfileParser;
import org.metaeffekt.core.commons.annotation.Public;

/**
 * The {@link DocGenerator} produces documentation based on the profile.
 * 
 * @author Karsten Klein
 */
@Public
public class DocGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(DocGenerator.class);

    private static final String TOOLTIP_NEWLINE = "&#10;";

    private static final String ENCODING_UTF_8 = "UTF-8";

    private List<File> profileFiles;
    private List<File> templateFiles;
    private List<File> profileTemplateFiles;
    private List<File> unitTemplateFiles;

    private File solutionRootDir;

    private File targetDir;

    private String packageId;

    private boolean loadDeploymentProperties = true;

    private Map<Id<ProfileId>, PropertiesHolder> profilePropertiesHolderMap = new HashMap<>();

    private boolean includeContributionProfiles = false;
    private boolean includeBaseProfiles = false;

    public DocGenerator() {
        File templateDir = new File("/META-INF/templates/");
        File templateFileProfileList = new File(templateDir, "index.html.vt");
        File templateFileCapabilityDefinitions = new File(templateDir, "profile-capability-definitions.html.vt");
        File templateFileUnitSvg = new File(templateDir, "unit.svg.vt");
        File templateFileProfiles = new File(templateDir, "profile-unit-overview.html.vt");
        File templateFileProfileUnits = new File(templateDir, "profile-unit-reference.html.vt");
        File templateFileCapabilities = new File(templateDir, "profile-capabilities.html.vt");

        templateFiles = new ArrayList<>();
        templateFiles.add(templateFileProfileList);

        profileTemplateFiles = new ArrayList<>();
        profileTemplateFiles.add(templateFileCapabilityDefinitions);
        profileTemplateFiles.add(templateFileCapabilities);
        profileTemplateFiles.add(templateFileProfiles);
        profileTemplateFiles.add(templateFileProfileUnits);
        // profileTemplateFiles.add(templateFileProfileSvg);

        unitTemplateFiles = new ArrayList<>();
        unitTemplateFiles.add(templateFileUnitSvg);
    }

    public void generate() {
        loadProfiles();
    }

    protected void loadProfiles() {
        List<Profile> profiles = new ArrayList<>();
        if (profiles != null) {
            String orginalValidationValue = System.getProperty(DccProperties.DCC_SYSTEM_PROPERTY_PROFILE_VALIDATION, "true");
            try {
                for (File file : profileFiles) {
                    System.setProperty(DccProperties.DCC_SYSTEM_PROPERTY_PROFILE_VALIDATION, "false");
                    try {
                        Profile profile = ProfileParser.parse(file, solutionRootDir);
                        if (includeBaseProfiles && profile.getType().equals(Profile.Type.BASE)) {
                            profiles.add(profile);
                        }
                        if (includeContributionProfiles && profile.getType().equals(Profile.Type.CONTRIBUTION)) {
                            profiles.add(profile);
                        }
                        if (profile.getType().equals(Profile.Type.SOLUTION)) {
                            profiles.add(profile);
                        }
                        if (profile.getType().equals(Profile.Type.DEPLOYMENT)) {
                            profiles.add(profile);
                        }
                    } catch (Exception e) {
                        LOG.warn("Unable to process profile {}. {}", file, e.getMessage());
                    }
                }
            } finally {
                System.setProperty(DccProperties.DCC_SYSTEM_PROPERTY_PROFILE_VALIDATION, orginalValidationValue);
            }
        }

        for (Profile profile : profiles) {
            Delete delete = new Delete();
            delete.setProject(new Project());
            delete.setDir(new File(targetDir, profile.getId().getValue()));
            delete.execute();
        }

        if (templateFiles != null) {
            for (File templateFile : templateFiles) {
                applyTemplate(templateFile, null, null, profiles, targetDir);
            }
        }

        if (profileTemplateFiles != null) {
            for (File profileTemplateFile : profileTemplateFiles) {
                for (Profile profile : profiles) {
                    applyTemplate(profileTemplateFile, null, profile, profiles,
                            new File(targetDir, getSubfolder(profile)));
                }
            }
        }

        if (unitTemplateFiles != null) {
            for (File unitTemplateFile : unitTemplateFiles) {
                for (Profile profile : profiles) {
                    for (ConfigurationUnit unit : profile.getUnits(true)) {
                        applyTemplate(unitTemplateFile, unit, profile, profiles,
                                new File(targetDir, getSubfolder(profile)));
                    }
                }
            }
        }
    }

    public String getSubfolder(Profile profile) {
        String id = profile.getId().getValue();
        if (id == null) {
            id = profile.getId().getValue();
        }
        return id;
    }

    public void applyTemplate(File templateFile, ConfigurationUnit unit, Profile profile, List<Profile> profiles,
            File targetDir) {
        Properties velocityProperties = new Properties();
        velocityProperties.setProperty(Velocity.RESOURCE_LOADER, "class");
        velocityProperties.setProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityProperties.setProperty(Velocity.INPUT_ENCODING, ENCODING_UTF_8);
        velocityProperties.setProperty(Velocity.OUTPUT_ENCODING, ENCODING_UTF_8);
        velocityProperties.setProperty(Velocity.VM_LIBRARY, "/META-INF/templates/macros.vm");
        velocityProperties.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS,
                "org.apache.velocity.runtime.log.NullLogChute");
        VelocityEngine velocityEngine = new VelocityEngine(velocityProperties);

        Template template = velocityEngine.getTemplate(templateFile.getPath().replace('\\', '/'));

        VelocityContext context = new VelocityContext();
        context.put("generator", this);
        context.put("profiles", profiles);
        context.put("profile", profile);
        context.put("unit", unit);
        context.put("StringEscapeUtils", StringEscapeUtils.class);
        context.put("solutionRootDir", solutionRootDir);
        context.put("urlUtils", new UrlUtils());

        targetDir.mkdirs();

        String targetFileName = templateFile.getName();
        if (targetFileName.endsWith("svg.vt")) {
            targetFileName = "resources/" + targetFileName;
            new File(targetDir, "resources").mkdir();
        }
        targetFileName = targetFileName.replaceFirst("\\.vt", "");
        if (unit != null) {
            targetFileName = targetFileName.replaceFirst("unit.", unit.getId() + ".");
        } else {
            if (profile != null) {
                targetFileName = targetFileName.replaceFirst("profile-", "");
            }
        }
        File targetFile = new File(targetDir, targetFileName);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(targetFile), ENCODING_UTF_8)) {
            template.merge(context, writer);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    public List<CapabilityDefinition> sort(List<CapabilityDefinition> capabilityDefinitions) {
        Collections.sort(capabilityDefinitions, new Comparator<CapabilityDefinition>() {
            @Override
            public int compare(CapabilityDefinition o1, CapabilityDefinition o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        return capabilityDefinitions;
    }

    public List<ConfigurationUnit> sortUnits(List<ConfigurationUnit> units) {
        Collections.sort(units, new Comparator<ConfigurationUnit>() {
            @Override
            public int compare(ConfigurationUnit o1, ConfigurationUnit o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        return units;
    }

    public <T extends Capability> List<T> sortCapabilities(List<T> capabilities) {
        Collections.sort(capabilities, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        return capabilities;
    }

    public List<CommandDefinition> sortCommands(List<CommandDefinition> commands) {
        Collections.sort(commands, new Comparator<CommandDefinition>() {
            @Override
            public int compare(CommandDefinition o1, CommandDefinition o2) {
                return o1.getCommandId().name().compareTo(o2.getCommandId().name());
            }
        });
        return commands;
    }

    public List<Attribute> sortAttributes(Collection<Attribute> attributes) {
        final ArrayList<Attribute> sortedList = new ArrayList<>(attributes);
        Collections.sort(sortedList, new Comparator<Attribute>() {
            @Override
            public int compare(Attribute o1, Attribute o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        return sortedList;
    }
    
    public List<AttributeKey> sortAttributeKeys(Collection<AttributeKey> attributeKeys) {
        final ArrayList<AttributeKey> sortedList = new ArrayList<>(attributeKeys);
        Collections.sort(sortedList, new Comparator<AttributeKey>() {
            @Override
            public int compare(AttributeKey o1, AttributeKey o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        return sortedList;
    }

    public List<File> getProfileFiles() {
        return profileFiles;
    }

    public void setProfileFiles(List<File> profileFiles) {
        this.profileFiles = profileFiles;
    }

    public List<File> getTemplateFiles() {
        return templateFiles;
    }

    public void setTemplateFiles(List<File> templateFiles) {
        this.templateFiles = templateFiles;
    }

    public List<File> getProfileTemplateFiles() {
        return profileTemplateFiles;
    }

    public void setProfileTemplateFiles(List<File> profileTemplateFiles) {
        this.profileTemplateFiles = profileTemplateFiles;
    }

    public File getTargetDir() {
        return targetDir;
    }

    public void setTargetDir(File targetDir) {
        this.targetDir = targetDir;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public List<File> getUnitTemplateFiles() {
        return unitTemplateFiles;
    }

    public void setUnitTemplateFiles(List<File> unitTemplateFiles) {
        this.unitTemplateFiles = unitTemplateFiles;
    }

    public String append(String s1, String s2) {
        return s1 + s2;
    }

    public int max(int a, int b) {
        return Math.max(a, b);
    }

    public int getCommandWidth(CommandDefinition command) {
        // FIXME-KKL this is not very nice (also dependency wise)
        switch (command.getCommandId()) {
        case CLEAN:
            return 47;
        case INITIALIZE:
            return 61;
        case PREPARE_PERSISTENCE:
            return 130;
        case IMPORT_TEST_DATA:
            return 105;
        case CONFIGURE:
            return 67;
        case DEPLOY:
            return 55;
        case INSTALL:
            return 50;
        case UNINSTALL:
            return 64;
        case IMPORT:
            return 53;
        case VERIFY:
            return 46;
        case BOOTSTRAP:
            return 67;
        case START:
            return 41;
        case STOP:
            return 39;
        case UPLOAD:
            return 55;
        case UPGRADE_RESOURCES:
            return 120;
        default:
            return command.getCommandId().toString().length() * 7;
        }
    }

    public int computeHeight(ConfigurationUnit unit) {
        return Math.max(unit.getProvidedCapabilities().size(), unit.getRequiredCapabilities().size());
    }

    public ProfileLayout computeLayout(Profile profile) {
        return new ProfileLayout(this, profile);
    }

    public String getProperty(Profile profile, Capability capability, String attributeKey) {
        PropertiesHolder ph = getPropertyHolder(profile);
        return ph.getProperty(capability, attributeKey);
    }

    public PropertiesHolder getPropertyHolder(Profile profile) {
        PropertiesHolder ph = profilePropertiesHolderMap.get(profile.getId());
        if (ph == null) {
            ph = profile.createPropertiesHolder(loadDeploymentProperties);
            profile.evaluate(ph);
            profilePropertiesHolderMap.put(profile.getId(), ph);
        }
        return ph;
    }

    public String getUniqueId(ConfigurationUnit unit, CommandDefinition cmd) {
        Validate.notNull(unit, "Unit is null.");
        Validate.notNull(cmd, "Command is null.");

        StringBuilder s = new StringBuilder(unit.getId().toString());
        s.append("/command.").append(cmd.getCommandId());

        return s.toString();
    }

    public String getText(Profile p, ConfigurationUnit unit, CommandDefinition c) {
        if (p == null) {
            throw new IllegalArgumentException("Profile is null");
        }
        if (c == null) {
            throw new IllegalArgumentException(
                    String.format("A command in profile %s is invalid or missing", p.getId()));
        }
        StringBuilder s = new StringBuilder("Command [unique id=");
        s.append(getUniqueId(unit, c)).append("]").append(TOOLTIP_NEWLINE).append("package=").append(c.getPackageId())
                .append(TOOLTIP_NEWLINE);
        for (CapabilityDefinitionReference ref : c.getCapabilities()) {
            s.append("capability=").append(escapeAndTrim(ref.getReferencedCapabilityDefId())).append(TOOLTIP_NEWLINE);
        }
        for (CapabilityDefinitionReference ref : c.getContributions()) {
            s.append("contribution=").append(escapeAndTrim(ref.getReferencedCapabilityDefId())).append(TOOLTIP_NEWLINE);
        }
        for (CapabilityDefinitionReference ref : c.getRequisitions()) {
            s.append("requisition=").append(escapeAndTrim(ref.getReferencedCapabilityDefId())).append(TOOLTIP_NEWLINE);
        }
        return s.toString();
    }

    public String getText(Profile p, Capability c) {
        if (p == null) {
            throw new IllegalArgumentException("Profile is null");
        }
        if (c == null) {
            throw new IllegalArgumentException(
                    String.format("A capability in profile %s is invalid or missing", p.getId()));
        }
        StringBuilder s = new StringBuilder(c.toString());
        s.append(TOOLTIP_NEWLINE);
        for (AttributeKey key : c.getCapabilityDefinition().getAttributeKeys()) {
            s.append(key.getKey()).append("=").append(escapeAndTrim(getProperty(p, c, key.getKey())))
                    .append(TOOLTIP_NEWLINE);
        }
        return s.toString();
    }

    public String getText(Profile p, Capability c, boolean isRequiredCapability) {
        if (p == null) {
            throw new IllegalArgumentException("Profile is null");
        }
        if (c == null) {
            throw new IllegalArgumentException(
                    String.format("A capability in profile %s is invalid or missing", p.getId()));
        }
        if (!isRequiredCapability)
            return getText(p, c);

        StringBuilder s = new StringBuilder(c.toString());
        s.append(TOOLTIP_NEWLINE);

        final Collection<Binding> bindings = p.findBindings(c);

        for (Binding b : bindings) {
            s.append(TOOLTIP_NEWLINE);
            s.append(b);
            s.append(TOOLTIP_NEWLINE);
            c = b.getSourceCapability();
            for (AttributeKey key : c.getCapabilityDefinition().getAttributeKeys()) {
                s.append(key.getKey()).append("=").append(escapeAndTrim(getProperty(p, c, key.getKey())))
                        .append(TOOLTIP_NEWLINE);
            }
        }
        return s.toString();
    }

    public String getText(Profile p, Binding b) {
        if (p == null) {
            throw new IllegalArgumentException("Profile is null");
        }
        if (b == null) {
            throw new IllegalArgumentException(
                    String.format("A binding in profile %s is invalid or missing", p.getId()));
        }

        StringBuilder s = new StringBuilder(b.toString());
        s.append(TOOLTIP_NEWLINE);
        Capability c = b.getSourceCapability();
        for (AttributeKey key : c.getCapabilityDefinition().getAttributeKeys()) {
            s.append(key.getKey()).append("=").append(escapeAndTrim(getProperty(p, c, key.getKey())))
                    .append(TOOLTIP_NEWLINE);
        }
        return s.toString();
    }

    public String getText(Profile profile, ConfigurationUnit unit) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile is null");
        }
        if (unit == null) {
            throw new IllegalArgumentException(
                    String.format("A unit in profile %s is invalid or missing", profile.getId()));
        }
        StringBuilder s = new StringBuilder("Unit [id=" + unit.getUniqueId() + "]");
        s.append(TOOLTIP_NEWLINE);

        PropertiesHolder ph = getPropertyHolder(profile);
        for (Attribute attribute : unit.getAttributes()) {
            s.append(attribute.getKey()).append("=").append(escapeAndTrim(ph.getProperty(unit, attribute.getKey())))
                    .append(TOOLTIP_NEWLINE);
        }
        return s.toString();
    }

    public void setSolutionRootDir(File solutionRootDir) {
        this.solutionRootDir = solutionRootDir;
    }

    public void setLoadDeploymentProperties(boolean loadDeploymentProperties) {
        this.loadDeploymentProperties = loadDeploymentProperties;
    }

    public String getAttributeValue(Profile profile, ConfigurationUnit unit, Attribute attribute) {
        PropertiesHolder propertiesHolder = getPropertyHolder(profile);
        return propertiesHolder.getProperty(unit, attribute.getKey());
    }

    public String getAttributeValue(Profile profile, Capability capability, AttributeKey attribute) {
        PropertiesHolder propertiesHolder = getPropertyHolder(profile);
        return propertiesHolder.getProperty(capability, attribute.getKey());
    }

    public String getAttributeValueOrDefault(Profile profile, Capability capability, AttributeKey attribute) {
        PropertiesHolder propertiesHolder = getPropertyHolder(profile);
        String value = propertiesHolder.getProperty(capability, attribute.getKey());
        
        if (value == null) {
            value = attribute.getDefaultValue();
        }
        return value;
    }

    public String escapeAndTrim(String text) {
        if (text != null) {
            text = text.replaceAll("[\\s]+", " ");
        }
        return escapeXml11(text);
    }

    public String getDescription(ConfigurationUnit configurationUnit, Profile profile) {
        String description = configurationUnit.getDescription();
        if (StringUtils.isBlank(description)) {
            if (configurationUnit.getParentId() != null) {
                ConfigurationUnit parentUnit = profile.findUnit(Id.createUnitId(configurationUnit.getParentId()));
                if (parentUnit != null) {
                    description = parentUnit.getDescription();
                    if (!StringUtils.isBlank(description)) {
                        description = description + "<i>(inherited from " + configurationUnit.getParentId() + ")</i>";
                    } else {
                        description = getDescription(parentUnit, profile);
                    }
                }
            }
        }
        return description;
    }

    public List<Profile> filterProfiles(List<Profile> profiles, String type) {
        List<Profile> filteredProfiles = new ArrayList<>();
        for (Profile profile : profiles) {
            if (profile.getType().name().equals(type)) {
                filteredProfiles.add(profile);
            }
        }
        Collections.sort(filteredProfiles, new Comparator<Profile>() {
            @Override
            public int compare(Profile arg0, Profile arg1) {
                return arg0.getId().compareTo(arg1.getId());
            }
        });
        return filteredProfiles.isEmpty() ? null : filteredProfiles;
    }

    public boolean isIncludeContributionProfiles() {
        return includeContributionProfiles;
    }

    public void setIncludeContributionProfiles(boolean includeContributionProfiles) {
        this.includeContributionProfiles = includeContributionProfiles;
    }

    public boolean isIncludeBaseProfiles() {
        return includeBaseProfiles;
    }

    public void setIncludeBaseProfiles(boolean includeBaseProfiles) {
        this.includeBaseProfiles = includeBaseProfiles;
    }

    public File getSolutionRootDir() {
        return solutionRootDir;
    }
    
    public String getOverwritableProperty(ConfigurationUnit unit, Attribute attribute) {
        return unit.getId().getValue() + "." + attribute.getKey();
    }
    
    public String loadFileContent(String path) {
        File file = new File(path);
        String content;
        try {
            content = FileUtils.readFileToString(file);
        } catch (IOException e) {
            content = "File '" + path + "' not found!";
        }
        
        return escapeXml11(content);
    }

}
