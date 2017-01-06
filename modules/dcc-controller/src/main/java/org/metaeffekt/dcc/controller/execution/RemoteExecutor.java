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
package org.metaeffekt.dcc.controller.execution;

import static org.metaeffekt.dcc.commons.commands.Commands.CLEAN;
import static org.metaeffekt.dcc.commons.commands.Commands.INITIALIZE;
import static org.metaeffekt.dcc.commons.commands.Commands.PURGE;
import static org.metaeffekt.dcc.commons.commands.Commands.STATE;
import static org.metaeffekt.dcc.commons.commands.Commands.VERSION;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import org.metaeffekt.dcc.agent.DccAgentEndpoint;
import org.metaeffekt.dcc.agent.DeploymentBasedEndpointUriBuilder;
import org.metaeffekt.dcc.agent.HostBasedEndpointUriBuilder;
import org.metaeffekt.dcc.agent.UnitBasedEndpointUriBuilder;
import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.DccUtils;
import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;
import org.metaeffekt.dcc.commons.domain.Type.HostName;
import org.metaeffekt.dcc.commons.domain.Type.PackageId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.execution.Executor;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.controller.DccControllerConstants;

/**
 * @author Alexander D.
 * @author Douglas B.
 * @author Jochen K.
 */
public class RemoteExecutor extends BaseExecutor implements Executor {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteExecutor.class);

    private static final Set<String> INCLUDED_FOLDERS = new HashSet<String>();
    static {
        INCLUDED_FOLDERS.add(DccConstants.LIB_SUB_DIRECTORY); 
        INCLUDED_FOLDERS.add(DccConstants.PACKAGES_SUB_DIRECTORY); 
        INCLUDED_FOLDERS.add(DccConstants.HOOKS_SUB_DIRECTORY); 
        INCLUDED_FOLDERS.addAll(Arrays.asList(SOLUTION_ADDON_FOLDERS)); 
    }
    
    private static final int DEFAULT_TIMEOUT = 30000; // 30 seconds

    private final SSLConfiguration sslConfiguration;

    private final DccAgentEndpoint agentEndpoint;

    private UnitBasedEndpointUriBuilder unitBasedEndpointUriBuilder;

    private DeploymentBasedEndpointUriBuilder deploymentBasedEndpointUriBuilder;

    private HostBasedEndpointUriBuilder hostBasedEndpointUriBuilder;

    public RemoteExecutor(ExecutionContext executionContext, String host, int port) {
        super(executionContext);
        Validate.notBlank(host, "Please provide a host name when creating a remote proxy.");

        if (getExecutionContext().getTargetBaseDir() == null) {
            File targetBaseDir = getWorkingTmpDirectory();
            getExecutionContext().setTargetBaseDir(targetBaseDir);
        }

        if (port == 0) {
            port = DccAgentEndpoint.DEFAULT_PORT;
            LOG.debug("Setting agent port to [{}]", port);
        }

        this.unitBasedEndpointUriBuilder = (UnitBasedEndpointUriBuilder) new UnitBasedEndpointUriBuilder()
                .withHost(host).withPort(port).withTimeout(DEFAULT_TIMEOUT);
        this.deploymentBasedEndpointUriBuilder = (DeploymentBasedEndpointUriBuilder) new DeploymentBasedEndpointUriBuilder()
                .withHost(host).withPort(port).withTimeout(DEFAULT_TIMEOUT);
        this.hostBasedEndpointUriBuilder = (HostBasedEndpointUriBuilder) new HostBasedEndpointUriBuilder()
                .withHost(host).withPort(port).withTimeout(DEFAULT_TIMEOUT);

        this.agentEndpoint = new DccAgentEndpoint(host, port, DEFAULT_TIMEOUT);
        this.sslConfiguration = executionContext.getSslConfiguration();
    }

    @Override
    public void purge() {
        // FIXME: validate that all units are uninstalled
        // - go through all units on this host (supporting uninstall command)
        // - fail in case a unit is started (force the user to execute stop first)
        
        // NOTE: currently only called, when uninstall was executed for all units 
        //   in a solution. Some of the concerns above are thereby mitigated.
        executeDeploymentBasedCommand(PURGE);
    }

    @Override
    public void clean() {
        // FIXME: validate that all processes are stopped (not running) before executing clean
        // - go through all units on this host (supporting start command)
        // - fail in case a unit is started (force the user to execute stop first)
        executeDeploymentBasedCommand(CLEAN);
        super.clean();
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        logCommand(INITIALIZE);

        // FIXME: consider the fact that clean will perform, while initialize could fail
        //   this would leave the host (with potentially started processes and running
        //   process watchdog) in an inconsistent state (start scripts can no longer be
        //   periodically executed). Potentially another approach is required to keep
        //   the watchdog operational.
        
        DccUtils.prepareFoldersForWriting(getWorkingTmpDirectory(), getStateCacheDirectory());

        final Id<DeploymentId> deploymentId = getExecutionContext().getProfile().getDeploymentId();

        HttpEntity zipFile = createZipFileOfSolutionLocation();
        HttpUriRequest initializeRequest = deploymentBasedEndpointUriBuilder.buildHttpUriRequest(
                INITIALIZE, deploymentId, zipFile);
        executeRequest(initializeRequest, new Callback() {

            @Override
            public void process(HttpResponse response) {
                if (200 == response.getStatusLine().getStatusCode()) {
                    LOG.debug("Initialize successfully executed against host [{}:{}]",
                            deploymentBasedEndpointUriBuilder.getHost(),
                            deploymentBasedEndpointUriBuilder.getPort());
                } else {
                    LOG.warn(
                            "Unexpected response while executing initialize against the host [{}:{}]",
                            deploymentBasedEndpointUriBuilder.getHost(),
                            deploymentBasedEndpointUriBuilder.getPort());
                }

            }

        });
    }

    public boolean hostAvailable() {
        LOG.debug("Checking if host [{}:{}] is available. ", hostBasedEndpointUriBuilder.getHost(),
                hostBasedEndpointUriBuilder.getPort());
        HttpUriRequest getVersion = hostBasedEndpointUriBuilder.buildHttpUriRequest(VERSION);
        try (final CloseableHttpClient httpClient = instantiateHttpClientWithTimeout()) {
            HttpResponse response = httpClient.execute(getVersion);
            if (response != null) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    try (InputStream content = response.getEntity().getContent();
                            ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                        IOUtils.copy(content, bos);
                        String version = bos.toString();
                        LOG.info("Successfully connected to [{}:{}] with version [{}].",
                                hostBasedEndpointUriBuilder.getHost(),
                                hostBasedEndpointUriBuilder.getPort(), version);
                        
                        // validate that the version conforms to the client
                        if (!version.matches(DccControllerConstants.DCC_SHELL_SUPPORTED_AGENT_VERSION_PATTERN)) {
                            throw new IllegalStateException(String.format("The version of the agent [%s] is not as expected. "
                                + "Please ensure an agent in version [%s] or later is running on [%s:%s].",
                                version,
                                DccControllerConstants.DCC_SHELL_RECOMMENED_AGENT_VERSION,
                                hostBasedEndpointUriBuilder.getHost(),
                                hostBasedEndpointUriBuilder.getPort()));
                        }
                        
                        if (DccControllerConstants.DCC_SHELL_WARN_AGENT_VERSIONS.contains(version)) {
                            LOG.warn("The current version of the connected agent [{}] on host [{}] is compatible, but not recommended."
                                + " Please upgrade the agent to version [{}].", version, hostBasedEndpointUriBuilder.getHost(),
                                DccControllerConstants.DCC_SHELL_RECOMMENED_AGENT_VERSION);
                        }
                    } finally {
                        consumeEntity(response);
                    }
                    return true;
                } else {
                    LOG.warn("Problem connecting to [{}:{}] - return code was [{}]",
                            hostBasedEndpointUriBuilder.getHost(),
                            hostBasedEndpointUriBuilder.getPort(), statusCode);
                    consumeEntity(response);
                    return false;
                }
            } else {
                LOG.warn("Problem connecting to [{}:{}] - no response.",
                        hostBasedEndpointUriBuilder.getHost(),
                        hostBasedEndpointUriBuilder.getPort());
                return false;
            }
        } catch (IOException | GeneralSecurityException e) {
            LOG.error(String.format("Problem connecting to [%s:%s] - execution resulted in exception.",
                hostBasedEndpointUriBuilder.getHost(), hostBasedEndpointUriBuilder.getPort()),
                e);
            return false;
        }
    }

    @Override
    public void retrieveLogs() {
        retrieveLog("dcc-agent.log");
        if (!retrieveLog("dcc-agent-service.log")) {
            retrieveLog("wrapper.log");
        }
    }

    @Override
    public void retrieveUpdatedState() {
        final Id<DeploymentId> deploymentId = getExecutionContext().getProfile().getDeploymentId();
        final Id<HostName> host = Id.createHostName(deploymentBasedEndpointUriBuilder.getHost());

        HttpUriRequest stateRequest = deploymentBasedEndpointUriBuilder.
            buildHttpUriRequest(STATE, deploymentId);
        LOG.debug("Retrieving current execution state from [{}:{}]",
                deploymentBasedEndpointUriBuilder.getHost(),
                deploymentBasedEndpointUriBuilder.getPort());
        executeRequest(stateRequest, new Callback() {

            @Override
            public void process(HttpResponse response) {
                try {
                    getExecutionStateHandler().updateConsolidatedState(
                            response.getEntity().getContent(), host, deploymentId);
                } catch (IOException e) {
                    LOG.error(String.format("Failed to retrieve current state from [%s]",
                            host), e);
                }
            }
        });
    }

    public void execute(final Commands command, final ConfigurationUnit unit) {
        
        // verify initialize (static host-level precondition) was executed
//        Id<HostName> host = Id.createHostName(unitBasedEndpointUriBuilder.getHost());
//        if (!getExecutionStateHandler().alreadySuccessfullyExecuted(
//                host, Commands.INITIALIZE, host, getExecutionContext().getProfile().getDeploymentId())) {
//            throw new IllegalStateException(
//                String.format("Unit based command [%s] requires that the commands [%s] is executed first.", 
//                command, Commands.INITIALIZE));
//        }
        
        logCommand(command, unit);

        final Id<DeploymentId> deploymentId = getExecutionContext().getProfile()
                .getDeploymentId();

        Id<PackageId> packageId = getExecutionContext().getPackageId(unit, command);
        Id<UnitId> unitId = unit.getId();

        HttpUriRequest request = unitBasedEndpointUriBuilder.buildHttpUriRequest(command,
                deploymentId, unitId, packageId, loadProperties(unitId, command));

        LOG.debug("    Invoking: [{}]", request.getURI());
        executeRequest(request, new Callback() {

            @Override
            public void process(HttpResponse response) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (200 == statusCode) {
                    LOG.debug("Command [{}] for unit [{}] successfully executed against host [{}:{}].",
                            command, unit.getId(), unitBasedEndpointUriBuilder.getHost(),
                            unitBasedEndpointUriBuilder.getPort());
                } else {
                    throw new RuntimeException(
                            String.format(
                                    "Unexpected response while executing [%s] for unit [%s] against the host [%s:%s]"
                                            + " - status code was [%s]. Remote exception message: [{%s}]",
                                            command.toString(), unit.getId(),
                                            unitBasedEndpointUriBuilder.getHost(),
                                            unitBasedEndpointUriBuilder.getPort(), statusCode,
                                            getExceptionMessageFromStream(response)));
                }
            }
        });
    }

    private Map<String, byte[]> loadProperties(Id<UnitId> unitId, Commands command) {
        final Map<String, byte[]> properties = new HashMap<String, byte[]>();
        properties.put(DccConstants.COMMAND_PROPERTIES, loadExecutionProperties(unitId, command));
        properties.put(DccConstants.PREREQUISITES_PROPERTIES, loadPrerequisitesProperties(unitId));

        return properties;
    }

    protected void logCommand(Commands command) {
        LOG.info("  Executing command [{}] on host [{}:{}]",
                command, unitBasedEndpointUriBuilder.getHost(),
                unitBasedEndpointUriBuilder.getPort());
    }

    protected void logCommand(Commands command, ConfigurationUnit unit) {
        String unitId = (unit == null ? "none" : unit.getId().getValue());
        LOG.info("  Executing command [{}] for unit [{}] on host [{}:{}]",
                command, unitId, unitBasedEndpointUriBuilder.getHost(),
                unitBasedEndpointUriBuilder.getPort());
    }

    private String getExceptionMessageFromStream(HttpResponse response) {
        ByteArrayOutputStream bos = null;
        String result = null;
        try {
            InputStream ios = response.getEntity().getContent();
            if (ios != null) {
                bos = new ByteArrayOutputStream();
                IOUtils.copy(ios, bos);
                result = bos.toString();
            }
        } catch (IOException e) {
            result = "Failed to parse exception message: " + e.getMessage();
        } finally {
            IOUtils.closeQuietly(bos);
        }

        return result;
    }

    private void consumeEntity(HttpResponse response) {
        try {
            if (response != null) {
                EntityUtils.consume(response.getEntity());
            }
        } catch (IOException e) {
            LOG.warn("Error while trying to consume response entity", e);
        }
    }

    private HttpEntity createZipFileOfSolutionLocation() {
        final ContentProducer contentProducer = new ContentProducer() {

            @Override
            public void writeTo(OutputStream outstream) throws IOException {
                try (ZipOutputStream zipFile = new ZipOutputStream(new BufferedOutputStream(outstream))) {
                    Path solutionDirectory = Paths.get(getExecutionContext().getSolutionDir()
                            .getAbsolutePath());
                    addDirectoryContentsToZipFile(solutionDirectory, solutionDirectory, zipFile,
                            INCLUDED_FOLDERS);
                    zipFile.finish();
                }
                outstream.flush();
            }
        };

        return new EntityTemplate(contentProducer);
    }

    private void addDirectoryContentsToZipFile(Path rootDirectory, Path directory,
            ZipOutputStream zipFile, Set<String> includes) throws IOException {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path path : directoryStream) {
                Path relativePath = rootDirectory.relativize(path);
                if (CollectionUtils.isEmpty(includes)
                        || includes.contains(relativePath.toString())) {
                    if (Files.isDirectory(path)) {
                        String name = relativePath.toString();
                        name = name.endsWith("/") ? name : name + "/";
                        zipFile.putNextEntry(new ZipEntry(replaceWindowsPathSeparator(name)));
                        addDirectoryContentsToZipFile(rootDirectory, path, zipFile, null);
                    } else {
                        String name = relativePath.toString();
                        zipFile.putNextEntry(new ZipEntry(replaceWindowsPathSeparator(name)));
                        try (InputStream fileToBeZipped = new BufferedInputStream(
                                Files.newInputStream(path))) {
                            IOUtils.copy(fileToBeZipped, zipFile);
                        }
                    }
                }
            }
        }
    }

    private String replaceWindowsPathSeparator(String input) {
        return input.replace('\\', '/');
    }

    private void executeRequest(HttpUriRequest request, Callback callback) {
        try (CloseableHttpClient httpClient = instantiateHttpClientWithTimeout()) {
            try (final CloseableHttpResponse response = httpClient.execute(request)) {
                try {
                    callback.process(response);
                } finally {
                    consumeEntity(response);
                }
            }
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Problem executing request!", e);
        }
    }

    private CloseableHttpClient instantiateHttpClientWithTimeout() throws IOException,
            GeneralSecurityException {
        final KeyStore keyStore = loadKeyStore(sslConfiguration.getKeyStoreLocation(),
                sslConfiguration.getKeyStorePassword());

        final KeyStore trustStore = loadKeyStore(sslConfiguration.getTrustStoreLocation(),
                sslConfiguration.getTrustStorePassword());

        final SSLContextBuilder sslContextBuilder = SSLContexts.custom();
        sslContextBuilder.loadKeyMaterial(keyStore, sslConfiguration.getKeyStorePassword());
        sslContextBuilder.loadTrustMaterial(trustStore);

        final HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setSslcontext(sslContextBuilder.build());
        builder.setHostnameVerifier(new AllowAllHostnameVerifier());

        final CloseableHttpClient client = builder.build();
        return client;
    }

    private KeyStore loadKeyStore(String location, char[] password) throws IOException,
            GeneralSecurityException {
        InputStream in = null;
        try {
            in = new FileInputStream(location);

            final KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(in, password);

            return keyStore;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private byte[] loadExecutionProperties(Id<UnitId> unitId, Commands command) {
        final File baseFolder = getConfigTmpDirectory();
        final File file = DccUtils.propertyFile(baseFolder, unitId, command);
        return loadProperties(file);
    }

    private byte[] loadPrerequisitesProperties(Id<UnitId> unitId) {
        final File baseFolder = getConfigTmpDirectory();
        final File file = DccUtils.propertyFile(baseFolder, unitId,
            DccConstants.PREREQUISITES_PROPERTIES_FILE_NAME);
        return loadProperties(file);
    }

    private byte[] loadProperties(File file) {
        if (!file.exists()) {
            return new byte[0];
        }

        try (FileInputStream fis = new FileInputStream(file);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            IOUtils.copy(fis, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean retrieveLog(final String logName) {
        final boolean[] semaphore = new boolean[1];
        HttpUriRequest retrieveAgentLogRequest = agentEndpoint.createRequest("logs/" + logName, null);
        executeRequest(retrieveAgentLogRequest, new Callback() {
            @Override
            public void process(HttpResponse response) {
                String host = agentEndpoint.getHost();
                File logsBaseDir = getExecutionContext().getLogsBaseDir();
                File logsDir = new File(logsBaseDir, host);
                logsDir.mkdirs();
                File localLog = new File(logsDir, logName);
                try {
                    final InputStream content = response.getEntity().getContent();
                    if (content != null) {
                        try (FileOutputStream fos = new FileOutputStream(localLog)) {
                            IOUtils.copy(content, fos);
                        }
                        LOG.debug("Log file [{}] written to folder [{}].", logName, logsDir);
                        semaphore[0] = true;
                    } else {
                        LOG.debug("Log file [{}] not available on remote host.", logName);
                    }
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        });
        return semaphore[0];
    }

    private void executeDeploymentBasedCommand(final Commands command) {
        logCommand(command);
        final Id<DeploymentId> deploymentId = getExecutionContext().getProfile().getDeploymentId();
        HttpUriRequest request = deploymentBasedEndpointUriBuilder.buildHttpUriRequest(
                command, deploymentId);

        executeRequest(request, new Callback() {

            @Override
            public void process(HttpResponse response) {
                if (200 == response.getStatusLine().getStatusCode()) {
                    LOG.debug("Command [{}] successfully executed against host [{}:{}]", command,
                            deploymentBasedEndpointUriBuilder.getHost(),
                            deploymentBasedEndpointUriBuilder.getPort());
                } else {
                    LOG.warn("Unexpected response while executing [{}] against the host [{}:{}]",
                            command,
                            deploymentBasedEndpointUriBuilder.getHost(),
                            deploymentBasedEndpointUriBuilder.getPort());
                }
            }
        });

    }

    @Override
    public void initializeUpgrade() {
        throw new UnsupportedOperationException("This operartion is not supported.");
    }

    private static interface Callback {
        void process(HttpResponse response);
    }

    @Override
    public String getDisplayName() {
        String host = null;
        if (hostBasedEndpointUriBuilder != null) {
            host = hostBasedEndpointUriBuilder.getHost();
        }
        if (deploymentBasedEndpointUriBuilder != null) {
            host = deploymentBasedEndpointUriBuilder.getHost();
        }
        return host;
    }

}
