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

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;

import org.metaeffekt.core.commons.annotation.Public;

@Public
public class DccAgentEndpoint {
    
    public static final int DEFAULT_PORT = 33036;
    public static final String DEFAULT_PROTOCOL = "https";
    public static final String PATH_ROOT = "dcc";

    private String host;
    private int port;
    private RequestConfig requestConfig;

    public DccAgentEndpoint(String host) {
        this(host, DEFAULT_PORT);
    }

    public DccAgentEndpoint(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public DccAgentEndpoint(String host, int port, int timeout) {
        this.host = host;
        this.port = port;
        this.requestConfig = RequestConfig.custom().setConnectionRequestTimeout(timeout).build();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public HttpUriRequest createRequest(String command, String deploymentId) {
        return createRequest(command, deploymentId, null, null, null);
    }
    
    public HttpUriRequest createRequest(String command, String deploymentId, String packageId, String unitId) {
        return createRequest(command, deploymentId, packageId, unitId, null);
        
    }
    public HttpUriRequest createRequest(String command, String deploymentId, String packageId, String unitId, byte[] payload) {
        
        if ("version".equals(command)) {
            return createGetRequest(command, null);
        } else if ("state".equals(command)) {
            return createGetRequest(command, deploymentId);
        } else  if (command.startsWith("logs")) {
            return createGetRequest(command, deploymentId);
        }
        else {
            return createPutRequest(command, deploymentId, packageId, unitId, payload);
        }
        
    }
    
    private HttpPut createPutRequest(String command, String deploymentId, String packageId, String unitId, byte[] payload) {
        StringBuilder sb = new StringBuilder("/");
        sb.append(deploymentId).append("/");
        if (!"clean".equals(command)) {
            sb.append("packages").append("/").append(packageId).append("/");
            sb.append("units").append("/").append(unitId != null ? unitId + "/" : "");
        }
        String paramsPart = sb.toString();
        
        URIBuilder uriBuilder = getUriBuilder();
        String path = String.format("/%s%s%s", PATH_ROOT, paramsPart, command);
        uriBuilder.setPath(path);
        URI uri;
        try {
            uri = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        HttpPut put = new HttpPut(uri);
        if (payload != null && payload.length>0) {
            put.setEntity(new ByteArrayEntity(payload));
        }
        if (requestConfig != null) {
            put.setConfig(requestConfig);
        }
        return put;
    }
    
    private HttpGet createGetRequest(String command, String deploymentId) {
        
        URIBuilder uriBuilder = getUriBuilder();
        if (deploymentId == null) {
            uriBuilder.setPath(String.format("/%s/%s", PATH_ROOT, command));
        } else {
            uriBuilder.setPath(String.format("/%s/%s/%s", PATH_ROOT, deploymentId, command));
        }
        URI uri;
        try {
            uri = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        HttpGet get = new HttpGet(uri);
        if (requestConfig != null) {
            get.setConfig(requestConfig);
        }
        return get;
        
    }

    private URIBuilder getUriBuilder() {
        
        URIBuilder builder = new URIBuilder();
        builder
            .setScheme(DEFAULT_PROTOCOL)
            .setHost(host)
            .setPort(port);
        
        return builder;
    }
    
}
