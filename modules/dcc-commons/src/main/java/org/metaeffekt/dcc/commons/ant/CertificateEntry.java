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

public class CertificateEntry {

    private String certificateFile;

    private String alias;

    private String chainBaseDir;
    
    private String chain;

    public String getCertificateFile() {
        return certificateFile;
    }

    public void setCertificateFile(String certificateFile) {
        this.certificateFile = certificateFile;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String certificateAlias) {
        this.alias = certificateAlias;
    }

    public String getChain() {
        return chain;
    }

    public void setChain(String chain) {
        this.chain = chain;
    }

    public String getChainBaseDir() {
        return chainBaseDir;
    }

    public void setChainBaseDir(String chainBaseDir) {
        this.chainBaseDir = chainBaseDir;
    }

    @Override
    public String toString() {
        return "CertificateEntry [certificateFile=" + certificateFile + ", certificateAlias=" + alias + "]";
    }
}