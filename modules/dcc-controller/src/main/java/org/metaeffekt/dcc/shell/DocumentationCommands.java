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
package org.metaeffekt.dcc.shell;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import org.metaeffekt.dcc.controller.DccControllerConstants;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;
import org.metaeffekt.dcc.docgenerator.DocGenerator;

/**
 * Documentation commands.
 * 
 * @author Karsten Klein
 */
@Component
public class DocumentationCommands implements CommandMarker {
    
    private static final Logger LOG = LoggerFactory.getLogger(DocumentationCommands.class);

    @Autowired
    public DocumentationCommands(ExecutionContext executionContext) {
        super();
    }

    @CliAvailabilityIndicator({ "documentation update", "documentation open" })
    public boolean arePropertiesAvailable() {
        return true;
    }

    @CliCommand(value = "documentation update", 
        help = "Updates the documentation of the profiles included in the solution. Execute when profiles have been modified to reflect the changes.")
    public void updateDocumentation(
            @CliOption(key = "includeBaseProfiles", mandatory = false, 
                unspecifiedDefaultValue="false", specifiedDefaultValue="true",
                help = "Flag to indicate that base profiles should be included in the generated documentation.") 
                boolean includeBaseProfiles,
            @CliOption(key = "includeContributionProfiles", mandatory = false, 
                unspecifiedDefaultValue="false", specifiedDefaultValue="true",
                help = "Flag to indicate that base profiles should be included in the generated documentation.") 
                boolean includeContributionProfiles,
            @CliOption(key = "includedFiles", mandatory = false, 
                unspecifiedDefaultValue="**/*-profile.xml, **/profile.xml, **/*-contribution.xml",
                help = "Input for profile file scan. Uses Ant-style file pattern to specify the files included in the scan.") 
                String scanIncludedFiles,
            @CliOption(key = "excludedFiles", mandatory = false, 
                unspecifiedDefaultValue="-empty-",
                help = "Input for profile file scan. Uses Ant-style file pattern to specify the files exluded from the scan.") 
                String scanExcludedFiles
            ) {
        DocGenerator docGenerator = new DocGenerator();
        
        DirectoryScanner scanner = new DirectoryScanner();
        final File solutionRootDir = new File(System.getProperty(DccControllerConstants.DCC_SHELL_HOME));

        LOG.info("Scanning [{}] for profiles...", solutionRootDir);
        scanner.setBasedir(solutionRootDir);
        scanner.setIncludes(scanIncludedFiles.split(","));
        scanner.setExcludes(scanExcludedFiles.split(","));
        scanner.scan();

        List<File> profileFiles = new ArrayList<>();
        String[] found = scanner.getIncludedFiles();
        for (String relativePath : found) {
            final File profileFile = new File(solutionRootDir, relativePath);
            profileFiles.add(profileFile);
            LOG.debug("Found profile file: [{}]", profileFile);
        }
        docGenerator.setProfileFiles(profileFiles);
        docGenerator.setTargetDir(new File(solutionRootDir, "doc"));
        docGenerator.setSolutionRootDir(solutionRootDir);
        docGenerator.setPackageId(solutionRootDir.getName());
        docGenerator.setIncludeBaseProfiles(includeBaseProfiles);
        docGenerator.setIncludeContributionProfiles(includeContributionProfiles);
        docGenerator.generate();
        LOG.info("HTML Documentation in [{}] updated.", docGenerator.getTargetDir());
    }

    @CliCommand(value = "documentation open", help = "Opens the system default browser with a documentation link.")
    public void openDocumentation() throws IOException, URISyntaxException {
        try {
            if (Desktop.isDesktopSupported()) {
                final File solutionRootDir = new File(System.getProperty(DccControllerConstants.DCC_SHELL_HOME));
                Desktop.getDesktop().browse(new File(solutionRootDir, "doc/index.html").toURI());
            } else {
                LOG.info("Native shell does not support opening a browser.");
            }
        } catch (Exception e) {
            LOG.error("Cannot open documentation!", e);
        }
    }

}
