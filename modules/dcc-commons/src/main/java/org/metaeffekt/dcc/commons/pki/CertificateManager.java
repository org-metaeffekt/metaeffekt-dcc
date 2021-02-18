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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OperatorException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.metaeffekt.core.common.kernel.ant.log.LoggingProjectAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.metaeffekt.dcc.commons.ant.PropertyUtils;

public class CertificateManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(CertificateManager.class);

    private static final String PURPOSE_SERVER_AUTHENTICATION = "Server-Authentication";
    private static final String PURPOSE_CLIENT_AUTHENTICATION = "Client-Authentication";

    private static final String PROPERTY_COMPONENT = "component";
    private static final String PROPERTY_CN = "cn";
    
    private static final String PROPERTY_CERT_PURPOSE = "cert.purpose";
    private static final String PROPERTY_CERT_VALID_FROM = "cert.valid.from";
    private static final String PROPERTY_CERT_VALID_TO = "cert.valid.to";
    private static final String PROPERTY_CERT_VALID_MONTHS = "cert.valid.months";
    private static final String PROPERTY_CERT_USAGE = "cert.usage";
    private static final String PROPERTY_CERT_SIGNATURE_ALGORITHM = "cert.signature.algorithm";
    private static final String PROPERTY_CERT_CHAIN_LENGTH = "cert.chain.length";
    private static final String PROPERTY_CERT_TYPE = "cert.type";
    private static final String PROPERTY_CERT_SELFSIGNED = "cert.selfsigned";
    
    // not documented in external documentation. Using defaults.
    private static final String PROPERTY_CERT_CRITICAL_NAMES = "cert.critical.names";
    private static final String PROPERTY_CERT_CRITICAL_KEY_PURPOSE = "cert.critical.key.purpose";
    private static final String PROPERTY_CERT_CRITICAL_KEY_USAGE = "cert.critical.key.usage";
    private static final String PROPERTY_CERT_CRITICAL_CA = "cert.critical.ca";
    private static final String PROPERTY_CERT_CRITICAL_KEY_IDENTIFIER = "cert.critical.key.identifier";
    private static final String PROPERTY_CERT_CRITICAL_CRL_DISTRIBUTION_POINTS = "cert.critical.crl.distribution.points";
    private static final String PROPERTY_CERT_CRITICAL_AUTHORITY_INFORMATION_ACCESS = "cert.critical.authority.information.access";

    private static final String PROPERTY_PREFIX_CRL_DISTRIBUTION_POINT = "cert.crl.distribution.point";
    private static final String PROPERTY_PREFIX_AUTHORITY_INFORMATION_ACCESS = "cert.authority.information.access";
    private static final String PROPERTY_PREFIX_CERT_NAME = "cert.name";
    private static final String PROPERTY_PREFIX_SUBJECT = "subject";
    private static final String PROPERTY_PREFIX_TRUST = "trust";

    private static final String PROPERTY_CSR_SIGNATURE_ALGORITHM = "csr.signature.algorithm";

    private static final String NAME_OTHER = "other";
    private static final String NAME_IP = "ip";
    private static final String NAME_DIRECTORY = "directory";
    private static final String NAME_DNS = "dns";

    private static final String CERT_TYPE_CA_OLD = "issuer";
    private static final String CERT_TYPE_CA = "ca";
    private static final String CERT_TYPE_TLS = "tls";
    private static final String CERT_TYPE_TOKEN_ISSUER = "token-issuer";

    private static final String PROPERTY_SIGNER_COMPONENT = "signer.component";
    private static final String PROPERTY_ISSUER_COMPONENT = "issuer.component";
    private static final String PROPERTY_KEY_SIZE = "key.size";
    private static final String PROPERTY_KEY_ALGORITHM = "key.algorithm";

    private static final String BOOLEAN_STRING_FALSE = "false";

    private static final String DEFAULT_SIGNING_ALGORITHM = "SHA256withRSA";
    private static final String DEFAULT_KEY_ALGORITHM = "RSA";
    private static final int DEFAULT_KEY_SIZE = 2048;
    private static final int DEFAULT_VALIDITY_IN_MONTHS = 3 * 12;

    private static final int DEFAULT_KEYUSAGE_ISSUER = 
            KeyUsage.keyCertSign | KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.cRLSign;
    private static final int DEFAULT_KEYUSAGE_TOKEN_ISSUER = 
            KeyUsage.digitalSignature | KeyUsage.keyEncipherment;
    private static final int DEFAULT_KEYUSAGE_TLS = 
            KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.keyEncipherment;

    private static final String FILE_EXTENSION_CERT = "cert.pem";
    private static final String FILE_PATTERN_CERT = "*." + FILE_EXTENSION_CERT;

    private static final String FILENAME_PRIVATE_KEY = "private.key.pem";
    private static final String FILENAME_PUBLIC_KEY = "public.key.pem";
    private static final String FILENAME_CERT = FILE_EXTENSION_CERT;
    private static final String FILENAME_CERT_REQUEST = "cert.request.pem";
    private static final String FILENAME_COMPONENT_PROPERTIES = "config/component.properties";

    private static final String FOLDERNAME_SIGNER_CHAIN = "signer-chain";
    private static final String FOLDERNAME_TRUST = "trust";
    
    private final Random random = new SecureRandom();

    private final File componentBaseDir;;
    private final File componentDir;
    private final String componentName;

    private transient Properties componentProperties;

    private transient File privateKeyFile;
    private transient File publicKeyFile;
    private transient File certFile;
    private transient File certRequestFile;

    private boolean finalIteration = false;
    
    private List<String> messages = new ArrayList<>();
    
    private Project antProject;

    private TimeZone timeZone = TimeZone.getDefault(); 
    
    private boolean processed = false;

    public CertificateManager(File componentBaseDir, String componentName) {
        super();
        this.componentBaseDir = componentBaseDir;
        this.componentName = componentName;
        this.componentDir = new File(componentBaseDir, componentName);
    }

    public File getComponentDir() {
        return componentDir;
    }

    public void init() {
        if (this.componentProperties == null) {
            Validate.isTrue(componentDir.exists(), String.format("Directory '%s' must exist.", componentDir));

            final String propertiesFilename = FILENAME_COMPONENT_PROPERTIES;
            File propertiesFile = new File(componentDir, propertiesFilename);
            
            Validate.isTrue(componentDir.exists(), String.format("Property file '%s' must exist.", propertiesFile));
            this.componentProperties = PropertyUtils.loadPropertyFile(propertiesFile);

            privateKeyFile = new File(componentDir, FILENAME_PRIVATE_KEY);
            publicKeyFile = new File(componentDir, FILENAME_PUBLIC_KEY);
            certFile = new File(componentDir, FILENAME_CERT);
            certRequestFile = new File(componentDir, FILENAME_CERT_REQUEST);
        }
    }

    public void createOrComplete() throws IOException, GeneralSecurityException, OperatorException {
        if (!processed) {
            this.processed = true;
            createOrCompleteKeyPair();
            createOrCompleteCertificates();
            createOrCompleteTrust();
            updateSignerChain();
        } 
    }

    public void createOrCompleteTrust() throws IOException, GeneralSecurityException, OperatorException {
        init();
        String prefix = PROPERTY_PREFIX_TRUST;
        
        File trustedCertTargetDir = new File(componentDir, FOLDERNAME_TRUST);
        
        // support the array notations <prefix>.component[index]=<name>
        for (Object key : componentProperties.keySet()) {
            final String attributeKey = String.valueOf(key);
            if (attributeKey.startsWith(prefix + ".")) {
                String attributeName = attributeKey.substring(prefix.length() + 1);
                if (attributeName.contains("[")) {
                    attributeName = attributeName.substring(0, attributeName.indexOf("["));
                    if (attributeName.equals(PROPERTY_COMPONENT)) {
                        String trustedComponentName = getProperty(attributeKey);
                        copyTrustedCert(trustedComponentName, trustedCertTargetDir);
                    }
                }
            }
        }
        
        // also support the none array notations <prefix>.component=<name>
        String trustedComponentName = getProperty(prefix + "." + PROPERTY_COMPONENT);
        if (trustedComponentName != null) {
            copyTrustedCert(trustedComponentName, trustedCertTargetDir);
        }
    }

    protected void copyTrustedCert(String trustedComponentName, File trustedCertTargetDir)
            throws IOException, CertificateException {
        File trustedDir =  new File(componentBaseDir, trustedComponentName);
        File trustedCert = new File(trustedDir, FILENAME_CERT);
        File trustedCertTargetFile = new File(trustedCertTargetDir,
                String.format("%s.%s", trustedComponentName, FILE_EXTENSION_CERT));
        
        trustedCertTargetDir.mkdirs();
        
        if (trustedCert.exists()) {
            final X509Certificate signerCertificate = (X509Certificate) 
                KeyUtils.loadCertificate(trustedCert.getPath());
            persist(signerCertificate, trustedCertTargetFile);
        } else {
            if (finalIteration) {
                final String message = String.format("Cannot complete trust folder for component '%s'. "
                    + "Certificate of trusted component '%s' not available.",
                    componentName, trustedComponentName);
                LOG.warn(message);
                messages.add(message);
            }
        }
    }

    public void createOrCompleteKeyPair() throws IOException, GeneralSecurityException, OperatorException {
        init();
        if (!publicKeyFile.exists() && !privateKeyFile.exists() && !certFile.exists()) {
            final KeyPair keyPair = generateKeyPair();
            persist(keyPair, privateKeyFile, publicKeyFile);
        } else {
            LOG.debug("Skipping key pair generation for '{}'. Either public key, private key or cert file already exists.",
                componentName);
        }
    }

    public void createOrCompleteCertificates() throws IOException, GeneralSecurityException, OperatorException {
        init();
        if (!certFile.exists()) {
            final X509Certificate caCert = generateCertificate();
            if (caCert != null) {
                persist(caCert, certFile);
            } else {
                // certificate could not be created due to missing issuer / signer
                if (finalIteration) {
                    // when this is the final iteration we file a CSR if not already exists
                    if (!certRequestFile.exists()) {
                        final PKCS10CertificationRequest certRequest = generateCertificateRequest();
                        persist(certRequest, certRequestFile);
                    } else {
                        LOG.debug("Skipping certification request generation for '{}'. Request file already exists.", componentName);
                    }
                    final String message = String.format("Cannot complete certificate configuration for component '%s'. "
                            + "A Certification Signing Request (CSR) has been created in file '%s'.", componentName, certRequestFile);
                    LOG.warn(message);
                    messages.add(message);
                }
            }
        } else {
            LOG.debug("Skipping certificate generation for '{}'. Either public key or private key file already exists.",
                    componentName);
        }
    }

    protected void persist(final KeyPair systemKeyPair, File privateKeyFile, File publicKeyFile) throws IOException {
        ensureParentDirExists(privateKeyFile);
        ensureParentDirExists(publicKeyFile);

        persistKey(privateKeyFile, systemKeyPair.getPrivate());
        persistKey(publicKeyFile, systemKeyPair.getPublic());
    }

    protected void persist(final X509Certificate certificate, File certFile) throws IOException {
        ensureParentDirExists(certFile);
        persistPemObject(certFile, certificate);
    }

    protected void persist(final PKCS10CertificationRequest certificateRequest, File certRequestFile)
            throws IOException {
        ensureParentDirExists(certRequestFile);
        persistPemObject(certRequestFile, certificateRequest);
    }

    protected boolean ensureParentDirExists(File privateKeyFile) {
        final File parent = privateKeyFile.getParentFile();
        return parent != null ? parent.mkdirs() : false;
    }

    private void persistPemObject(File file, Object serverCertificate) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        JcaPEMWriter writer = new JcaPEMWriter(out);
        try {
            writer.writeObject(serverCertificate);
        } finally {
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(out);
        }
    }

    private void persistKey(File file, Key key) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        JcaPEMWriter writer = new JcaPEMWriter(out);
        try {
            writer.writeObject(key);
        } finally {
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(out);
        }
    }

    private void roundFloor(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.setTimeZone(timeZone);
    }

    private void roundCeiling(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        String keyAlgorithm = getProperty(PROPERTY_KEY_ALGORITHM, DEFAULT_KEY_ALGORITHM);
        int keySize = getProperty(PROPERTY_KEY_SIZE, DEFAULT_KEY_SIZE);

        final KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(keyAlgorithm);
        keyGenerator.initialize(keySize);
        return keyGenerator.generateKeyPair();
    }

    public void updateSignerChain() throws CertificateException, IOException  {
        String issuerComponentName = getIssuerComponentName();

        String signerComponentName = getSignerComponentName(issuerComponentName);

        if (!componentName.contentEquals(signerComponentName)) {
            File signerDir = new File(componentBaseDir, signerComponentName);
            copyCertificateChain(signerDir, signerComponentName);
        }
    }

    public String getIssuerComponentName() {
        return getProperty(PROPERTY_ISSUER_COMPONENT, componentName);
    }
    
    private X509Certificate generateCertificate() throws GeneralSecurityException, IOException, OperatorException {
        String issuerComponentName = getIssuerComponentName();

        // determine signer; per default issuer is signer (issuer can be subject --> self-signed)
        String signerComponentName = getSignerComponentName(issuerComponentName);

        if (signerComponentName.equals(componentName)) {
            // self-signed certs is not our goal
            if (BOOLEAN_STRING_FALSE.equals(getProperty(PROPERTY_CERT_SELFSIGNED, BOOLEAN_STRING_FALSE))) {
                return null;
            }
        }

        PublicKey publicKey = loadPublicKey();

        final Calendar begin = getValidityPeriodBegin();
        final Calendar end = getValidityPeriodEnd(begin);
        
        final X500Name name = createSubjectNameBuilder();

        final BigInteger serialNo = new BigInteger(String.valueOf(random.nextInt()));

        JcaX509v3CertificateBuilder certBuilder = null;

        X509Certificate issuerCertificate = null;

        if (issuerComponentName.equals(componentName)) {
            // check whether this and the issuer are the same and user the already constructed name
            if (issuerComponentName.equals(componentName)) {
                certBuilder = new JcaX509v3CertificateBuilder(name, serialNo, 
                        begin.getTime(), end.getTime(), name, publicKey);
            }
        } else {
            // lookup the certificate of the referenced issuer
            File issuerDir = new File(componentBaseDir, issuerComponentName);
            File issuerCert = new File(issuerDir, FILENAME_CERT);
            if (issuerCert.exists()) {
                issuerCertificate = (X509Certificate) KeyUtils.loadCertificate(issuerCert.getPath());
                certBuilder = new JcaX509v3CertificateBuilder(issuerCertificate, serialNo, 
                        begin.getTime(), end.getTime(), name, publicKey);
            }
        }
        
        if (certBuilder == null) {
            // issuer cert was not found. Potentially it was not yet created
            return null;
        }
        
        List<Extension> extensions = createExtensions(publicKey, issuerCertificate);
        
        for (Extension extension : extensions) {
            certBuilder.addExtension(extension);
        }
        
        // load the private key of the signer (signer may be issuer, may be self)
        PrivateKey signerPrivateKey = null;
        File signerDir = new File(componentBaseDir, signerComponentName);
        File signerPrivateKeyFile = new File(signerDir, FILENAME_PRIVATE_KEY);
        if (signerPrivateKeyFile.exists()) {
            signerPrivateKey = KeyUtils.loadKey(signerPrivateKeyFile.getPath());
        } else {
            // when we cannot access the signer we cannot provide a certificate
            return null;
        }

        final String signatureAlgorithm = getProperty(PROPERTY_CERT_SIGNATURE_ALGORITHM, DEFAULT_SIGNING_ALGORITHM);
        final X509CertificateHolder certificateHolder = certBuilder
                .build(new JcaContentSignerBuilder(signatureAlgorithm).build(signerPrivateKey));

        return new JcaX509CertificateConverter().getCertificate(certificateHolder);
    }

    public String getSignerComponentName(String issuerComponentName) {
        String signerComponentName = getProperty(PROPERTY_SIGNER_COMPONENT, issuerComponentName);
        return signerComponentName;
    }

    protected List<Extension> createExtensions(PublicKey publicKey, X509Certificate issuerCertificate)
            throws NoSuchAlgorithmException, IOException {
        
        List<Extension> extensions = new ArrayList<>();

        String certType = getProperty(PROPERTY_CERT_TYPE, CERT_TYPE_TLS);
        
        // backward compatibility
        if (CERT_TYPE_CA_OLD.equals(certType)) {
            certType = CERT_TYPE_CA;
        }
        
        // subject key identifier
        boolean criticalKeyIdentifier = getProperty(PROPERTY_CERT_CRITICAL_KEY_IDENTIFIER, false);
        extensions.add(new Extension(Extension.subjectKeyIdentifier, criticalKeyIdentifier,
            new JcaX509ExtensionUtils().createSubjectKeyIdentifier(publicKey).getEncoded()));

        // basic constraints
        if (CERT_TYPE_CA.equals(certType)) {
            boolean criticalCaConstraints = getProperty(PROPERTY_CERT_CRITICAL_CA, true);
            int chainLengthConstraint = getProperty(PROPERTY_CERT_CHAIN_LENGTH, 0);
            if (chainLengthConstraint > 0) {
                extensions.add(new Extension(Extension.basicConstraints, criticalCaConstraints, 
                    new BasicConstraints(chainLengthConstraint).getEncoded()));
            } else {
                extensions.add(new Extension(Extension.basicConstraints, criticalCaConstraints, 
                    new BasicConstraints(true).getEncoded()));
            }
        }

        // key usage
        int keyUsageInt = getKeyUsage(certType);
        if (keyUsageInt != 0) {
            // FIXME: test whether we can default to true here
            boolean criticalKeyUsage = getProperty(PROPERTY_CERT_CRITICAL_KEY_USAGE, false);
            KeyUsage keyUsage = new KeyUsage(keyUsageInt);
            extensions.add(new Extension(Extension.keyUsage, criticalKeyUsage, keyUsage.getEncoded()));
        }

        // extended key usage
        KeyPurposeId[] keyPurposeDefault = null;
        if (CERT_TYPE_TLS.equals(certType)) {
            // defaults for TLS
            keyPurposeDefault = new KeyPurposeId[] { KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth };
        }
        boolean criticalKeyPurpose = getProperty(PROPERTY_CERT_CRITICAL_KEY_PURPOSE, false);
        KeyPurposeId[] keyPurpose = createKeyPurposeIds(keyPurposeDefault);
        if (keyPurpose != null) {
            extensions.add(new Extension(Extension.extendedKeyUsage, 
                criticalKeyPurpose, new ExtendedKeyUsage(keyPurpose).getEncoded()));
        }
        
        // subjectAlternativeName
        List<ASN1Encodable> subjectAlternativeNames = extractAlternativeNames(PROPERTY_PREFIX_CERT_NAME);
        if (!subjectAlternativeNames.isEmpty()) {
            boolean criticalNames = getProperty(PROPERTY_CERT_CRITICAL_NAMES, false);
            DERSequence subjectAlternativeNamesExtension = new DERSequence(
                    subjectAlternativeNames.toArray(new ASN1Encodable[subjectAlternativeNames.size()]));
            extensions.add(new Extension(Extension.subjectAlternativeName, criticalNames, 
                subjectAlternativeNamesExtension.getEncoded()));
        }
        
        if (issuerCertificate == null) {
            // crl distribution point
            DistributionPoint[] crlDistributionPoints = createCrlDistributionPoints();
            if (crlDistributionPoints != null) {
                boolean criticalCrlDistPoints = getProperty(PROPERTY_CERT_CRITICAL_CRL_DISTRIBUTION_POINTS, false);
                extensions.add(new Extension(Extension.cRLDistributionPoints, criticalCrlDistPoints, 
                        new CRLDistPoint(crlDistributionPoints).getEncoded()));
            }        
            
            // authority information access
            AccessDescription[] accessDescriptions = createAccessDescriptions();
            if (accessDescriptions != null) {
                boolean criticalAuthorityInformationAccess = getProperty(PROPERTY_CERT_CRITICAL_AUTHORITY_INFORMATION_ACCESS, false);
                extensions.add(new Extension(Extension.authorityInfoAccess, criticalAuthorityInformationAccess,   
                        new AuthorityInformationAccess(accessDescriptions).getEncoded()));
            }
        } else {
            copyExtension(Extension.cRLDistributionPoints, issuerCertificate, extensions);
            copyExtension(Extension.authorityInfoAccess, issuerCertificate, extensions);
        }
        return extensions;
    }

    protected void copyExtension(final ASN1ObjectIdentifier extensionType, X509Certificate issuerCertificate,
            List<Extension> extensions) {
        final byte[] encodedAttribute = issuerCertificate.getExtensionValue(extensionType.getId());
        ASN1OctetString data = ASN1OctetString.getInstance(encodedAttribute);
        boolean isCritical = issuerCertificate.getCriticalExtensionOIDs().contains(extensionType.getId());
        if (encodedAttribute != null) {
            extensions.add(new Extension(extensionType, isCritical, data)); 
        }
    }

    private DistributionPoint[] createCrlDistributionPoints() {
        List<DistributionPoint> list = new ArrayList<>();
        Set<String> keys = getArrayKeys(PROPERTY_PREFIX_CRL_DISTRIBUTION_POINT);
        for (String dpPrefix : keys) {
            final String uriKey = dpPrefix + ".uri";
            String uri = getMandatoryProperty(uriKey);
            
            DistributionPointName dpName = new DistributionPointName(new GeneralNames(
                new GeneralName(GeneralName.uniformResourceIdentifier, uri)));
            list.add(new DistributionPoint(dpName, null, null));
        }
        if (list.isEmpty()) return null;
        return list.toArray(new DistributionPoint[list.size()]);
    }

    private AccessDescription[] createAccessDescriptions() {
        List<AccessDescription> list = new ArrayList<>();
        Set<String> keys = getArrayKeys(PROPERTY_PREFIX_AUTHORITY_INFORMATION_ACCESS);
        for (String dpPrefix : keys) {
            final String typeKey = dpPrefix + ".type";
            final String type = getMandatoryProperty(typeKey);

            final String uriKey = dpPrefix + ".uri";
            final String uri = getMandatoryProperty(uriKey);

            ASN1ObjectIdentifier aiaId = null;
            switch(type) {
                case "ocsp": aiaId = AccessDescription.id_ad_ocsp; break;
                case "issuer": aiaId = AccessDescription.id_ad_caIssuers; break;
                default:
                    throw new IllegalArgumentException(String.format(
                        "Value '%s' not supported for '%s'. Supported values are 'ocsp' or 'issuer'.", 
                        type, typeKey));
            }
            
            AccessDescription accessDescription = new AccessDescription(aiaId, 
                new GeneralName(GeneralName.uniformResourceIdentifier, uri));
            
            list.add(accessDescription);
        }

        if (list.isEmpty()) return null;
        return list.toArray(new AccessDescription[list.size()]);
    }

    protected Set<String> getArrayKeys(String prefix) {
        Set<String> keys = new LinkedHashSet<>();
        for (Object key : componentProperties.keySet()) {
            final String attributeKey = String.valueOf(key);
            if (attributeKey.startsWith(prefix + "[") ) {
                String dpPrefix = attributeKey.substring(0,  attributeKey.lastIndexOf("]") + 1);
                keys.add(dpPrefix);
            }
        }
        return keys;
    }

    protected String getMandatoryProperty(final String uriKey) {
        String uri = getProperty(uriKey);
        if (StringUtils.isBlank(uri)) {
            throw new IllegalArgumentException(String.format("The property '%s' must be defined.", 
                uriKey));
        }
        return uri;
    }

    private KeyPurposeId[] createKeyPurposeIds(KeyPurposeId[] defaultKeyPurposeIds) {
        String purpose = getProperty(PROPERTY_CERT_PURPOSE, "");
        
        String[] split = purpose.split(",");
        
        List<KeyPurposeId> purposeList = new ArrayList<>();
        
        for (int i = 0; i < split.length; i++) {
            String p = split[i].trim();
            if (StringUtils.isNotBlank(p)) {
                switch(p) {
                    case PURPOSE_CLIENT_AUTHENTICATION:
                        purposeList.add(KeyPurposeId.id_kp_clientAuth);
                        break;
                    case PURPOSE_SERVER_AUTHENTICATION:
                        purposeList.add(KeyPurposeId.id_kp_serverAuth);
                        break;
                    default:
                        try {
                            ASN1ObjectIdentifier newKeyPurposeIdOID = new ASN1ObjectIdentifier(p);
                            purposeList.add(KeyPurposeId.getInstance(newKeyPurposeIdOID));
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException(String.format("Certificate purpose '%s' not supported. "
                                + "Supported values are '%s', '%s', or any valid OID.", 
                                p, PURPOSE_CLIENT_AUTHENTICATION, PURPOSE_SERVER_AUTHENTICATION));
                        }
                }
            }
        }
        
        if (purposeList.isEmpty()) {
            return defaultKeyPurposeIds;
        }
        
        return purposeList.toArray(new KeyPurposeId[purposeList.size()]);
    }

    protected List<ASN1Encodable> extractAlternativeNames(String prefix) {
        List<ASN1Encodable> subjectAlternativeNames = new ArrayList<ASN1Encodable>();
        for (Object key : componentProperties.keySet()) {
            final String attributeKey = String.valueOf(key);
            if (attributeKey.startsWith(prefix)) {
                String nameTypeString = attributeKey.substring(attributeKey.lastIndexOf(".") + 1);
                String nameValue = getProperty(attributeKey);
                int nameType = 0;
                switch(nameTypeString) {
                    case NAME_DNS : nameType = GeneralName.dNSName; break;
                    case NAME_DIRECTORY : nameType = GeneralName.directoryName; break;
                    case NAME_IP : nameType = GeneralName.iPAddress; break;
                    case NAME_OTHER : nameType = GeneralName.otherName; break;
                    default:
                        throw new IllegalArgumentException(String.format("Alternative name '%s' not supported.", nameTypeString));
                }
                
                if (StringUtils.isNotBlank(nameValue)) {
                    subjectAlternativeNames.add(new GeneralName(nameType, nameValue));
                }
            }
        }
        
        return subjectAlternativeNames;
    }

    protected PKCS10CertificationRequest generateCertificateRequest() throws IOException, OperatorCreationException, NoSuchAlgorithmException {
        PublicKey publicKey = loadPublicKey();
        PrivateKey privateKey = loadPrivateKey();

        final X500Name name = createSubjectNameBuilder();

        JcaPKCS10CertificationRequestBuilder certReqBuilder = new JcaPKCS10CertificationRequestBuilder(name, publicKey);
        
        List<Extension> extensionList = createExtensions(publicKey, null);
        Extensions extensions = new Extensions(extensionList.toArray(new Extension[extensionList.size()]));

        certReqBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions);
        
        final String signatureAlgorithm = getProperty(PROPERTY_CSR_SIGNATURE_ALGORITHM, DEFAULT_SIGNING_ALGORITHM);
        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(signatureAlgorithm);
        ContentSigner signer = csBuilder.build(privateKey);
        return certReqBuilder.build(signer);
    }

    protected X500Name createSubjectNameBuilder() {
        final X500NameBuilder nameBuilder = createNameBuilder(PROPERTY_PREFIX_SUBJECT);
        return nameBuilder.build();
    }

    protected PrivateKey loadPrivateKey() throws IOException {
        return KeyUtils.loadKey(privateKeyFile.getPath());
    }

    protected PublicKey loadPublicKey() throws IOException {
        PublicKey publicKey = null;
        if (publicKeyFile.exists()) {
            publicKey = KeyUtils.loadPublicKey(publicKeyFile.getPath());
        } else {
            if (privateKeyFile.exists()) {
                publicKey = KeyUtils.loadPublicKeyFromKeyPair(privateKeyFile.getPath());
            }
        }

        Validate.notNull(publicKey, String.format("Cannot access public key. Either '%s' or '%s' must exist.",
                publicKeyFile, privateKeyFile));
        return publicKey;
    }

    protected boolean copyCertificateChain(File signerDir, String signerComponentName) throws IOException, CertificateException {

        final File targetChainDir = new File(componentDir, FOLDERNAME_SIGNER_CHAIN);

        if (targetChainDir.exists()) {
            Delete delete = new Delete();
            delete.setProject(getAntProject());
            delete.setIncludes(FILE_PATTERN_CERT);
            delete.setDir(targetChainDir);
            delete.execute();
        }

        targetChainDir.mkdirs();
        final File sourceChainDir = new File(signerDir, FOLDERNAME_SIGNER_CHAIN);
        int foundFiles = 0;
        if (sourceChainDir.exists()) {
            // copy the issuer certificate chain
            Copy copy = new Copy();
            copy.setOverwrite(true);
            copy.setProject(getAntProject());
            copy.setTodir(targetChainDir);
            FileSet set = new FileSet();
            set.setDir(sourceChainDir);
            set.setIncludes(FILE_PATTERN_CERT);
            copy.add(set);
            copy.execute();

            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(sourceChainDir);
            scanner.setIncludes(new String[] { FILE_PATTERN_CERT });
            scanner.scan();

            foundFiles = scanner.getIncludedFilesCount();
        }

        File signerCert = new File(signerDir, FILENAME_CERT);
        File signerCertTargetFile = new File(targetChainDir,
                String.format("%02d-%s.%s", foundFiles + 1, signerComponentName, FILE_EXTENSION_CERT));

        // NOTE: the signer cert may not yet exist due to a pending csr
        if (signerCert.exists()) {
            final X509Certificate signerCertificate = (X509Certificate) 
                KeyUtils.loadCertificate(signerCert.getPath());
            persist(signerCertificate, signerCertTargetFile);
            return true;
        } else {
            if (finalIteration) {
                final String message = String.format("Cannot complete signer chain for component '%s'. "
                        + "Signer certificate of component '%s' not available.",
                        componentName, signerComponentName);
                LOG.warn(message);
                messages.add(message);
            }
            
            return false;
        }
    }

    protected int getKeyUsage(String certType) {
        int defaultKeyUsage = 0;
        switch(certType) {
            case CERT_TYPE_CA: defaultKeyUsage = DEFAULT_KEYUSAGE_ISSUER; break;
            case CERT_TYPE_TOKEN_ISSUER: defaultKeyUsage = DEFAULT_KEYUSAGE_TOKEN_ISSUER; break;
            case CERT_TYPE_TLS: defaultKeyUsage = DEFAULT_KEYUSAGE_TLS; break;
            default:
                throw new IllegalArgumentException(String.format("Certificate type '%s' not supported.", certType));
        }
        return getProperty(PROPERTY_CERT_USAGE, defaultKeyUsage);
    }

    protected Calendar getValidityPeriodBegin() {
        // construct default
        final Calendar beginCalendar = Calendar.getInstance(timeZone);
        roundFloor(beginCalendar);
        Date begin = beginCalendar.getTime();

        // overwrite from configuration
        String configuredBegin = getProperty(PROPERTY_CERT_VALID_FROM);
        if (StringUtils.isNotBlank(configuredBegin)) {
            begin = parseDate(configuredBegin);
            beginCalendar.setTime(begin);
            roundFloor(beginCalendar);
            begin = beginCalendar.getTime();
        }
        return beginCalendar;
    }

    protected Calendar getValidityPeriodEnd(final Calendar beginCalendar) {
        int validityInMonths = getProperty(PROPERTY_CERT_VALID_MONTHS, DEFAULT_VALIDITY_IN_MONTHS);
        // construct default (based on begin + validity)
        final Calendar endCalendar = Calendar.getInstance(timeZone);
        endCalendar.set(Calendar.YEAR, beginCalendar.get(Calendar.YEAR));
        endCalendar.set(Calendar.MONTH, beginCalendar.get(Calendar.MONTH));
        endCalendar.add(Calendar.MONTH, validityInMonths);
        endCalendar.set(Calendar.DAY_OF_MONTH, beginCalendar.get(Calendar.DAY_OF_MONTH));
        roundCeiling(endCalendar);

        // overwrite from configuration
        String configuredEnd = getProperty(PROPERTY_CERT_VALID_TO);
        if (StringUtils.isNotBlank(configuredEnd)) {
            Date end = endCalendar.getTime();
            end = parseDate(configuredEnd);
            endCalendar.setTime(end);
            roundCeiling(endCalendar);
            end = endCalendar.getTime();
        }

        return endCalendar;
    }

    protected Date parseDate(String dateString) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(dateString.trim());
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    String.format("Date '%s' could not be parsed. Expecting 'yyyy-MM-dd' formatted date.", dateString));
        }
    }

    protected Project getAntProject() {
        if (antProject == null) {
            antProject = new LoggingProjectAdapter();
        }
        return antProject;
    }
    
    protected X500NameBuilder createNameBuilder(String prefix) {
        final X500NameBuilder nameBuilder = new X500NameBuilder();
        
        String subjectString = getProperty(prefix);
        if (subjectString != null) {
            RDN[] rdns = BCStyle.INSTANCE.fromString(subjectString);
            for (RDN rdn : rdns) {
                if (rdn.isMultiValued()) {
                    nameBuilder.addMultiValuedRDN(rdn.getTypesAndValues());
                } else {
                    nameBuilder.addRDN(rdn.getFirst());
                }
            }
        }
        
        // multiple attributes can be added using an array-like notation
        List<String> objects = new ArrayList<>(componentProperties.stringPropertyNames());
        objects.sort(String.CASE_INSENSITIVE_ORDER);
        for (Object key :objects) {
            final String attributeKey = String.valueOf(key);
            if (attributeKey.startsWith(prefix + ".")) {
                String attributeName = attributeKey.substring(prefix.length() + 1);
                if (attributeName.contains("[")) {
                    attributeName = attributeName.substring(0, attributeName.indexOf("["));
                }
                final ASN1ObjectIdentifier oid = BCStyle.INSTANCE.attrNameToOID(attributeName);
                nameBuilder.addRDN(oid, getProperty(attributeKey));
            }
        }

        // the prefix.CN specifies the main CN. Per default it is the component
        // name in upper case.
        String componentCN = getProperty(prefix + "." + PROPERTY_CN, componentName.toUpperCase());
        nameBuilder.addRDN(BCStyle.CN, componentCN);

        return nameBuilder;
    }

    protected int getProperty(String key, int defaultInt) {
        return Integer.parseInt(getProperty(key, String.valueOf(defaultInt)));
    }

    protected boolean getProperty(String key, boolean defaultBoolean) {
        return Boolean.parseBoolean(getProperty(key, String.valueOf(defaultBoolean)));
    }

    protected String getProperty(String key, String defaultString) {
        return componentProperties.getProperty(key, defaultString);
    }

    protected String getProperty(String key) {
        return componentProperties.getProperty(key);
    }

    public boolean isFinalIteration() {
        return finalIteration;
    }

    public void setFinalIteration(boolean finalIteration) {
        this.finalIteration = finalIteration;
    }

    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void setAntProject(Project antProject) {
        this.antProject = antProject;
    }

    public String getComponentName() {
        return componentName;
    }

    public boolean isProcessed() {
        return processed;
    }
    
    @Override
    public String toString() {
        return getComponentName();
    }

}
