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

import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.core.commons.annotation.Public;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.util.Assert;

/**
 * @author Alexander D.
 * @author Jochen K.
 */

@Public
public abstract class DccAgentUriBuilder {

    public static final String DEPLOYMENT_ID = "deploymentId";
    public static final String PACKAGE_ID = "packageId";
    public static final String UNIT_ID = "unitId";
    public static final String COMMAND = "command";
    
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 33036;
    public static final String DEFAULT_PROTOCOL = "https";
    public static final String PATH_ROOT = "dcc";

    protected static final String HTTP_GET = "GET";
    protected static final String HTTP_PUT = "PUT";

    protected String host;
    protected Integer port;
    protected String protocol = DEFAULT_PROTOCOL;
    protected Commands command;
    protected RequestConfig requestConfig;

    public DccAgentUriBuilder withHost(String host) {
        this.host = host;
        return this;
    }
    
    public DccAgentUriBuilder withPort(int port) {
        this.port = port;
        return this;
    }
    
    public DccAgentUriBuilder withTimeout(int timeout) {
        this.requestConfig = RequestConfig.custom().setConnectionRequestTimeout(timeout).build();
        return this;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    protected URIBuilder createUriBuilder() {

        Assert.notNull(host,"No host configured");
        Assert.notNull(port,"No port configured");
        
        URIBuilder builder = new URIBuilder();
        builder
            .setScheme(DEFAULT_PROTOCOL)
            .setHost(host)
            .setPort(port);

        return builder;
    }
}
