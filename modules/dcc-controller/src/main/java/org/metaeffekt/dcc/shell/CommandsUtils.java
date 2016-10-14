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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;

public class CommandsUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CommandsUtils.class);
    /**
     * Retrieves the profile from the executionContext. If the profile is null then it prints 
     * a suitable message to the console. NOTE: this method could return {@code null}, so calling 
     * methods you have been warned and need to check.
     */
    public static Profile retrieveProfile(ExecutionContext executionContext) {
        Profile profile = executionContext.getProfile();
        if (profile == null) {
            LOG.info("No profile currently selected.");
        }
        return profile;
    }
    
}
