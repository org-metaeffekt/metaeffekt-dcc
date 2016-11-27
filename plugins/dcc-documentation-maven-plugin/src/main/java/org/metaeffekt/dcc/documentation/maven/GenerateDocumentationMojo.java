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
package org.metaeffekt.dcc.documentation.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.DirectoryScanner;

import org.metaeffekt.dcc.docgenerator.DocGenerator;
import org.metaeffekt.core.maven.kernel.AbstractProjectAwareMojo;

@Mojo(defaultPhase=LifecyclePhase.COMPILE, requiresProject=true, name="generate", requiresDependencyResolution=ResolutionScope.RUNTIME)
public class GenerateDocumentationMojo extends AbstractProjectAwareMojo {

    @Parameter(defaultValue="${project}")
    private MavenProject project;
    
    @Parameter(property="dcc.documentation.skip", defaultValue="false")
    private boolean skip;

    @Parameter(defaultValue="${project.artifactId}")
    private String packageId;

    @Parameter(defaultValue="src/main/resources")
    private String profileDir;
    
    @Parameter(defaultValue="**/*-profile.xml,**/profile.xml,**/*-contribution.xml")
    private String includes;
    
    @Parameter
    private String excludes;
    
    @Parameter(defaultValue="${project.build.directory}/documentation")
    private String targetDir;

    @Parameter(property="dcc.documentation.load.depoyment.properties", defaultValue="true")
    private boolean loadDeploymentProperties;

    @Parameter(property="dcc.documentation.include.base.profile", defaultValue="false")
    private boolean includeBaseProfiles;

    @Parameter(property="dcc.documentation.include.contribution.profile", defaultValue="false")
    private boolean includeContributionProfiles;

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        
        if (!skip && !isPomPackagingProject()) {
            DocGenerator docGenerator = new DocGenerator();
            docGenerator.setPackageId(packageId);
            docGenerator.setLoadDeploymentProperties(loadDeploymentProperties);
            docGenerator.setIncludeBaseProfiles(includeBaseProfiles);
            docGenerator.setIncludeContributionProfiles(includeContributionProfiles);
            docGenerator.setTargetDir(new File(targetDir));

            DirectoryScanner scanner = new DirectoryScanner();
            final File profileFilesDir = new File(profileDir);
            scanner.setBasedir(profileFilesDir);
            scanner.setIncludes(toArray(includes));
            scanner.setExcludes(toArray(excludes));
            scanner.scan();
            List<File> profileFiles = new ArrayList<>();
            String[] found = scanner.getIncludedFiles();
            for (String relativePath : found) {
                if (relativePath.startsWith(profileDir)) {
                    profileFiles.add(new File(relativePath));
                } else {
                    profileFiles.add(new File(profileFilesDir.getPath(), relativePath));
                }
            }

            docGenerator.setProfileFiles(profileFiles);
            docGenerator.setSolutionRootDir(profileFilesDir);

            docGenerator.generate();
        }
    }

    public String[] toArray(String string) {
        if (string == null) return null;
        
        String[] split = string.split(",");
        for (int i = 0; i < split.length; i++) {
            split[i] = split[i].trim();
        }
        return split;
    }

}
