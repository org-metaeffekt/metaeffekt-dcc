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

import org.apache.tools.ant.Project;

import org.metaeffekt.core.common.kernel.ant.log.LoggingProjectAdapter;

/**
 * <p>
 * The {@link ProjectAdapter} bridges Ant and Commons Logging in 
 * respect to harmonize logging.
 * 
 * Since Ant and Commons Logging are not equivalent and specific to Ant tasks
 * an escalation mechanism is supported. In particular when Ant logs to
 * verbose level the message can be escalated to another log level and
 * the build can be caused to fail.
 * </p>
 * 
 * @author Karsten Klein
 */
public class ProjectAdapter extends LoggingProjectAdapter {

    protected int modulateMessageLevel(String message, int msgLevel) {
        // move strange ant log to debug level
        if (message.startsWith("Result: ")) {
            msgLevel = Project.MSG_DEBUG;
        }
        return msgLevel;
    }

    @Override
    public void log(String message, int msgLevel) {
        msgLevel = modulateMessageLevel(message, msgLevel);
        super.log(message, msgLevel);
    }

    @Override
    public void log(String message, Throwable throwable, int msgLevel) {
        msgLevel = modulateMessageLevel(message, msgLevel);
        super.log(message, throwable, msgLevel);
    }

    @Override
    public void log(String message) {
        log(message, Project.MSG_DEBUG);
    }

}
