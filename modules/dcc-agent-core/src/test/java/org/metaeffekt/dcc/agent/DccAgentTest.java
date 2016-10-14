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
package org.metaeffekt.dcc.agent;

import static org.metaeffekt.dcc.commons.commands.Commands.CLEAN;
import static org.metaeffekt.dcc.commons.commands.Commands.CONFIGURE;
import static org.metaeffekt.dcc.commons.commands.Commands.INITIALIZE;
import static org.metaeffekt.dcc.commons.commands.Commands.INSTALL;
import static org.metaeffekt.dcc.commons.commands.Commands.LOGS;
import static org.metaeffekt.dcc.commons.commands.Commands.STATE;
import static org.metaeffekt.dcc.commons.commands.Commands.VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;
import org.metaeffekt.dcc.commons.domain.Type.PackageId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;

/**
 * @author Alexander D.
 * @author Jochen K.
 */
public class DccAgentTest {

    private static final Logger LOG = LoggerFactory.getLogger(DccAgentTest.class);

    private static final String HOST = "localhost";

    private static final long STARTUP_TIMEOUT = 30000;

    private static final int NETWORK_TIMEOUT = 3000000;

    private static final Id<DeploymentId> DEPLOYMENT_ID = Id.createDeploymentId("phm");

    private static final Id<PackageId> PACKAGE_ID = Id.createPackageId("somePackageId");

    private static final Id<UnitId> UNIT_ID = Id.createUnitId("someUnitId");

    private static DccAgent agent;

    private static int port;

    private HttpClient httpClient;

    private UnitBasedEndpointUriBuilder unitBasedEndpointUriBuilder;

    private DeploymentBasedEndpointUriBuilder deploymentBasedEndpointUriBuilder;

    private HostBasedEndpointUriBuilder hostBasedEndpointUriBuilder;

    private byte[] executionProperties = "somePropertyKey=somePropertyValue".getBytes();

    private Map<String, byte[]> properties = new HashMap<String, byte[]>();

    @BeforeClass
    public static void startup() throws BindException {

        port = findAvailablePort();

        startAgent();

    }

    @AfterClass
    public static void shutdown() throws Exception {
        agent.stop();
    }

    @Before
    public void setUp() throws IOException, GeneralSecurityException {
        properties.put(DccConstants.COMMAND_PROPERTIES, executionProperties);

        this.unitBasedEndpointUriBuilder = (UnitBasedEndpointUriBuilder) new UnitBasedEndpointUriBuilder()
                .withHost(HOST).withPort(port).withTimeout(NETWORK_TIMEOUT);
        this.deploymentBasedEndpointUriBuilder = (DeploymentBasedEndpointUriBuilder) new DeploymentBasedEndpointUriBuilder()
                .withHost(HOST).withPort(port).withTimeout(NETWORK_TIMEOUT);
        this.hostBasedEndpointUriBuilder = (HostBasedEndpointUriBuilder) new HostBasedEndpointUriBuilder()
                .withHost(HOST).withPort(port).withTimeout(NETWORK_TIMEOUT);

        httpClient = newHttpClient();
        File configTargetDir = new File("target/opt");
        FileUtils.deleteQuietly(configTargetDir);
        FileUtils.forceMkdir(configTargetDir);

        File tmpTargetDir = new File("target/tmp/");
        FileUtils.deleteQuietly(tmpTargetDir);
        FileUtils.forceMkdir(tmpTargetDir);
    }

    @Test
    public void cleanInitializeInstallAndConfigure() throws URISyntaxException, IOException,
            InterruptedException {

        HttpResponse response = httpClient.execute(deploymentBasedEndpointUriBuilder
                .buildHttpUriRequest(STATE, DEPLOYMENT_ID));
        assertEquals("GET STATE failed", 200, response.getStatusLine().getStatusCode());
        assertEquals(0, getStates(response).size());

        response = httpClient.execute(deploymentBasedEndpointUriBuilder.buildHttpUriRequest(CLEAN,
                DEPLOYMENT_ID));
        assertEquals("CLEAN failed", 200, response.getStatusLine().getStatusCode());
        EntityUtils.consume(response.getEntity());

        response = httpClient.execute(deploymentBasedEndpointUriBuilder.buildHttpUriRequest(
                INITIALIZE, DEPLOYMENT_ID, new FileEntity(new File("src/test/resources/test.zip"))));
        assertEquals("INITIALIZE failed", 200, response.getStatusLine().getStatusCode());
        EntityUtils.consume(response.getEntity());

        response = httpClient.execute(unitBasedEndpointUriBuilder.buildHttpUriRequest(INSTALL,
                DEPLOYMENT_ID, UNIT_ID, PACKAGE_ID, properties));
        assertEquals("INSTALL failed", 200, response.getStatusLine().getStatusCode());
        EntityUtils.consume(response.getEntity());

        response = httpClient.execute(unitBasedEndpointUriBuilder.buildHttpUriRequest(CONFIGURE,
                DEPLOYMENT_ID, UNIT_ID, PACKAGE_ID, properties));
        assertEquals("CONFIGURE failed", 200, response.getStatusLine().getStatusCode());
        EntityUtils.consume(response.getEntity());

        response = httpClient.execute(deploymentBasedEndpointUriBuilder.buildHttpUriRequest(STATE,
                DEPLOYMENT_ID));
        assertEquals("GET STATE failed", 200, response.getStatusLine().getStatusCode());
        Set<String> states = getStates(response);
        assertEquals(3, states.size());
        assertTrue("Does not contain " + INITIALIZE.toString(),
                states.contains(INITIALIZE.toString()));
        assertTrue("Does not contain " + UNIT_ID + "/" + INSTALL.toString(),
                states.contains(UNIT_ID + "/" + INSTALL.toString()));
        assertTrue("Does not contain " + UNIT_ID + "/" + CONFIGURE.toString(),
                states.contains(UNIT_ID + "/" + CONFIGURE.toString()));

    }

    @Test
    public void getLogs() throws IOException {
        HttpResponse response = httpClient.execute(hostBasedEndpointUriBuilder
                .buildHttpUriRequest(LOGS));
        assertEquals("Expected Http 404 Bad Request", 404, response.getStatusLine().getStatusCode());
    }

    @Test
    public void sendEmptyPayload() throws URISyntaxException, ClientProtocolException, IOException {
        HttpResponse response = httpClient.execute(deploymentBasedEndpointUriBuilder
                .buildHttpUriRequest(INITIALIZE, DEPLOYMENT_ID, null));
        assertEquals("Expected Http 400 Bad Request", 400, response.getStatusLine().getStatusCode());
    }

    @Test
    public void getVersion() throws URISyntaxException, ClientProtocolException, IOException {

        HttpResponse response = httpClient.execute(hostBasedEndpointUriBuilder
                .buildHttpUriRequest(VERSION));

        assertEquals("GET VERSION failed", 200, response.getStatusLine().getStatusCode());

        try (InputStream content = response.getEntity().getContent();
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            IOUtils.copy(content, bos);
            assertEquals("1.4.0", bos.toString());
        }

    }

    @Test
    public void state() throws URISyntaxException, ClientProtocolException, IOException {

        HttpResponse response = httpClient.execute(deploymentBasedEndpointUriBuilder
                .buildHttpUriRequest(STATE, DEPLOYMENT_ID));
        assertEquals("GET STATE failed", 200, response.getStatusLine().getStatusCode());
        assertEquals(0, getStates(response).size());

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

    private static void startAgent() {

        agent = new DccAgent(port);

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        final long begin = System.currentTimeMillis();

        executorService.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    agent.start();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        LOG.info("Waiting for agent to start");
        while (!agent.isStarted()) {

            if (System.currentTimeMillis() - begin > STARTUP_TIMEOUT) {
                throw new IllegalStateException("Failed to start agent. Timeout expired.");
            }

            try {
                Thread.sleep(500);
                LOG.info(".");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        LOG.info("");
        LOG.info("Agent is started");
    }

    private HttpClient newHttpClient() throws GeneralSecurityException, IOException {
        final char[] password = "changeit".toCharArray();

        final KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(DccAgentTest.class.getResourceAsStream("/client.keystore"), password);

        final KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(DccAgentTest.class.getResourceAsStream("/client.truststore"), password);

        final SSLContextBuilder sslContextBuilder = SSLContexts.custom();
        sslContextBuilder.loadKeyMaterial(keyStore, password);
        sslContextBuilder.loadTrustMaterial(trustStore);

        final HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setSslcontext(sslContextBuilder.build());
        builder.setHostnameVerifier(new AllowAllHostnameVerifier());

        final HttpClient client = builder.build();
        return client;
    }

    private static int findAvailablePort() throws BindException {

        for (int p = DccAgentEndpoint.DEFAULT_PORT; p < 65535; p++) {

            try {
                new ServerSocket(p);
                return p;
            } catch (IOException e) {
            }
        }

        throw new BindException("unable to find an available port");

    }

}
