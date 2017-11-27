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

import java.util.regex.Pattern;

import org.metaeffekt.core.commons.annotation.Public;

@Public
public final class DccProperties {

    public static final String DCC_EXECUTION_PROPERTIES = "dcc.execution.properties";
    public static final String DCC_PREREQUISITES_PROPERTIES = "dcc.prerequisites.properties";
    public static final String DCC_UPGRADE_PROPERTIES = "dcc.upgrade.properties";

    // NOTE: base dir properties are not exposed to script, but used in shell/agent configuration
    public static final String DCC_SOLUTION_BASE_DIR = "dcc.solution.base.dir";

    public static final String DCC_TARGET_BASE_DIR = "dcc.target.base.dir";

    public static final String DCC_SOLUTION_DIR = "dcc.solution.dir";

    public static final String DCC_WORK_DIR = "dcc.work.dir";

    public static final String DCC_TARGET_DIR = "dcc.target.dir";

    public static final String DCC_COMMAND = "dcc.command";

    public static final String DCC_UNIT_ID = "dcc.unit.id";

    public static final String DCC_PACKAGE_ID = "dcc.package.id";

    public static final String DCC_PACKAGE_DIR = "dcc.package.dir";

    public static final String DCC_JAVA_HOME = "dcc.java.home";

    /**
     * Used in two circumstances:
     * 1) to enable overwriting the profile set deployment id in the deployment properties
     * 2) to transport the deployment id to the forked ant executor
     */
    public static final String DCC_DEPLOYMENT_ID = "dcc.deployment.id";

    public static final String DCC_AGENT_HOST_NAME = "name";

    public static final String DCC_AGENT_PORT = "port";

    private static final String DCC_UNIT_CONTRIUBTION_PREFIX = "dcc.unit.contribution";

    private static final String DCC_UNIT_REQUISITION_PREFIX = "dcc.unit.requisition";

    private static final String DCC_UNIT_PROVISION_PREFIX = "dcc.unit.provision";

    private static final String ITERATOR_SEQUENCE_SUFFIX = "iterator.sequence";

    public static final String DCC_UNIT_CONTRIBUTION_ITERATOR_SEQUENCE_TEMPLATE =
        DCC_UNIT_CONTRIUBTION_PREFIX + ".%s." + ITERATOR_SEQUENCE_SUFFIX;

    public static final Pattern DCC_UNIT_CONTRIBUTION_ITERATOR_SEQUENCE_REGEX =
        Pattern.compile(DCC_UNIT_CONTRIUBTION_PREFIX + "\\.(.+)\\." + ITERATOR_SEQUENCE_SUFFIX);

    public static final String DCC_UNIT_REQUISITION_ITERATOR_SEQUENCE_TEMPLATE =
        DCC_UNIT_REQUISITION_PREFIX + ".%s." + ITERATOR_SEQUENCE_SUFFIX;

    public static final Pattern DCC_UNIT_REQUISITION_ITERATOR_SEQUENCE_REGEX =
        Pattern.compile(DCC_UNIT_REQUISITION_PREFIX + "\\.(.+)\\." + ITERATOR_SEQUENCE_SUFFIX);

    public static final String DCC_UNIT_CONTRIBUTION_ITERATOR_SEQUENCE =
        DCC_UNIT_CONTRIUBTION_PREFIX + "." + ITERATOR_SEQUENCE_SUFFIX;

    public static final String DCC_UNIT_REQUISITION_ITERATOR_SEQUENCE =
        DCC_UNIT_REQUISITION_PREFIX + "." + ITERATOR_SEQUENCE_SUFFIX;

    public static final String DCC_UNIT_PROVISION_ITERATOR_SEQUENCE =
        DCC_UNIT_PROVISION_PREFIX + "." + ITERATOR_SEQUENCE_SUFFIX;

    public static final String DCC_UNIT_PROVISION_ITERATOR_SEQUENCE_TEMPLATE =
        DCC_UNIT_PROVISION_PREFIX + ".%s." + ITERATOR_SEQUENCE_SUFFIX;

    public static final Pattern DCC_UNIT_PROVISION_ITERATOR_SEQUENCE_REGEX =
        Pattern.compile(DCC_UNIT_PROVISION_PREFIX + "\\.(.+)\\." + ITERATOR_SEQUENCE_SUFFIX);

    public static final String DCC_LOCAL_DESTINATION_DIR = "dcc.local.destination.dir";

    public static final String DCC_SYSTEM_PROPERTY_PROFILE_VALIDATION = "dcc.profile.validation";
    
    public static String[] DCC_DEPLOYMENT_PROPERTIES_WHITELIST = new String[] {
            DCC_LOCAL_DESTINATION_DIR,
            DCC_DEPLOYMENT_ID,
    };
    
    public static String[] DCC_SOLUTION_PROPERTIES_WHITELIST = new String[] {
            DCC_LOCAL_DESTINATION_DIR,
            DCC_DEPLOYMENT_ID
    };

    private DccProperties() {
        super();
    }

}
