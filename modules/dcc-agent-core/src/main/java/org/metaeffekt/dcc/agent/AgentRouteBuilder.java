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

import static org.metaeffekt.dcc.agent.DccAgentUriBuilder.COMMAND;
import static org.metaeffekt.dcc.agent.DccAgentUriBuilder.DEPLOYMENT_ID;
import static org.metaeffekt.dcc.agent.DccAgentUriBuilder.PACKAGE_ID;
import static org.metaeffekt.dcc.agent.DccAgentUriBuilder.UNIT_ID;
import static org.metaeffekt.dcc.commons.commands.Commands.CLEAN;
import static org.metaeffekt.dcc.commons.commands.Commands.INITIALIZE;
import static org.metaeffekt.dcc.commons.commands.Commands.LOGS;
import static org.metaeffekt.dcc.commons.commands.Commands.PURGE;
import static org.metaeffekt.dcc.commons.commands.Commands.STATE;
import static org.metaeffekt.dcc.commons.commands.Commands.VERSION;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.RoutePolicy;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.DccUtils;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;
import org.metaeffekt.dcc.commons.domain.Type.PackageId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;

/**
 * @author Alexander D.
 * @author Jochen K.
 */
public class AgentRouteBuilder extends RouteBuilder {

    private static final int MAX_FILE_SIZE = 1024 * 1024;

    private static final Logger LOG = LoggerFactory.getLogger(AgentRouteBuilder.class);

    private RoutePolicy routePolicy;

    private String version;

    private PropertySource propertySource;

    private final RestletFileUpload restletFileUpload;
    
    private final AgentScriptExecutor remoteScriptExecutor;

    public AgentRouteBuilder(AgentScriptExecutor scriptExecutor) {
        final DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(MAX_FILE_SIZE);
        factory.setFileCleaningTracker(new FileCleaningTracker());

        restletFileUpload = new RestletFileUpload(factory);
        this.remoteScriptExecutor = scriptExecutor;
    }

    @Override
    public void configure() throws Exception {

        DeploymentBasedEndpointUriBuilder deploymentBasedEndpointUriBuilder = new DeploymentBasedEndpointUriBuilder();
        UnitBasedEndpointUriBuilder unitBasedEndpointUriBuilder = new UnitBasedEndpointUriBuilder();
        HostBasedEndpointUriBuilder hostBasedEndpointUriBuilder = new HostBasedEndpointUriBuilder();
        
        // FIXME JKO does not fit into our URI pattern, resource for Browser usage, move to different RouteBuilder
        from("resource:" + DccAgentEndpoint.PATH_ROOT + "/logs/{logId}?restletMethod=GET").
                routeId("log").routePolicy(getRoutePolicy()).process(new Processor() {
            // NOTE: the standard agent logs are deploymentId unspecific. To address the logs
            //   inside a deployment part an additional endpoint may be required.
            @Override
            public void process(Exchange exchange) throws Exception {
                String logId = notNull((String) exchange.getIn().getHeader("logId"), "logId");
                
                // backward compatibility: wrapper.log renamed to dcc-agent-service.log
                if (logId != null && logId.equals("wrapper.log")) {
                    logId = "dcc-agent-service.log";
                }
                
                if (logId.contains("..")) {
                    throw new IllegalStateException("Leaving log file context is not allowed: " + logId);
                }
                
                LOG.info("Received GET request for [logs] with id [{}].", logId);
                File logFile = new File(System.getProperty(DccAgent.DCC_AGENT_HOME), "logs/" + logId);
                if (logFile.exists()) {
                    exchange.getOut().setBody(new AutoCloseInputStream(new FileInputStream(logFile)));
                }
            }
        });

        from(hostBasedEndpointUriBuilder.buildRestletResourceUri(LOGS)).
        routeId(LOGS.toString()).routePolicy(getRoutePolicy()).process(new Processor() {
            
            @Override
            public void process(Exchange exchange) throws Exception {
                LOG.info("Received GET request for [logs].");
                throw new UnsupportedOperationException("Non! Si! Ohhh!");
            }
        });
        

        from(hostBasedEndpointUriBuilder.buildRestletResourceUri(VERSION)).
        routeId(VERSION.toString()).routePolicy(getRoutePolicy()).process(new Processor() {
            
            @Override
            public void process(Exchange exchange) throws Exception {
                LOG.info("Received GET request for [version].");
                exchange.getOut().setBody(version);
            }
        });
        
        from(deploymentBasedEndpointUriBuilder.buildRestletResourceUri(STATE)).
        routeId(STATE.toString()).routePolicy(getRoutePolicy()).process(new Processor() {

            @Override
            public void process(Exchange exchange) throws Exception {
                LOG.info("Received GET request for [state].");
                Id<DeploymentId> deploymentId = extractDeploymentId(exchange);
                exchange.getOut().setBody(remoteScriptExecutor.getExecutionStateHandler(deploymentId).consolidateState(deploymentId));
            }
        });

        
        from(deploymentBasedEndpointUriBuilder.buildRestletResourceUri(INITIALIZE)).
        routeId(INITIALIZE.toString()).routePolicy(getRoutePolicy()).process(new Processor() {
            
            @Override
            public void process(Exchange exchange) throws Exception {
                LOG.info("Received PUT request for [initialize].");

                Id<DeploymentId> deploymentId = extractDeploymentId(exchange);

                // execute an implicit clean
                remoteScriptExecutor.cleanFilesystemLocations(extractDeploymentId(exchange));

                // ensure the file system is as required
                remoteScriptExecutor.prepareFilesystemLocations(deploymentId);
                prepareSolutionLocation(exchange);
                
                // currently no properties are passed on from the shell. To persist a state
                // we create one with the appropriate name.
                File executionPropertiesFile = createInitializeExecutionProperties(exchange, INITIALIZE.toString());
                remoteScriptExecutor.getExecutionStateHandler(deploymentId).
                    persistStateAfterSuccessfulExecution(deploymentId, INITIALIZE, executionPropertiesFile);
            }
        });
        
        from(deploymentBasedEndpointUriBuilder.buildRestletResourceUri(CLEAN)).
        routeId(CLEAN.toString()).routePolicy(getRoutePolicy()).process(new Processor() {
            
            @Override
            public void process(Exchange exchange) throws Exception {
                LOG.info("Received PUT request for [clean].");
                remoteScriptExecutor.cleanFilesystemLocations(extractDeploymentId(exchange));
            }
        });

        from(deploymentBasedEndpointUriBuilder.buildRestletResourceUri(PURGE)).
                routeId(PURGE.toString()).routePolicy(getRoutePolicy()).process(new Processor() {

            @Override
            public void process(Exchange exchange) throws Exception {
                LOG.info("Received PUT request for [purge].");
                remoteScriptExecutor.purgeFilesystemLocations(extractDeploymentId(exchange));
            }
        });

        from(unitBasedEndpointUriBuilder.buildRestletResourceUri()).
            routeId("execute").routePolicy(getRoutePolicy()).process(new Processor() {

            @Override
            public void process(Exchange exchange) throws Exception {
                String commandString = notNull((String) exchange.getIn().getHeader(COMMAND), COMMAND);
                Id<PackageId> packageId = extractPackageId(exchange);
                Id<UnitId> unitId = extractUnitId(exchange);
                Id<DeploymentId> deploymentId = extractDeploymentId(exchange);
                final Map<String, File> properties = dropOffProperties(exchange);
                File executionPropertiesFile = properties.get(DccConstants.COMMAND_PROPERTIES);
                File prerequisitesPropertiesFile = properties.get(DccConstants.PREREQUISITES_PROPERTIES);

                remoteScriptExecutor.executeScript(deploymentId, packageId, unitId, commandString,
                        executionPropertiesFile, prerequisitesPropertiesFile);
            }
        });
    }


    public PropertySource getPropertySource() {
        return propertySource;
    }

    public void setPropertySource(PropertySource propertySource) {
        this.propertySource = propertySource;
    }

    public RoutePolicy getRoutePolicy() {
        return routePolicy;
    }

    public void setRoutePolicy(RoutePolicy routePolicy) {
        this.routePolicy = routePolicy;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    private File createInitializeExecutionProperties(Exchange exchange, String command) throws IOException {
        File executionProperties =
            new File(remoteScriptExecutor.getTmpDir(extractDeploymentId(exchange)), "initialize");
        FileUtils.write(executionProperties, "");
        return executionProperties;
    }

    private void prepareSolutionLocation(Exchange exchange) throws IOException {
        Request request = (Request) exchange.getIn().getBody();
        Representation entity;

        if (request == null || (entity = request.getEntity()) == null) {
            throw new IllegalArgumentException("Incoming payload not as expected");
        }

        try (InputStream in = new BufferedInputStream(entity.getStream()); ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry = zis.getNextEntry();

            final File solutionDir = remoteScriptExecutor.getSolutionDir(extractDeploymentId(exchange));

            while (entry != null) {

                String fileName = entry.getName();
                File newFile = new File(solutionDir, fileName);
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(newFile))) {
                        IOUtils.copy(zis, fos);
                    }
                }
                entry = zis.getNextEntry();
            }
        }
    }


    private<T> T notNull(T obj, String label) {
        if (obj == null) {
            throw new IllegalArgumentException(String.format("No [%s] parameter found in request", label));
        }
        return obj;
    }

    private Map<String, File> dropOffProperties(Exchange exchange) throws IOException {
        final Map<String, File> properties = new HashMap<String, File>();

        Request request = (Request) exchange.getIn().getBody();
        Representation entity;

        String command = notNull((String) exchange.getIn().getHeader("command"), "command");

        if (request == null || (entity = request.getEntity()) == null) {
            throw new IllegalArgumentException(String.format(
                    "%s request received without execution properties", command));
        }

        File solutionTmpDir = remoteScriptExecutor.getTmpDir(extractDeploymentId(exchange));

        final Id<UnitId> unitId = extractUnitId(exchange);

        if (MediaType.MULTIPART_FORM_DATA.equals(entity.getMediaType(), true)) {

            try {
                for (FileItemIterator iterator = restletFileUpload.getItemIterator(entity); iterator
                        .hasNext();) {
                    final FileItemStream next = iterator.next();
                    String propertyName = null;
                    final String fieldName = next.getFieldName();
                    if (DccConstants.COMMAND_PROPERTIES.equals(fieldName)) {
                        propertyName = DccUtils.propertyFileName(command);
                    } else if (DccConstants.PREREQUISITES_PROPERTIES.equals(fieldName)) {
                        propertyName = DccConstants.PREREQUISITES_PROPERTIES_FILE_NAME;
                    } else {
                        propertyName = DccUtils.propertyFileName(fieldName);
                    }
                    try (InputStream in = next.openStream()) {
                        final File propertiesFile =
                            DccUtils.propertyFile(solutionTmpDir, unitId, propertyName);
                        properties.put(fieldName, storeInFile(propertiesFile, in));
                    }
                }
            } catch (FileUploadException e) {
                // ignore
            } finally {
                IOUtils.closeQuietly(entity.getStream());
            }
        } else {
            try (InputStream in = entity.getStream()) {
                final File propertiesFile =
                    DccUtils.propertyFile(solutionTmpDir, unitId,
                            DccUtils.propertyFileName(command));
                properties.put(DccConstants.COMMAND_PROPERTIES, storeInFile(propertiesFile, in));
            }
        }

        return properties;
    }

    private File storeInFile(File propertiesFile, InputStream in) throws IOException, FileNotFoundException {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(propertiesFile))) {
            if (in == null) {
                throw new IllegalArgumentException("Incoming payload not as expected");
            }

            IOUtils.copy(in, out);
        }

        return propertiesFile;
    }

    private Id<DeploymentId> extractDeploymentId(Exchange exchange) {
        return Id.createDeploymentId((String) exchange.getIn().getHeader(DEPLOYMENT_ID));
    }

    private Id<PackageId> extractPackageId(Exchange exchange) {
        return Id.createPackageId((String) exchange.getIn().getHeader(PACKAGE_ID));
    }

    private Id<UnitId> extractUnitId(Exchange exchange) {
        return Id.createUnitId((String) exchange.getIn().getHeader(UNIT_ID));
    }

}
