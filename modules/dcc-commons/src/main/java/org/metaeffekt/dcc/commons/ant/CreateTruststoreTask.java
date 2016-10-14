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
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.metaeffekt.dcc.commons.pki.KeyUtils;


public class CreateTruststoreTask extends Task {
    private String truststoreFile;
    private String password;
    private String keystoreType = KeyUtils.KEY_STORE_TYPE_JKS;
    private List<CertificateEntry> certificates = new ArrayList<>();

    @Override
    public void execute() throws BuildException {
        try {
            createTrustStore(new File(getTruststoreFile()), getPassword().toCharArray());
        } catch (GeneralSecurityException | IOException e) {
            throw new BuildException(e);
        }
    }
    
    private void createTrustStore(File file, char[] password) throws GeneralSecurityException,
            IOException {
        final KeyStore trustStore = KeyUtils.loadKeyStore(file, getKeystoreType(), password);

        for (CertificateEntry certificateEntry : certificates) {
            Certificate certificate = KeyUtils.loadCertificate(certificateEntry.getCertificateFile());
            trustStore.setCertificateEntry(certificateEntry.getAlias(), certificate);
        }

        KeyUtils.persistKeyStore(trustStore, file, password);
    }

    public String getTruststoreFile() {
        return truststoreFile;
    }

    public void setTruststoreFile(String truststore) {
        this.truststoreFile = truststore;
    }
    
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public void setKeystoreType(String keyStoreType) {
        this.keystoreType = keyStoreType;
    }
    
    public List<CertificateEntry> getCertificates() {
        return certificates;
    }
    
    public void add(CertificateEntry cert) {
        certificates.add(cert);
    }
    
    public void addConfigured(CertificateEntry cert) {
        add(cert);
    }
}
