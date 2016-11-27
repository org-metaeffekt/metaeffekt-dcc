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
package org.metaeffekt.dcc.bundle.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.metaeffekt.dcc.commons.ant.LibUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.BuildException;
import org.metaeffekt.core.maven.kernel.AbstractProjectAwareMojo;

@Mojo(defaultPhase=LifecyclePhase.COMPILE, requiresProject=true, name="filter-libs", requiresDependencyResolution=ResolutionScope.RUNTIME)
public class FilterLibsMojo extends AbstractProjectAwareMojo {

    @Parameter(defaultValue="${project}")
    private MavenProject project;
    
    @Parameter(property="dcc.bundle.filter.skip", defaultValue="false")
    private boolean skip;

    @Parameter(defaultValue="${project.build.directory}/tmp/bundle-artifact/lib")
    private String libDir;

    @Parameter(defaultValue="${project.build.directory}/tmp/filter")
    private String metaDataDir;

    @Parameter(defaultValue="**/lib.metadata")
    private String metaDataPattern;

    @Parameter(defaultValue="*.jar")
    String libPattern = "*.jar";
    
    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        
        if (!skip && !isPomPackagingProject()) {
            Map<String, String> fileToArtifactMap = new HashMap<>();
            Map<String, Set<String>> artifactPatternToGroupMap = new HashMap<>();

            final File metaDataDirFile = new File(metaDataDir);
            final File libDirFile = new File(libDir);

            if (metaDataDirFile.exists() && libDirFile.exists()) {
                final File localMetaData = new File(libDirFile.getParent(), "lib.metadata");
                if (!localMetaData.exists()) {
                    throw new IllegalStateException(String.format(
                        "Required file '%s' not available. Check the plugin configuration.", localMetaData));
                }
                readMetaData(fileToArtifactMap, new HashMap<>(), localMetaData);

                String[] metaDataFiles = LibUtils.scanFiles(metaDataDirFile, metaDataPattern);
                String[] libFiles = LibUtils.scanFiles(libDirFile, libPattern);
    
                for (String file : metaDataFiles) {
                    getLog().debug("Using meta data from: " + file);
                    readMetaData(fileToArtifactMap, artifactPatternToGroupMap, new File(metaDataDir, file));
                }
    
                Set<String> toBeDeleted = new HashSet<>();
                for (String file : libFiles) {
                    getLog().debug("Analysing file: " + file);
                    String artifact = fileToArtifactMap.get(file);
                    if (artifact != null) {
                        String filePattern = LibUtils.extractFilePattern(artifact);
                        getLog().debug("Analysing file pattern: " + filePattern);
                        Set<String> artifactSet = artifactPatternToGroupMap.get(filePattern);
                        getLog().debug("Analysing set: " + artifactSet);
                        if (artifactSet != null && !artifactSet.isEmpty()) {
                            getLog().info("Removing redundant lib: " + file + " (" + filePattern + ")");
                            toBeDeleted.add(file);
                        }
                    }
                }
                
                toBeDeleted.forEach(f -> new File(libDirFile, f).delete());
            }
        }
    }

    protected void readMetaData(Map<String, String> fileToArtifactMap,
            Map<String, Set<String>> artifactPatternToGroupMap, File file) {
        try (FileReader fileReader = new FileReader(file)) {
            BufferedReader reader = new BufferedReader(fileReader);
            reader.lines().forEach(l -> LibUtils.parse(l, fileToArtifactMap, artifactPatternToGroupMap));
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }
    
    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public String getLibDir() {
        return libDir;
    }

    public void setLibDir(String libDir) {
        this.libDir = libDir;
    }

    public String getMetaDataDir() {
        return metaDataDir;
    }

    public void setMetaDataDir(String metaDataDir) {
        this.metaDataDir = metaDataDir;
    }

    public String getMetaDataPattern() {
        return metaDataPattern;
    }

    public void setMetaDataPattern(String metaDataPattern) {
        this.metaDataPattern = metaDataPattern;
    }

    public String getLibPattern() {
        return libPattern;
    }

    public void setLibPattern(String libPattern) {
        this.libPattern = libPattern;
    }

    public static void main(String[] args) throws MojoExecutionException, MojoFailureException {
        FilterLibsMojo mojo = new FilterLibsMojo();
        mojo.setLibDir("C:/dev/workspace/ehi-system-integration-trunk/bundles/ehi-atna-integration/target/tmp/bundle-artifact/lib");
        mojo.setLibPattern("*.jar");
        mojo.setMetaDataDir("C:/dev/workspace/ehi-system-integration-trunk/bundles/ehi-atna-integration/target/tmp/filter");
        mojo.setMetaDataPattern("**/lib.metadata");
        mojo.setProject(new MavenProject());
        mojo.setLog(new Log() {
            
            @Override
            public void warn(CharSequence arg0, Throwable arg1) {}
            
            @Override
            public void warn(Throwable arg0) {}
            
            @Override
            public void warn(CharSequence arg0) {
            }
            
            @Override
            public boolean isWarnEnabled() {
                return false;
            }
            
            @Override
            public boolean isInfoEnabled() {
                return false;
            }
            
            @Override
            public boolean isErrorEnabled() {
                return false;
            }
            
            @Override
            public boolean isDebugEnabled() {
                return false;
            }
            
            @Override
            public void info(CharSequence arg0, Throwable arg1) {
                System.out.println(arg0);
            }
            
            @Override
            public void info(Throwable arg0) {
                System.out.println(arg0);
            }
            
            @Override
            public void info(CharSequence arg0) {
                System.out.println(arg0);
            }
            
            @Override
            public void error(CharSequence arg0, Throwable arg1) {
                System.out.println(arg0);
            }
            
            @Override
            public void error(Throwable arg0) {}
            
            @Override
            public void error(CharSequence arg0) {}
            
            @Override
            public void debug(CharSequence arg0, Throwable arg1) {}
            
            @Override
            public void debug(Throwable arg0) {}
            
            @Override
            public void debug(CharSequence arg0) {}
        });
        mojo.execute();
    }

}
