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
package org.metaeffekt.dcc.commons.spring.xml;

import java.io.File;

import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class ProfileApplicationContext extends FileSystemXmlApplicationContext {

    private File solutionDir;
    
    public ProfileApplicationContext(File profileFile, File solutionDir) {
        super(new String[] {profileFile.getPath()}, false);
        this.solutionDir = solutionDir;
        super.refresh();
    }

    @Override
    protected Resource getResourceByPath(String path) {
        return new FileSystemResource(path);
    }

    public File getSolutionDir() {
        return solutionDir;
    }

    public void setSolutionDir(File solutionDir) {
        this.solutionDir = solutionDir;
    }

}
