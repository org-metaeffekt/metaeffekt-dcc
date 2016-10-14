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

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;

import org.springframework.core.io.UrlResource;

public class UrlUtils {

    public String asUrl(String filePath) throws IOException {
        UrlResource urlResource = new UrlResource("file:" + filePath);
        return urlResource.getURL().toExternalForm();
    }

    public String asRelativePath(String workingDirPath, String filePath) throws IOException {
        File file = new File(filePath).getCanonicalFile();
        File workingDirFile = new File(workingDirPath).getCanonicalFile();
        return asRelativePath(workingDirFile, file);
    }

    public String asRelativePath(File workingDirFile, File file) throws IOException {
        final LinkedHashSet<File> set = new LinkedHashSet<>();
        File commonBaseDir = workingDirFile;
        set.add(commonBaseDir);
        
        // decompose the working dir in separate files
        while (commonBaseDir.getParentFile() != null) {
            set.add(commonBaseDir.getParentFile());
            commonBaseDir = commonBaseDir.getParentFile();
        }
        
        // walk down the file path until common base dir is found
        commonBaseDir = file;
        String path = "";
        while (commonBaseDir != null && !set.contains(commonBaseDir)) {
            if (path.length() > 0) {
                path = commonBaseDir.getName() + "/" + path;
            } else {
                path = commonBaseDir.getName();
            }
            commonBaseDir = commonBaseDir.getParentFile();
        }
        
        // see on which index the common base path lies
        int index = 0;
        for (File pos : set) {
            if (pos.equals(commonBaseDir)) {
                break;
            }
            index ++;
        }
        
        // move up the path until common base path is reached
        String relativePath = "";
        for (int i = 0; i < index; i++) {
            relativePath = "../" + relativePath;
        }
        
        // and append the path composed earlier
        return relativePath + path;
    }
    
    public String asRelativeUrl(String workingDirPath, String filePath) throws IOException {
        return asUrl(asRelativePath(workingDirPath, filePath));
    }
    
}
