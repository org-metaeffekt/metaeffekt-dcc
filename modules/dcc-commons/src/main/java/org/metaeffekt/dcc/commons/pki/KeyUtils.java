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
package org.metaeffekt.dcc.commons.pki;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;


public class KeyUtils {
    
    public static final String KEY_STORE_TYPE_JKS = "JKS";

    public static Certificate loadCertificate(String file) throws IOException, CertificateException {
        PEMParser parser = new PEMParser(new FileReader(file));
        try {
            X509CertificateHolder holder = (X509CertificateHolder) parser.readObject();
            JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
            return converter.getCertificate(holder);
        } finally {
            IOUtils.closeQuietly(parser);
        }
    }

    public static PrivateKey loadKey(String file) throws IOException {
        PEMParser parser = new PEMParser(new FileReader(file));
        try {
            PEMKeyPair pemObject = (PEMKeyPair) parser.readObject();
            PrivateKeyInfo info=pemObject.getPrivateKeyInfo();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter(); 
            return converter.getPrivateKey(info);
        } finally {
            IOUtils.closeQuietly(parser);
        }
    }
    
    public static PublicKey loadPublicKey(String file) throws IOException {
        PEMParser parser = new PEMParser(new FileReader(file));
        try {
            SubjectPublicKeyInfo pemObject = (SubjectPublicKeyInfo) parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter(); 
            return converter.getPublicKey(pemObject);
        } finally {
            IOUtils.closeQuietly(parser);
        }
    }

    public static PublicKey loadPublicKeyFromKeyPair(String file) throws IOException {
        PEMParser parser = new PEMParser(new FileReader(file));
        try {
            PEMKeyPair pemObject = (PEMKeyPair) parser.readObject();
            SubjectPublicKeyInfo info = pemObject.getPublicKeyInfo();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter(); 
            return converter.getPublicKey(info);
        } finally {
            IOUtils.closeQuietly(parser);
        }
    }

    public static KeyStore loadKeyStore(File file, String type, char[] password)
            throws KeyStoreException, IOException,
            NoSuchAlgorithmException, CertificateException {
        final KeyStore keyStore = KeyStore.getInstance(type);
        if (file.exists()) {
            FileInputStream stream = new FileInputStream(file);
            try {
                keyStore.load(stream, password);
            } finally {
                IOUtils.closeQuietly(stream);
            }
        } else {
            keyStore.load(null, password);
        }
        
        return keyStore;
    }

    public static void persistKeyStore(KeyStore keyStoreObject, File file, char[] password)
            throws GeneralSecurityException, IOException {
        OutputStream out = null;
        try {
            file.getParentFile().mkdirs();

            out = new BufferedOutputStream(new FileOutputStream(file));

            keyStoreObject.store(out, password);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
    
}
