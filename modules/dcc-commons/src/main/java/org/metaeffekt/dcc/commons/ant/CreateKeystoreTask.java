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
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.metaeffekt.dcc.commons.pki.KeyUtils;

public class CreateKeystoreTask extends Task {

    private String keystoreFile;
    private String keystoreType = KeyUtils.KEY_STORE_TYPE_JKS;
    private List<KeyEntry> keysEntries = new ArrayList<KeyEntry>();
    private String password;

    @Override
    public void execute() throws BuildException {
        try {
            createKeyStore(new File(getKeystoreFile()), getPassword().toCharArray());
        } catch (GeneralSecurityException | IOException e) {
            throw new BuildException(e);
        }
    }

    private void createKeyStore(File file, char[] password) throws GeneralSecurityException, IOException {
        final KeyStore keyStore = KeyUtils.loadKeyStore(file, getKeystoreType(), password);

        for (KeyEntry keyEntry : getKeysEntries()) {
            PrivateKey key = KeyUtils.loadKey(keyEntry.getKeyFile());
            List<Certificate> certificates = new ArrayList<Certificate>();
            for (CertificateEntry certEntry : keyEntry.getCertificateFiles()) {
                Certificate certificate = KeyUtils.loadCertificate(certEntry.getCertificateFile());
                certificates.add(certificate);
                
                final String chain = certEntry.getChain();
                if (!StringUtils.isBlank(chain)) {
                    String chainWithCommaSeparator = chain.replace(";", ",");
                    
                    String[] splitChain = chainWithCommaSeparator.split(",");
                    
                    for (String chainCertFile : splitChain) {
                        if (!StringUtils.isBlank(chainCertFile)) {
                            chainCertFile = chainCertFile.trim();
                            
                            Certificate chainCertificate = 
                                KeyUtils.loadCertificate(certEntry.getChainBaseDir() + "/" + chainCertFile);
                            
                            certificates.add(chainCertificate);
                        }
                    }
                }
                
            }

            char[] passwd = password;
            if (keyEntry.getPassword() != null) {
                passwd = keyEntry.getPassword().toCharArray();
            }

            keyStore.setKeyEntry(keyEntry.getAlias(), key, passwd,
                    certificates.toArray(new Certificate[certificates.size()]));
        }

        KeyUtils.persistKeyStore(keyStore, file, password);
    }

    public String getKeystoreFile() {
        return keystoreFile;
    }

    public void setKeystoreFile(String keystore) {
        this.keystoreFile = keystore;
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public void setKeystoreType(String keyStoreType) {
        this.keystoreType = keyStoreType;
    }

    public void add(KeyEntry entry) {
        keysEntries.add(entry);
    }

    public List<KeyEntry> getKeysEntries() {
        return keysEntries;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
}
