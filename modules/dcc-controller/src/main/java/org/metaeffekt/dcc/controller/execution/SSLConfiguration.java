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
package org.metaeffekt.dcc.controller.execution;

import org.apache.commons.lang3.Validate;

public class SSLConfiguration {

    private final String keyStorePassword;

    private final String trustStorePassword;

    private final String keyStoreLocation;

    private final String trustStoreLocation;

    public SSLConfiguration(String keyStoreLocation, String keyStorePassword,
            String trustStoreLocation, String trustStorePassword) {
        Validate.notEmpty(keyStoreLocation, "keyStoreLocation is required");
        Validate.notEmpty(trustStoreLocation, "trustStoreLocation is required");

        this.keyStorePassword = keyStorePassword == null ? "" : keyStorePassword;
        this.trustStorePassword = trustStorePassword == null ? "" : trustStorePassword;

        this.keyStoreLocation = keyStoreLocation;
        this.trustStoreLocation = trustStoreLocation;
    }

    public char[] getKeyStorePassword() {
        return keyStorePassword.toCharArray();
    }

    public char[] getTrustStorePassword() {
        return trustStorePassword.toCharArray();
    }

    public String getKeyStoreLocation() {
        return keyStoreLocation;
    }

    public String getTrustStoreLocation() {
        return trustStoreLocation;
    }

}
