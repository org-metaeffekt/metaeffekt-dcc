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
package org.metaeffekt.dcc.commons.commands;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Alexander D.
 */
public enum Commands {

    // host-level commands
    CLEAN(false),
    EVALUATE_TEMPLATES(false),
    INITIALIZE,
    PURGE(false),

    // unit-level (named) commands
    INITIALIZE_RESOURCES,
    INSTALL,
    CONFIGURE,
    DEPLOY,
    PREPARE_PERSISTENCE,
    BOOTSTRAP,
    IMPORT,
    IMPORT_TEST_DATA,
    VERIFY,
    START,
    UPLOAD,
    RECONFIGURE,
    STOP,
    UNINSTALL(false),
    UPGRADE_RESOURCES,
    UPGRADE_INITIALIZE,
    UPGRADE_PREPARE,
    UPGRADE,
    UPGRADE_FINALIZE,

    // other (management) commands 
    LOGS(false),
    STATE(false),
    VERSION(false);

    public static final Set<Commands> CONFIGURABLE_COMMANDS = Collections.unmodifiableSet(EnumSet.of(
            CLEAN, INITIALIZE_RESOURCES, INITIALIZE, INSTALL,
            CONFIGURE, DEPLOY, PREPARE_PERSISTENCE,
            BOOTSTRAP, IMPORT, IMPORT_TEST_DATA, VERIFY,
            START, UPLOAD, RECONFIGURE, STOP, UNINSTALL,
            UPGRADE_RESOURCES, UPGRADE_INITIALIZE, 
            UPGRADE_PREPARE, UPGRADE, UPGRADE_FINALIZE));

    private static final Map<String, Commands> CONFIGURABLE_COMMANDS_MAPPING =
        new HashMap<String, Commands>();

    static {
        for (Commands command : CONFIGURABLE_COMMANDS) {
            CONFIGURABLE_COMMANDS_MAPPING.put(command.name(), command);
        }
    }
    
    private final boolean persistentState;
    
    private Commands() {
        this.persistentState = true;
    }

    private Commands(boolean peristentState) {
        this.persistentState = peristentState;
    }

    @Override
    public String toString() {
        return name().toLowerCase().replaceAll("_", "-");
    }

    public static Commands parseConfigurableCommand(String commandValue) {
        Commands parsedCommand = null;

        if (!StringUtils.isEmpty(commandValue)) {
            parsedCommand =
                CONFIGURABLE_COMMANDS_MAPPING.get(commandValue.toUpperCase().replace('-', '_'));
        }

        return parsedCommand;
    }

    public boolean persistentState() {
        return persistentState;
    }

}
