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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Scans metaDataDir for lib.metadata files and consolidates the libDir with the found information.
 * 
 * Consolidation currently anticipates to remove older artifact versions.
 * 
 * @author Karsten Klein
 */
public class ConsolidateLibsTask extends Task {

    private static final char _CHAR_BEFORE_DOT = '.' - 1;
    private static final String CHAR_BEFORE_DOT = "" + _CHAR_BEFORE_DOT;

    private File metaDataDir;
    
    private File libDir;
    
    @Override
    public void execute() throws BuildException {
        super.execute();
        
        String[] libFiles = LibUtils.scanFiles(libDir, "*.jar");
        String[] metaDataFiles = LibUtils.scanFiles(metaDataDir, "**/lib.metadata");

        Map<String, String> fileToArtifactMap = new HashMap<>();
        Map<String, Set<String>> artifactPatternToGroupMap = new HashMap<>();
        for (String file : metaDataFiles) {
            try (FileReader fileReader = new FileReader(new File(metaDataDir, file))) {
                BufferedReader reader = new BufferedReader(fileReader);
                reader.lines().forEach(l -> LibUtils.parse(l, fileToArtifactMap, artifactPatternToGroupMap));
            } catch (IOException e) {
                throw new BuildException(e);
            }
        }
        
        Set<String> toBeDeleted = new HashSet<>();
        for (String file : libFiles) {
            
            String artifact = fileToArtifactMap.get(file);
            if (artifact != null) {
                String filePattern = LibUtils.extractFilePattern(artifact);
                Set<String> artifactSet = artifactPatternToGroupMap.get(filePattern);
                
                if (artifactSet.size() == 1) {
                    if (!artifactSet.contains(file)) {
                        toBeDeleted.add(file);
                    } 
                } else {
                    List<String> asList = new ArrayList<>(artifactSet);

                    final Comparator<String> comparator = new Comparator<String>() {
                        @Override
                        public int compare(String arg0, String arg1) {
                            String version0 = LibUtils.extractVersion(fileToArtifactMap.get(arg0));
                            String version1 = LibUtils.extractVersion(fileToArtifactMap.get(arg1));
                            version0 = canonicalizeVersion(version0);
                            version1 = canonicalizeVersion(version1);
                            return version0.compareTo(version1);
                        }
                    };
                    
                    Collections.sort(asList, comparator);
                    if (!file.equals(asList.get(asList.size() - 1))) {
                        // delete the file, but only if another version exists
                        for (int i = 0; i < asList.size(); i++) {
                            String otherFile = asList.get(i);
                            if (!file.equals(otherFile)) {
                                if (fileToArtifactMap.containsKey(otherFile)) {
                                    if (new File(libDir, otherFile).exists()) {
                                        toBeDeleted.add(file);
                                    }
                                }
                            }
                        }
                    } 
                }
            }
        }
        
        log("Deleting " + toBeDeleted + ".", Project.MSG_INFO);
        
        toBeDeleted.forEach(f -> new File(libDir, f).delete());
        
        // validate the result and fail in case an artifact is missing
        validate(libFiles, fileToArtifactMap);
    }

    private void validate(String[] libFiles, Map<String, String> fileToArtifactMap) {
        
        HashSet<String> libPatternSet = new HashSet<>();
        
        for (String file : libFiles) {
            String artifact = fileToArtifactMap.get(file);
            if (artifact != null) {
                String pattern = LibUtils.extractFilePattern(artifact);
                libPatternSet.add(pattern);
            }
        }
        
        List<String> missing = new ArrayList<>();
        for (String artifact : fileToArtifactMap.values()) {
            String filePattern = LibUtils.extractFilePattern(artifact);
            if (!libPatternSet.contains(filePattern)) {
                missing.add(LibUtils.extractFile(artifact) + " (" + artifact + ")");
            }
        }
        
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Not all required artifacts are present. Missing artifacts: " + missing);
        }
    }


    public File getMetaDataDir() {
        return metaDataDir;
    }

    public void setMetaDataDir(File metaDataDir) {
        this.metaDataDir = metaDataDir;
    }

    public File getLibDir() {
        return libDir;
    }

    public void setLibDir(File libDir) {
        this.libDir = libDir;
    }

    public static String canonicalizeVersion(String version) {
        // replace common placeholders with lexicographic elements
        version = version.replace("0.1.0-SNAPSHOT", "Z");
        version = version.replace("STABLE-SNAPSHOT", "Y");
        version = version.replace("-SNAPSHOT", "Z");
        version = version.replace("_", CHAR_BEFORE_DOT);
        version = version.replace("-", CHAR_BEFORE_DOT);
        
        // extend number sequence with trailing zero and fixed length
        String[] nonNumberComponents = version.split("[0-9]+");

        String v = version;
        version = "";
        for (int i = 0; i <= nonNumberComponents.length; i++) {
            String nonNumber;
            int end;
            if (i < nonNumberComponents.length) {
                nonNumber = nonNumberComponents[i];
                end = nonNumber.length() == 0 ? 0 : v.indexOf(nonNumber);
            } else {
                nonNumber = "";
                end = v.length();
            }
            String numString = v.substring(0, end);
            if (numString.length() != 0) {
                String num = String.format("%04d", Integer.parseInt(numString));
                version = version + num + nonNumber;
            } else {
                version = version + nonNumber;
            }
            v = v.substring(end + nonNumber.length());
        }
        
        version = replaceFragment(version, "rc");
        version = replaceFragment(version, "ca");
        version = replaceFragment(version, "alpha");
        version = replaceFragment(version, "beta");
        version = replaceFragment(version, "m");
        return version;
    }

    protected static String replaceFragment(String version, final String semanticFragment) {
        return version.replace(semanticFragment, CHAR_BEFORE_DOT + semanticFragment.toUpperCase());
    }

}