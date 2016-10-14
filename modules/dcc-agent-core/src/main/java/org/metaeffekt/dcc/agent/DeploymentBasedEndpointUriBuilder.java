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


import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;

import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;
import org.metaeffekt.core.commons.annotation.Public;

/**
 * @author Alexander D.
 * @author Jochen K.
 */
@Public
public class DeploymentBasedEndpointUriBuilder extends DccAgentUriBuilder {

    public HttpUriRequest buildHttpUriRequest(Commands command, Id<DeploymentId> deploymentId) {
        return buildHttpUriRequest(command, deploymentId, null);
    }
    
    public HttpUriRequest buildHttpUriRequest(Commands command, Id<DeploymentId> deploymentId, HttpEntity payload) {

        StringBuilder sb = new StringBuilder("/");
        sb.append(PATH_ROOT).append("/");
        sb.append(deploymentId).append("/");
        sb.append(command);
        String path = sb.toString();

        URIBuilder uriBuilder = createUriBuilder();
        uriBuilder.setPath(path);
        URI uri;
        try {
            uri = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        HttpRequestBase request;
        
        if (HTTP_PUT.equals(determineHttpMethod(command))) {
            request = new HttpPut(uri);
            if (payload != null) {
                ((HttpPut)request).setEntity(payload);
            }
        } else {
          request = new HttpGet(uri);  
        }

        if (requestConfig != null) {
            request.setConfig(requestConfig);
        }
        return request;

    }
    
    public String buildRestletResourceUri(Commands command) {
        return "resource:" + PATH_ROOT + "/{" + DEPLOYMENT_ID + "}/" + command + "?restletMethod=" + determineHttpMethod(command);
    }

    private String determineHttpMethod(Commands command) {
        switch (command) {
            case CLEAN:
            case PURGE:
            case INITIALIZE:
                return HTTP_PUT;
            case STATE:
                return HTTP_GET;
            default:
                throw new IllegalArgumentException("Unsupported command [" + command + "]");
        }
    }
}
