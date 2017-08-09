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

/**
 * Simple Java class to exit with the returnCode given as argument to verify
 * calling script behaviour (bash / bat).
 * @author Jochen K.
 */
public class SystemExiter {

    public static void main(String[] args) {
        if ((args[0] == null || args[0].length() < 1)) {
            throw new IllegalArgumentException("No designatedExitCode given.");
        }

        int designatedExitCode = Integer.parseInt(args[0]);
        System.exit(designatedExitCode);
    }
}
