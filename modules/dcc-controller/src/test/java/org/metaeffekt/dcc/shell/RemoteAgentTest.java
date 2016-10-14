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
package org.metaeffekt.dcc.shell;

import static org.metaeffekt.dcc.commons.commands.Commands.STATE;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
import org.junit.Test;

import org.metaeffekt.dcc.agent.DeploymentBasedEndpointUriBuilder;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;

@Ignore
public class RemoteAgentTest {

    private String host = "localhost";
    private int port = 33036;

    private Id<DeploymentId> deploymentId = Id.createDeploymentId("pd-application");

    private static final int NETWORK_TIMEOUT = 3000000;

    @Test
    public void testRemoteAgent() throws GeneralSecurityException, IOException {

        HttpClient httpClient = newHttpClient();

//        UnitBasedEndpointUriBuilder unitBasedEndpointUriBuilder = 
//            (UnitBasedEndpointUriBuilder) new UnitBasedEndpointUriBuilder()
//                .withHost(host).withPort(port).withTimeout(NETWORK_TIMEOUT);

        DeploymentBasedEndpointUriBuilder deploymentBasedEndpointUriBuilder = 
            (DeploymentBasedEndpointUriBuilder) new DeploymentBasedEndpointUriBuilder()
                .withHost(host).withPort(port).withTimeout(NETWORK_TIMEOUT);
        
//        HostBasedEndpointUriBuilder hostBasedEndpointUriBuilder = 
//            (HostBasedEndpointUriBuilder) new HostBasedEndpointUriBuilder()
//                .withHost(host).withPort(port).withTimeout(NETWORK_TIMEOUT);

        HttpResponse response = httpClient
                .execute(deploymentBasedEndpointUriBuilder.buildHttpUriRequest(STATE, deploymentId));
        
        assertEquals("GET STATE failed", 200, response.getStatusLine().getStatusCode());
     
        Set<String> states = getStates(response);
        System.out.println(states);
    }

    private Set<String> getStates(HttpResponse response) throws IOException {

        Set<String> states = new HashSet<>();

        try (ZipInputStream zipStream = new ZipInputStream(response.getEntity().getContent());) {

            ZipEntry zipEntry = zipStream.getNextEntry();

            while (zipEntry != null) {
                String name = zipEntry.getName();
                name = name.substring(0, name.lastIndexOf("."));
                states.add(name);
                if (zipStream.available() > 0) {
                    zipEntry = zipStream.getNextEntry();
                }
            }
        } finally {
            EntityUtils.consume(response.getEntity());
        }

        return states;
    }
    
    private HttpClient newHttpClient() throws GeneralSecurityException, IOException {
        final char[] password = "DYKK8T8m9nKqBRPZ".toCharArray();

        final KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(getClass().getResourceAsStream("/dcc-shell.keystore"), password);

        final KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(getClass().getResourceAsStream("/dcc-shell.truststore"), password);

        final SSLContextBuilder sslContextBuilder = SSLContexts.custom();
        sslContextBuilder.loadKeyMaterial(keyStore, password);
        sslContextBuilder.loadTrustMaterial(trustStore);

        final HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setSslcontext(sslContextBuilder.build());
        builder.setHostnameVerifier(new AllowAllHostnameVerifier());

        final HttpClient client = builder.build();
        return client;
    }

}
