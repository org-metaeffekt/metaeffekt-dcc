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
package org.metaeffekt.dcc.commons;

import org.metaeffekt.core.commons.annotation.Public;

@Public
public final class DccConstants {

    public static final String ANT_FILE = "ant.file";
    public static final String ANT_PROJECT_HELPER = "ant.projectHelper";

    public static final String CAPABILITY = "capability";
    public static final String CONTRIBUTION = "contribution";
    public static final String REQUISITION = "requisition";
    public static final String PROVISION = "provision";
    public static final String PACKAGE = "package";
    public static final String HOST_CAPABILITY = "agent.endpoint";

    public static final String CONFIG_SUB_DIRECTORY = "config";
    public static final String PACKAGES_SUB_DIRECTORY = "packages";
    public static final String SCRIPTS_SUB_DIRECTORY = "scripts";
    public static final String HOOKS_SUB_DIRECTORY = "hooks";
    public static final String LIB_SUB_DIRECTORY = "lib";

    public static final String WORK_SUB_DIRECTORY = "work";
    public static final String STATE_CACHE_SUB_DIRECTORY = "work/state";
    public static final String TMP_SUB_DIRECTORY = "work/tmp";

    public static final String COMMAND_PROPERTIES = "command-properties";
    public static final String PREREQUISITES_PROPERTIES = "prerequisites-properties";

    public static final String PREREQUISITES_PROPERTIES_FILE_NAME = "prerequisites.properties";

    public static final String SEPARATOR_UNIT_KEY = ".";

    public static final String SCRIPT_EXTENSION = ".xml";
    
    public static final String PROPERTIES_SEPARATOR = 
        "####----------------------------------------------------------------------####";

    private DccConstants() {
        super();
    }

}
