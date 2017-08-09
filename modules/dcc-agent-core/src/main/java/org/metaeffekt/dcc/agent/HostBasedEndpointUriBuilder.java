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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Alexander D.
 * @author Jochen K.
 */
@Public
public class HostBasedEndpointUriBuilder extends DccAgentUriBuilder {


    public HttpUriRequest buildHttpUriRequest(Commands command) {

        StringBuilder sb = new StringBuilder("/");
        sb.append(PATH_ROOT).append("/");
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
        return new HttpGet(uri);
    }


    public String buildRestletResourceUri(Commands command) {
        return "resource:" + PATH_ROOT + "/" + command + "?restletMethod=" + HTTP_GET;
    }
    
}
