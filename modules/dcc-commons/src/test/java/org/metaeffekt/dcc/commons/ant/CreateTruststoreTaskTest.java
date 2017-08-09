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

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Test;

public class CreateTruststoreTaskTest {

    private Project project;

    @Before
    public void setUp() {
        project = new Project();
        project.setBaseDir(new File("."));
    }

    @Test
    public void executeTest() throws KeyStoreException, NoSuchAlgorithmException,
            CertificateException, FileNotFoundException, IOException {
        String truststoreFile = "target/tomcat.truststore";
        String certificateFile1 = "src/test/resources/certificates/trust/ldap.cert.pem";
        String certificateFile2 = "src/test/resources/certificates/trust/tomcat.cert.pem";
        String password = "changeit";

        String tomcatCertificateAlias = "tomcat-certificate";
        String ldapCertificateAlias = "ldap-certificate";
        
        CreateTruststoreTask task = new CreateTruststoreTask();
        task.setProject(project);
        task.setPassword(password);
        task.setTruststoreFile(truststoreFile);

        CertificateEntry tomcatCert = new CertificateEntry();
        tomcatCert.setAlias(tomcatCertificateAlias);
        tomcatCert.setCertificateFile(certificateFile1);

        task.add(tomcatCert);

        CertificateEntry ldapCert = new CertificateEntry();
        ldapCert.setAlias(ldapCertificateAlias);
        ldapCert.setCertificateFile(certificateFile2);

        task.add(ldapCert);

        task.execute();

        assertValidKeystore(truststoreFile, task.getKeystoreType(), password, tomcatCertificateAlias, ldapCertificateAlias);
    }

    private void assertValidKeystore(String keystoreFile, String keystoreType, 
            String password, String... aliases) throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, FileNotFoundException {
        KeyStore store = KeyStore.getInstance(keystoreType);
        FileInputStream keystore = new FileInputStream(keystoreFile);
        try {
            store.load(keystore, password.toCharArray());
            for (String alias : aliases) {
                assertNotNull(store.getCertificate(alias));
            }
        } finally {
            IOUtils.closeQuietly(keystore);
        }
    }
}
