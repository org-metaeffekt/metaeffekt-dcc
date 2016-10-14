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
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.bouncycastle.operator.OperatorException;

import org.metaeffekt.dcc.commons.pki.CertificateManager;

public class GenerateCertificatesTask extends Task {

    private File baseDir;
    private boolean failOnError = true;
    
    @Override
    public void execute() throws BuildException {
        super.execute();

        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(baseDir);
        directoryScanner.setIncludes(new String[] { "**/config/component.properties"});
        directoryScanner.scan();
        
        final String[] files = directoryScanner.getIncludedFiles();

        final Map<String,  CertificateManager> managers = new HashMap<>();
        
        try {
            // initialize managers for each component
            for (String file : files) {
                File f = new File(baseDir, file);
                f = f.getParentFile(); // config
                f = f.getParentFile(); // component folder
                final CertificateManager certificateManager = 
                    new CertificateManager(baseDir, f.getName());
                certificateManager.setAntProject(getProject());
                managers.put(f.getName(), certificateManager);
            }

            // 1st pass: create public / private keys independently
            for (CertificateManager certificateManager : managers.values()) {
                certificateManager.createOrCompleteKeyPair();
            }

            // 2nd pass: create certificates (recursive to honor dependencies) 
            StringBuilder output = new StringBuilder();
            final String lineSep = System.getProperty("line.separator");
            for (CertificateManager certificateManager : managers.values()) {
                final Set<CertificateManager> processedCertManager = new HashSet<>();
                createCertificates(certificateManager, managers, processedCertManager);
                for (String message : certificateManager.getMessages()) {
                    if (output.length() != 0) {
                        output.append(lineSep);
                    }
                    output.append(message);
                }
            }

            if (failOnError && output.length() > 0) {
                throw new BuildException("Generation of certificates could not be completed:" + lineSep + output.toString());
            }
        } catch (IOException | GeneralSecurityException | OperatorException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Recursive method that that breaks once an issue in the recursive chain is detected.
     */
    protected boolean createCertificates(CertificateManager certificateManager, Map<String, CertificateManager> managers, 
            Set<CertificateManager> processedCertManager)
            throws IOException, GeneralSecurityException, OperatorException, CertificateException {
        boolean hasMessage = false;

        // mark this one as being processed
        processedCertManager.add(certificateManager);
        
        Set<CertificateManager> processedCertManagerSignerChain = new HashSet<>(processedCertManager);
        
        final String issuerComponentName = certificateManager.getIssuerComponentName();
        if (!issuerComponentName.equals(certificateManager.getComponentName())) {
            hasMessage |= createCertificate(certificateManager, issuerComponentName, managers, processedCertManager);
        }
        
        final String signerComponentName = certificateManager.getSignerComponentName(issuerComponentName);
        if (!signerComponentName.equals(certificateManager.getComponentName())) {
            if (!signerComponentName.equals(issuerComponentName)) {
                hasMessage = createCertificate(certificateManager, signerComponentName, managers, processedCertManagerSignerChain);
            }
        }

        if (hasMessage) {
            return true;
        } else {
            certificateManager.setFinalIteration(true);
            certificateManager.createOrComplete();
        }
        
        return certificateManager.getMessages().size() > 0;
    }

    protected boolean createCertificate(CertificateManager certificateManager, String componentName, 
            Map<String, CertificateManager> managers, Set<CertificateManager> processedCertManagers)
            throws IOException, GeneralSecurityException, OperatorException, CertificateException {
        
        CertificateManager nextCertManager = null;
        if (componentName != null) {
            nextCertManager = managers.get(componentName);
        }
        if (nextCertManager != null) {
            if (processedCertManagers.contains(nextCertManager)) {
                throw new IllegalStateException(String.format(
                    "Cycle for issuers in component definition detected. Please revise the configuration for '%s'.", 
                    processedCertManagers));
            }
            processedCertManagers.add(nextCertManager);
            createCertificates(nextCertManager, managers, processedCertManagers);
            return nextCertManager.getMessages().size() > 0;
        }
        return false;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

}
