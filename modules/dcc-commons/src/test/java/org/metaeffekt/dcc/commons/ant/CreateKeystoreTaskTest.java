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

import static org.junit.Assert.assertTrue;

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


public class CreateKeystoreTaskTest {
    private Project project;
    
    @Before
    public void setUp() {
        project = new Project();
        project.setBaseDir(new File("."));
    }

    @Test
    public void executeTest() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException {
        String alias = "tomcat-keystore";
        String keyFile = "src/test/resources/certificates/server/private.key.pem";
        String keystoreFile = "target/tomcat.keystore";
        String certificateFile = "src/test/resources/certificates/server/cert.pem";
        String certificateChainBaseDir = "src/test/resources/certificates/server/signer-chain";
        String certificateChain = "01-xyz-ca.cert.pem;02-xyz-root.cert.pem";
        String password = "changeit";
        
        CreateKeystoreTask task = new CreateKeystoreTask();
        task.setProject(project);
        task.setKeystoreFile(keystoreFile);
        task.setPassword(password);
        
        KeyEntry key = new KeyEntry();
        key.setAlias(alias);
        key.setKeyFile(keyFile);
        
        CertificateEntry cert = new CertificateEntry();
        cert.setCertificateFile(certificateFile);
        cert.setChain(certificateChain);
        cert.setChainBaseDir(certificateChainBaseDir);
        key.add(cert);
        
        task.add(key);
        
        task.execute();
        
        assertValidKeystore(keystoreFile, task.getKeystoreType(), alias, password);
    }

    private void assertValidKeystore(String keystoreFile, String keystoreType, String alias,
            String password) throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, FileNotFoundException {
        KeyStore store = KeyStore.getInstance(keystoreType);
        FileInputStream keystore = new FileInputStream(keystoreFile);
        try {
            store.load(keystore, password.toCharArray());
            assertTrue(store.containsAlias(alias));
        } finally {
            IOUtils.closeQuietly(keystore);
        }
    }
}
