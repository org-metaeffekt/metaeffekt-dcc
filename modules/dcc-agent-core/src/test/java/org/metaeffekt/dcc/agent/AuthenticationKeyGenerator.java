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
package org.metaeffekt.dcc.agent;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.OperatorException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.Test;

public class AuthenticationKeyGenerator {

    private static final int VALIDITY_IN_YEARS = 3;

    private static final String KEY_STORE_TYPE = "JKS";

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private static final String KEY_ALGORITHM = "RSA";

    private static final int DEFAULT_KEY_SIZE = 2048;

    private final Random random = new Random();

    @Test
    public void testGeneratePassword() throws Exception {
        final String randomAlphanumeric = RandomStringUtils.randomAlphanumeric(16);
        System.out.println(randomAlphanumeric);
    }

    @Test
    public void testGenerateKeyAndTrustStore() throws Exception {
        final File folder = new File("target");
        folder.mkdir();

        final int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        generateKeyAndTrustStore(folder, createDate(currentYear, 1, 1),
                createDate(currentYear + VALIDITY_IN_YEARS, 1, 1), "DYKK8T8m9nKqBRPZ".toCharArray());
    }

    public void generateKeyAndTrustStore(File folder, Date begin, Date end, char[] password)
            throws GeneralSecurityException, IOException, OperatorException {
        final KeyPair serverKey = generateKey();

        final X509Certificate serverCertificate = generateCertificate(serverKey,
                "DCC Agent", begin, end);

        final KeyPair clientKey = generateKey();

        final X509Certificate clientCertificate = generateCertificate(clientKey,
                "DCC Client", begin, end);

        createKeyStore(new File(folder, "agent-keystore.jks"), "agent", serverKey,
                serverCertificate,
                password);
        createTrustStore(new File(folder, "agent-truststore.jks"), "shell", clientCertificate,
                password);

        createKeyStore(new File(folder, "shell-keystore.jks"), "shell", clientKey,
                clientCertificate,
                password);
        createTrustStore(new File(folder, "shell-truststore.jks"), "agent", serverCertificate,
                password);
    }

    private void createKeyStore(File file, String keyAlias, KeyPair key,
            X509Certificate certificate, char[] password) throws GeneralSecurityException,
            IOException {
        final KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
        keyStore.load(null, password);

        keyStore.setKeyEntry(keyAlias, key.getPrivate(), password,
                new Certificate[] { certificate });

        persistKeyStore(keyStore, file, password);
    }

    private void createTrustStore(File file, String certificateAlias, X509Certificate certificate,
            char[] password) throws GeneralSecurityException, IOException {
        final KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
        keyStore.load(null, password);

        keyStore.setCertificateEntry(certificateAlias, certificate);

        persistKeyStore(keyStore, file, password);
    }

    private void persistKeyStore(KeyStore keyStoreObject, File file, char[] password)
            throws GeneralSecurityException, IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file));

            keyStoreObject.store(out, password);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    private Date createDate(int year, int month, int day) {
        Validate.inclusiveBetween(1, 5000, year);
        Validate.inclusiveBetween(0, 11, month);
        Validate.inclusiveBetween(1, 31, day);

        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

    private KeyPair generateKey() throws NoSuchAlgorithmException {
        final KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        keyGenerator.initialize(DEFAULT_KEY_SIZE);

        return keyGenerator.generateKeyPair();
    }

    private X509Certificate generateCertificate(KeyPair key, String certificateCN,
            Date begin, Date end) throws GeneralSecurityException, IOException, OperatorException {
        final X500NameBuilder nameBuilder = new X500NameBuilder();
        nameBuilder.addRDN(BCStyle.CN, certificateCN);
        final X500Name name = nameBuilder.build();

        final JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(name,
                new BigInteger(String.valueOf(random.nextInt())), begin, end, name, key.getPublic());
        certBuilder.addExtension(Extension.subjectKeyIdentifier, false,
                new JcaX509ExtensionUtils().createSubjectKeyIdentifier(key.getPublic()));

        final X509CertificateHolder certificateHolder = certBuilder
                .build(new JcaContentSignerBuilder(
                        SIGNATURE_ALGORITHM).build(key.getPrivate()));

        final X509Certificate certificate = new JcaX509CertificateConverter()
                .getCertificate(certificateHolder);
        return certificate;
    }

}
