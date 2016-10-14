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
package org.metaeffekt.dcc.commons.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Task enabling to access a property as a variable.
 * 
 * @author Karsten Klein
 */
public class GetPropertyAsVariableTask extends Task {

    private String sourceProperty;

    private String targetProperty;

    /**
     * Executes the task.
     * 
     * @see org.apache.tools.ant.Task#execute()
     */
    @Override
    public void execute() throws BuildException {
        String value = PropertyUtils.getProperty(sourceProperty, null, getProject());
        if (value != null) {
            value = PropertyUtils.getProperty(value, null, getProject());
        }
        if (value != null) {
            PropertyUtils.setProperty(targetProperty, value, PropertyUtils.PROPERTY_PROJECT_LEVEL,
                    getProject());
        }
    }

    public String getSourceProperty() {
        return sourceProperty;
    }

    public void setSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
    }

    public String getTargetProperty() {
        return targetProperty;
    }

    public void setTargetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
    }

}
