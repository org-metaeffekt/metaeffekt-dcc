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

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class CanonicalizePathTask extends Task {

    private String pathProperty;

    /**
     * Executes the task.
     * 
     * @see org.apache.tools.ant.Task#execute()
     */
    @Override
    public void execute() {
        try {
            String value = PropertyUtils.getProperty(pathProperty, null, getProject());
            
            if (value != null) {
                File file = new File(value);
                File canonicalFile = file.getCanonicalFile();
                
                String canonicalPath = canonicalFile.getAbsolutePath();
                canonicalPath = canonicalPath.replace("\\", "/");
    
                // overwrite the property in place:
                PropertyUtils.setProperty(pathProperty, 
                        canonicalPath, PropertyUtils.PROPERTY_PROJECT_LEVEL, getProject());
            } else {
                getProject().log(String.format("Property '%s' does not contain any path information.", 
                    pathProperty), Project.MSG_VERBOSE);
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    public String getPathProperty() {
        return pathProperty;
    }

    public void setPathProperty(String pathProperty) {
        this.pathProperty = pathProperty;
    }

}
