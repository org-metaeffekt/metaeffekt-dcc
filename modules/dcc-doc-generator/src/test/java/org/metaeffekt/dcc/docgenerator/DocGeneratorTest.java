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
package org.metaeffekt.dcc.docgenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;
import org.junit.Ignore;
import org.junit.Test;


public class DocGeneratorTest {

    @Test
    public void testGenerator() throws IOException {
        
        File targetDir = new File("target");
        File testTargetDir = new File(targetDir, "documentation");
        
        List<File> profileFiles = new ArrayList<>();
        profileFiles.add(new File("src/test/resources/single-profile-test/profile.xml"));
        profileFiles.add(new File("src/test/resources/single-profile-test/dcc-core-profile.xml"));
        
        DocGenerator docGenerator = new DocGenerator();
        docGenerator.setProfileFiles(profileFiles);
        docGenerator.setTargetDir(testTargetDir);
        docGenerator.setPackageId("dcc-pack");
        docGenerator.setSolutionRootDir(new File("src/test/resources/single-profile-test"));
        
        docGenerator.generate();
    }

    @Ignore
    @Test
    public void testGeneratorExternalProfile() throws IOException {
        
        File targetDir = new File("target");
        File testTargetDir = new File(targetDir, "documentation");
        
        List<File> profileFiles = new ArrayList<>();

        DirectoryScanner scanner = new DirectoryScanner();
        final File basedir = new File("C:/dev/workspace/ehi-system-integration-trunk/solutions/wok-solution/target/dcc/");
        
//      final File baseDir = new File("C:/dev/workspace/hpd-installer-trunk/packages/pd-solution/target/dcc"));
//      final File baseDir = new File("C:/dev/workspace/pep-deliverable-trunk/packages/pep-example-solution/target/dcc"));
//      final File baseDir = new File("C:/dev/workspace/ancona-trunk/deliverable/packages/pcm-solution/target/dcc"));
//      final File baseDir = new File("C:/dev/workspace/dcc-application-trunk/assemblies/dcc-test-solution/target/dcc"));
//      final File baseDir = new File("C:/dev/workspace/pxs-trunk/packages/pxs-pd-example-solution/target/dcc"));
//      final File baseDir = new File("C:/dev/workspace/pxs-trunk/packages/pxs-pd-example-solution/target/dcc"));
//      final File baseDir = new File("C:/dev/workspace/ehi-system-integration-trunk/solutions/ehi-integration-reference/target/dcc"));
//      final File baseDir = new File("C:/dev/workspace/mmc-application-trunk/packages/mmc-reference-solution/target/dcc"));
//      final File baseDir = new File("C:/dev/workspace/mmc-application-trunk/packages/mmc-reference-solution/target/dcc"));
        
        scanner.setBasedir(basedir);
        scanner.setIncludes(new String[] {"**/*-profile.xml"});
        scanner.scan();
        
        for (String file : scanner.getIncludedFiles()) {
            profileFiles.add(new File(basedir, file));
        }
        
        DocGenerator docGenerator = new DocGenerator();
        docGenerator.setProfileFiles(profileFiles);
        docGenerator.setTargetDir(testTargetDir);
        docGenerator.setPackageId("dcc-pack");
        docGenerator.setSolutionRootDir(basedir);
        
        docGenerator.generate();
    }

}
