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
import org.apache.tools.ant.Task;

/**
 * Another class to compensate the lack of reevaluating the boundary conditions of the WaitFor task.
 * The Task produces content for given properties that can be further evaluated:
 * <ol>
 *      <li>the timestamp of the current time (to be used as reference for timeouts)</li>
 *      <li>an iterator sequence of indexes for the individual retries. The sequence has
 *          the length (timeout / interval)</li>
 * </ol>
 * 
 * @author Karsten Klein
 */
// FIXME-KKL part of this class (iterator sequence are definitly a workaround. Perhaps we should
//     find a better solution for this problem. 
public class Timestamp extends Task {

    private long interval = 1000;

    private long timeout = 20000;

    private String timestampPropertyName = "timestamp";

    private String iteratorPropertyName = null;

    @Override
    public void execute() throws BuildException {
        super.execute();

        long currentTime = System.currentTimeMillis();
        
        // set the property with name timestampPropertyName
        getProject().setProperty(timestampPropertyName, String.valueOf(currentTime));

        // the iterator sequence is options
        if (iteratorPropertyName != null) {
            long elements = timeout / interval;
            if (elements < 0) {
                elements = 1;
            }
            
            // limit upper bound
            if (elements > 1000) {
                elements = 1000;
            }
            
            StringBuilder sb = new StringBuilder();
            for (long i = 0; i < elements; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(i + 1);
            }
            
            getProject().setProperty(iteratorPropertyName, sb.toString());
        }
    }

    public String getPropertyName() {
        return timestampPropertyName;
    }

    public void setPropertyName(String propertyName) {
        this.timestampPropertyName = propertyName;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getTimestampPropertyName() {
        return timestampPropertyName;
    }

    public void setTimestampPropertyName(String timestampPropertyName) {
        this.timestampPropertyName = timestampPropertyName;
    }

    public String getIteratorPropertyName() {
        return iteratorPropertyName;
    }

    public void setIteratorPropertyName(String iteratorPropertyName) {
        this.iteratorPropertyName = iteratorPropertyName;
    }

}
