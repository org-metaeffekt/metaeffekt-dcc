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
package org.metaeffekt.dcc.commons.execution;

import java.io.File;

import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;

/**
 * Executes command on a given {@link ConfigurationUnit} instance.
 */
public interface Executor {

    String getDisplayName();

    void evaluateTemplateResources();
    void initializeResources(ConfigurationUnit unit);
 
    void initialize();
    void clean();
    void purge();
    
    void initializeUpgrade();
    void upgradeResources(ConfigurationUnit unit, File upgradePropertiesFile);

    boolean hostAvailable();
    void execute(Commands command, ConfigurationUnit unit);
    void retrieveUpdatedState();
    void retrieveLogs();

}