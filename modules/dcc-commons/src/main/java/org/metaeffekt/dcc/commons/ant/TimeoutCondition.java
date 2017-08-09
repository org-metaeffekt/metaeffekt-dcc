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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;


/**
 * Condition that allows to implement a timeout without using the WaitFor task. The WaitFor task
 * is currently too limited, as it does not allow to reevaluate the boundary conditions of a check.
 * 
 * @author Karsten Klein
 */
public class TimeoutCondition extends ConditionBase implements Condition {
    
    private long timeout;

    private long started;
    
    public long getTimeout() {
        return timeout;
    }
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    public long getStarted() {
        return started;
    }
    
    public void setStarted(long initialTime) {
        this.started = initialTime;
    }

    @Override
    public boolean eval() throws BuildException {
        // returns false as long as the timeout period was not exceeded.
        long currentTime = System.currentTimeMillis();
        return started + timeout < currentTime;
    }

}
