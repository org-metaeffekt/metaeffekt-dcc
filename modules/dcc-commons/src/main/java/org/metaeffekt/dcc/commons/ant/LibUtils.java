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

import org.apache.tools.ant.DirectoryScanner;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LibUtils {

    public static void parse(String line, Map<String, String> fileArtifactIdMap, Map<String, Set<String>> artifactPatternGroupMap) {
        String artifactFile = extractFile(line);
        if (artifactFile != null) {
            fileArtifactIdMap.put(artifactFile, line.trim());
        }

        String artifactPattern = extractFilePattern(line);
        if (artifactPattern != null) {
            Set<String> set = artifactPatternGroupMap.get(artifactPattern);
            if (set == null) {
                set = new HashSet<>();
            }
            set.add(artifactFile);
            artifactPatternGroupMap.put(artifactPattern, set);
        }
    }

    public static String extractFile(String line) {
        String artifactFile = null;
        String[] split = line.split(":");
        if (validateMetaDataFormat(line, split)) {
            if (split.length == 4) {
                artifactFile = split[1] + "-" + split[3] + "." + split[2];
            } else  {
                artifactFile = split[1] + "-" + split[4] + "-" + split[3] + "." + split[2];
            }
        }
        return artifactFile;
    }

    public static String extractFilePattern(String line) {
        String artifactPattern = null;
        String[] split = line.trim().split(":");
        if (validateMetaDataFormat(line, split)) {
            final String version = "*";
            if (split.length == 4) {
                artifactPattern = split[1] + "-" + version + "." + split[2];
            } else {
                artifactPattern = split[1] + "-" + version + "-" + split[3] + "." + split[2];
            }
        }
        return artifactPattern;
    }

    public static String extractVersion(String line) {
        String version = null;
        final String[] split = line.trim().split(":");
        if (validateMetaDataFormat(line, split)) {
            if (split.length == 4) {
                version = split[3];
            } else {
                version = split[4];
            }
        }
        return version;
    }

    private static boolean validateMetaDataFormat(String line, String[] split) {
        if (split.length > 1) {
            if (!(split.length == 4 || split.length == 5)) {
                throw new IllegalStateException("Meta data format is not as expected: " + line.trim());
            }
            return true;
        }
        return false;
    }
    
    public static String[] scanFiles(final File baseDir, final String pattern) {
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(baseDir);
        directoryScanner.setIncludes(new String[]{pattern});
        directoryScanner.scan();
        String[] files = directoryScanner.getIncludedFiles();
        return files;
    }

}
