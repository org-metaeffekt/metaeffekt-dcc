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
package org.metaeffekt.dcc.commons.pki;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.bouncycastle.operator.OperatorException;
import org.junit.Ignore;
import org.junit.Test;
import org.metaeffekt.core.common.kernel.ant.log.LoggingProjectAdapter;
import org.metaeffekt.dcc.commons.ant.GenerateCertificatesTask;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Test supporting validation of external configurations
 */
public class CustomCertificateGeneratorTest {

    @Ignore
    @Test
    public void testGenerateKeyAndTrustStore() throws GeneralSecurityException, IOException, OperatorException {
        final File componentsSourceDir = new File("/Volumes/USB/ca");
        final File componentsTmpDir = new File("/Volumes/USB/ca-tmp");

        try {
            if (componentsTmpDir.exists()) FileUtils.deleteDirectory(componentsTmpDir);
        } catch (Exception e) {
        }

        FileUtils.copyDirectory(componentsSourceDir, componentsTmpDir);

        GenerateCertificatesTask task = new GenerateCertificatesTask();
        task.setBaseDir(componentsTmpDir);
        task.setFailOnError(true);
        task.execute();
    }

    protected Project getAntProject() {
        return new LoggingProjectAdapter();
    }

}
