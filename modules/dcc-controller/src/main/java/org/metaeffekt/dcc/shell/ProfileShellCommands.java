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
package org.metaeffekt.dcc.shell;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.spring.xml.ProfileParser;
import org.metaeffekt.dcc.controller.DccControllerConstants;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;

/**
 * {@link CommandMarker} instance which holds the default commands that are available when you 
 * first start the shell ({@code select} and {@code deselect} {@code profile}). 
 */
@Component
public class ProfileShellCommands implements CommandMarker {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileShellCommands.class);

    private DccPromptProvider prompt;
    
    private ExecutionContext executionContext;
    
    @Autowired
    public ProfileShellCommands(DccPromptProvider prompt, ExecutionContext executionContext) {
        super();
        this.prompt = prompt;
        this.executionContext = executionContext;
    }

    @CliAvailabilityIndicator({"select profile"})
    public boolean isSelectProfileAvailable() {
        // select profile is always possible. Even re-selection.
        return true;
    }
    
    @CliAvailabilityIndicator({"deselect profile"})
    public boolean isDeSelectProfileAvailable() {
        return executionContext.containsProfile();
    }

    @CliCommand(value = "select profile", help = "Load the profile from the given file.")
    public void selectProfile(@CliOption(key = "location", mandatory = true, help = "The properties file to load") File location) {
        resetProfileContext();
        try {
            final File solutionDir = new File(System.getProperty(DccControllerConstants.DCC_SHELL_HOME));
            Profile profile = ProfileParser.parse(location, solutionDir);
            setLoadedProfile(profile);
            LOG.info("Profile at [" + fileDisplay(location, profile) + "] successfully loaded.");
            LOG.info("  Profile id:            [" + profile.getId() + "]");
            LOG.info("  Solution properties:   [" + fileDisplay(profile.getSolutionPropertiesFile(), profile) + "]");
            LOG.info("  Deployment properties: [" + fileDisplay(profile.getDeploymentPropertiesFile(), profile) + "]");
            LOG.info("  Deployment id:         [" + profile.getDeploymentId() + "]");
            executionContext.setSolutionDir(solutionDir);
            
            if (profile.getType() != Profile.Type.DEPLOYMENT) {
                LOG.warn("The selected profile is NOT a deployment profile. Execute commands will not be available.");
            }
        } catch (Exception e) {
            LOG.error(String.format("Profile at [%s] could not be loaded. Error message: %n%n%s%n%n", location.getPath(), e.getMessage()));
            LOG.debug(e.getMessage(), e);
            setLoadedProfile(null);
            
            // FIXME: revise all commands and decide whether they have to fail
            // FIXME: refactor (common base class) before copying everywhere
            if (executionContext.isFailOnError()) {
                LOG.error("Aborting shell execution ...", e);
                // that seems a bit harsh ... but the spring shell is surprisingly robust :-)
                System.exit(1);
            } else {
                LOG.debug(String.format("Continuing execution. Ignoring [%s] profile load exception.", location.getPath()), e);
            }
            
        }
    }
    
    private String fileDisplay(File file, Profile profile) {
        if (file == null) {
            return "-not available-";
        }
        return profile.getRelativePath(file);
    }

    private void setLoadedProfile(Profile profile) {
        if (profile != null) {
            executionContext.setProfile(profile);
            StringBuilder sb = new StringBuilder();
            sb.append(DccPromptProvider.DCC_PROMPT).append(DccPromptProvider.DCC_PROMPT_SEPARATOR);
            sb.append(profile.getId());
            prompt.setPrompt(sb.toString());
        } else {
            prompt.resetPrompt();
            executionContext.setProfile(null);
        }
    }
    
    @CliCommand(value = "deselect profile", help = "Deselect the profile.")
    public void unloadProfile(@CliOption(key = "profileId", mandatory = false, help = "Id of the profile to unload") String profileId) {
        LOG.info("Unloading profile with id [" + executionContext.getProfile().getId() + "].");
        resetProfileContext();
    }

    protected void resetProfileContext() {
        executionContext.resetContext();
        prompt.resetPrompt();
    }
    
}
