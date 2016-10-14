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
package org.metaeffekt.dcc.controller;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Karsten Klein
 */
public class DccControllerConstants {

    public static final String DCC_SHELL_HOME = "dcc.shell.home";
    public static final String DCC_SHELL_VERSION = "1.4";
    
    public static final String DCC_SHELL_SUPPORTED_AGENT_VERSION_PATTERN = "1\\.[2-9][1-9]*\\..*";
    public static final String DCC_SHELL_RECOMMENED_AGENT_VERSION = "1.4.0";

    public static final Set<String> DCC_SHELL_WARN_AGENT_VERSIONS = new HashSet<>();
    
    static {
        DCC_SHELL_WARN_AGENT_VERSIONS.add("1.2.0");
    }

}
