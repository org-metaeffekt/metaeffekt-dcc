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
import org.apache.tools.ant.taskdefs.WaitFor;
import org.junit.Test;


public class WaitForTimeoutTest {

    @Test
    public void test() {
        
        final Project project = new Project();
        
        TimeoutCondition timeoutCondition = new TimeoutCondition();
        timeoutCondition.setProject(project);
        timeoutCondition.setStarted(System.currentTimeMillis());
        timeoutCondition.setTimeout(2000);
        
        WaitFor waitFor = new WaitFor();
        waitFor.setProject(project);
        waitFor.add(timeoutCondition);
        waitFor.execute();
    }

}
