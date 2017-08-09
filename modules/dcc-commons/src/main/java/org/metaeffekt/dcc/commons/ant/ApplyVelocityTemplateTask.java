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

import static org.metaeffekt.dcc.commons.DccProperties.DCC_UNIT_CONTRIBUTION_ITERATOR_SEQUENCE_REGEX;
import static org.metaeffekt.dcc.commons.DccProperties.DCC_UNIT_PROVISION_ITERATOR_SEQUENCE_REGEX;
import static org.metaeffekt.dcc.commons.DccProperties.DCC_UNIT_REQUISITION_ITERATOR_SEQUENCE_REGEX;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.texen.util.FileUtil;

import org.metaeffekt.dcc.commons.DccProperties;

/**
 * Applies a velocity template to a given input properties file. The task supports an additional
 * iterator sequence to support an iterator in the velocity template independent from the data
 * inside the property file.
 * 
 * @author Karsten Klein
 * @author Alexander D.
 */
public class ApplyVelocityTemplateTask extends Task {

    private static final String ENCODING_UTF_8 = "UTF-8";

    private File propertyFile;

    private File templateFile;

    private File targetFile;

    private boolean enableLogging = false;

    @Override
    public void execute() throws BuildException {
        notNull(propertyFile, "propertyFile");
        notNull(templateFile, "templateFile");
        notNull(targetFile, "targetFile");

        Properties properties = PropertyUtils.loadEnrichedProperties(getProject(), propertyFile);
        
        String contributionIteratorSequence =
            properties.getProperty(DccProperties.DCC_UNIT_CONTRIBUTION_ITERATOR_SEQUENCE);
        String requisitionIteratorSequence =
            properties.getProperty(DccProperties.DCC_UNIT_REQUISITION_ITERATOR_SEQUENCE);
        String provisionIteratorSequence =
                properties.getProperty(DccProperties.DCC_UNIT_PROVISION_ITERATOR_SEQUENCE);

        Properties velocityProperties = new Properties();
        velocityProperties.setProperty(Velocity.RESOURCE_LOADER, "file");
        velocityProperties.setProperty("file.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        velocityProperties.setProperty(Velocity.FILE_RESOURCE_LOADER_CACHE,
                Boolean.FALSE.toString());
        velocityProperties.setProperty(Velocity.INPUT_ENCODING, ENCODING_UTF_8);
        velocityProperties.setProperty(Velocity.OUTPUT_ENCODING, ENCODING_UTF_8);
        if (!enableLogging) {
            velocityProperties.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS,
                    "org.apache.velocity.runtime.log.NullLogChute");
        }

        String templatePath = templateFile.getParentFile().getAbsolutePath();
        velocityProperties.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, templatePath);

        VelocityEngine velocityEngine = new VelocityEngine(velocityProperties);

        Template template = velocityEngine.getTemplate(templateFile.getName());
        VelocityContext context = new VelocityContext();
        context.put("system", System.getProperties());
        context.put("input", properties);
        context.put("project", getProject());
        context.put("contributionIteratorSequence",
                deserializeSequence(contributionIteratorSequence));
        context.put("requisitionIteratorSequence", deserializeSequence(requisitionIteratorSequence));
        context.put("provisionIteratorSequence", deserializeSequence(provisionIteratorSequence));
        context.put("StringUtils", StringUtils.class);
        context.put("FileUtil", FileUtil.class);
        context.put("UrlUtils", new UrlUtils());
        context.put("xpath", new XPathProvider());
        context.put("PropertyUtils", new PropertyUtils());

        addConcreteExtensionSequence(DCC_UNIT_CONTRIBUTION_ITERATOR_SEQUENCE_REGEX, context,
                "ContributionIteratorSequence", properties);
        addConcreteExtensionSequence(DCC_UNIT_REQUISITION_ITERATOR_SEQUENCE_REGEX, context,
                "RequisitionIteratorSequence", properties);
        addConcreteExtensionSequence(DCC_UNIT_PROVISION_ITERATOR_SEQUENCE_REGEX, context,
                "ProvisionIteratorSequence", properties);

        targetFile.getParentFile().mkdirs();
        try (Writer writer =
            new OutputStreamWriter(new FileOutputStream(targetFile), ENCODING_UTF_8)) {
            template.merge(context, writer);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    private void addConcreteExtensionSequence(Pattern extensionPattern,
            VelocityContext velocityContext, String suffix, Properties inputProperties) {

        final Set<String> existingKeys = new HashSet<>();
        for (Enumeration<?> enumeration = inputProperties.propertyNames(); enumeration
                .hasMoreElements();) {
            final String key = (String) enumeration.nextElement();
            final Matcher matcher = extensionPattern.matcher(key);
            if (matcher.matches()) {
                final List<String> sequence = deserializeSequence(inputProperties.getProperty(key));
                final String extensionName = matcher.group(1);
                String variableName =
                    removeIllegalCharactersFromVariableName(extensionName + suffix);
                if (existingKeys.contains(variableName)) {
                    variableName = generateUniqueName(variableName, existingKeys);
                }
                existingKeys.add(variableName);
                velocityContext.put(variableName, sequence);
            } else {
                existingKeys.add(key);
            }
        }
    }

    private String generateUniqueName(String name, Set<String> existingKeys) {
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            final String newName = name + "_" + i;
            if (!existingKeys.contains(newName)) {
                return newName;
            }
        }

        throw new IllegalStateException("Failed to generate unique name : " + name);
    }

    private static String removeIllegalCharactersFromVariableName(String variableName) {
        final char[] charArray = new char[variableName.length()];
        boolean toUpperCase = false;
        int j = 0;
        for (int i = 0; i < variableName.length(); i++) {
            final char current = variableName.charAt(i);
            final boolean isValidLetter =
                (current >= 'a' && current <= 'z') || (current >= 'A' && current <= 'Z');
            final boolean isValidDigit = (current >= '0' && current <= '9');
            final boolean isValidSeparator = (current == '-' && current <= '_');
            if (!((j == 0 && isValidLetter) || (j != 0 && (isValidLetter || isValidDigit || isValidSeparator)))) {
                toUpperCase = true;
            } else if (toUpperCase && !isValidDigit && !isValidSeparator && j != 0) {
                charArray[j] = Character.toUpperCase(current);
                toUpperCase = false;
                j++;
            } else {
                charArray[j] = current;
                j++;
            }
        }

        return new String(charArray, 0, j);
    }

    private List<String> deserializeSequence(String iteratorSequence) {
        if (iteratorSequence == null) {
            return Collections.emptyList();
        }
        String[] items = iteratorSequence.split(",");
        List<String> list = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null && !items[i].trim().isEmpty()) {
                list.add(items[i].trim());
            }
        }
        return list;
    }

    private <T> T notNull(T parameter, String label) {
        if (parameter == null) {
            throw new IllegalArgumentException(label + " property not set");
        }
        return parameter;
    }

    public File getPropertyFile() {
        return propertyFile;
    }

    public void setPropertyFile(File propertyFile) {
        this.propertyFile = propertyFile;
    }

    public File getTemplateFile() {
        return templateFile;
    }

    public void setTemplateFile(File templateFile) {
        this.templateFile = templateFile;
    }

    public File getTargetFile() {
        return targetFile;
    }

    public void setTargetFile(File targetFile) {
        this.targetFile = targetFile;
    }

    public boolean isEnableLogging() {
        return enableLogging;
    }

    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }
}
