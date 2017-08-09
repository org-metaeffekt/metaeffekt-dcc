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
import java.security.GeneralSecurityException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;
import org.bouncycastle.operator.OperatorException;
import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.common.kernel.ant.log.LoggingProjectAdapter;

/**
 * Generates the certificate required for using SSL. 
 */
public class GenerateCertificatesTaskTest {

    @Test
    public void testGenerateKeyAndTrustStore01() throws GeneralSecurityException, IOException, OperatorException {
        final File componentsDir = generateCertificates("definition-01");
        Assert.assertTrue(new File(componentsDir, "xyz-ca/cert.pem").exists());
    }

    @Test
    public void testGenerateKeyAndTrustStore02() throws GeneralSecurityException, IOException, OperatorException {
        final File componentsDir = generateCertificates("definition-02");
        Assert.assertTrue(new File(componentsDir, "ae-test-ca-2016-04/cert.pem").exists());
        Assert.assertTrue(new File(componentsDir, "mmc-root/cert.pem").exists());
        Assert.assertTrue(new File(componentsDir, "mmc-application/cert.pem").exists());
    }
    
    @Test
    public void testGenerateKeyAndTrustStore03_noCertGeneratedWhenIssuerNotAvailable() throws GeneralSecurityException, IOException, OperatorException {
        final File componentsDir = generateCertificates("definition-03");
        Assert.assertFalse(new File(componentsDir, "mmc-application/cert.pem").exists());
        Assert.assertFalse(new File(componentsDir, "mmc-application/cert.request.pem").exists());
        Assert.assertFalse(new File(componentsDir, "mmc-root/cert.pem").exists());
        Assert.assertTrue(new File(componentsDir, "mmc-root/cert.request.pem").exists());
    }

    @Test
    public void testGenerateKeyAndTrustStore04_noCertGeneratedWhenIssuerNotAvailable() throws GeneralSecurityException, IOException, OperatorException {
        final File componentsDir = generateCertificates("definition-04");
        Assert.assertFalse(new File(componentsDir, "mmc-application/cert.pem").exists());
        Assert.assertFalse(new File(componentsDir, "mmc-application/cert.request.pem").exists());
        Assert.assertFalse(new File(componentsDir, "mmc-root/cert.pem").exists());
        Assert.assertTrue(new File(componentsDir, "mmc-root/cert.request.pem").exists());
    }

    @Test(expected=IllegalStateException.class)
    public void testGenerateKeyAndTrustStore05_detectCycle() throws GeneralSecurityException, IOException, OperatorException {
        final File componentsDir = generateCertificates("definition-05");
        Assert.assertFalse(new File(componentsDir, "mmc-application/cert.pem").exists());
        Assert.assertFalse(new File(componentsDir, "mmc-application/cert.request.pem").exists());
        Assert.assertFalse(new File(componentsDir, "mmc-root/cert.pem").exists());
        Assert.assertTrue(new File(componentsDir, "mmc-root/cert.request.pem").exists());
    }

    @Test
    public void testGenerateKeyAndTrustStore06_detectCertAlreadyAvailable() throws GeneralSecurityException, IOException, OperatorException {
        final File componentsDir = generateCertificates("definition-06");
        Assert.assertTrue(new File(componentsDir, "mmc-root/cert.pem").exists());
        Assert.assertFalse(new File(componentsDir, "mmc-root/private.key.pem").exists());
        Assert.assertFalse(new File(componentsDir, "mmc-root/public.key.pem").exists());
    }

    protected File generateCertificates(final String testInput) {
        final File componentsDir = new File("target/test", testInput);

        Delete delete = new Delete();
        delete.setDir(componentsDir);
        delete.execute();
        
        final File testResourcesDir = new File("src/test/resources/certificates", testInput);
        if (!testResourcesDir.exists()) {
            throw new IllegalArgumentException("Source directory does not exist.");
        }

        Copy copy = new Copy();
        copy.setProject(getAntProject());
        FileSet testSet = new FileSet();
        testSet.setDir(testResourcesDir);
        testSet.setIncludes("**/*");
        copy.add(testSet);

        copy.setTodir(componentsDir);
        copy.execute();

        GenerateCertificatesTask task = new GenerateCertificatesTask();
        task.setBaseDir(componentsDir);
        task.setFailOnError(false);
        task.execute();
        return componentsDir;
    }

    protected Project getAntProject() {
        return new LoggingProjectAdapter();
    }

}
