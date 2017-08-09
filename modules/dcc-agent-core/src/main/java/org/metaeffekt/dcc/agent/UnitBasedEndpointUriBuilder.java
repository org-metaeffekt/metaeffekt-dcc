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
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.DeploymentId;
import org.metaeffekt.dcc.commons.domain.Type.PackageId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.core.commons.annotation.Public;

/**
 * @author Alexander D.
 * @author Jochen K.
 */
@Public
public class UnitBasedEndpointUriBuilder extends DccAgentUriBuilder {

    public HttpUriRequest buildHttpUriRequest(Commands command, Id<DeploymentId> deploymentId, Id<UnitId> unitId, Id<PackageId> packageId, Map<String, byte[]> executionProperties) {
        Validate.isTrue(executionProperties != null && !executionProperties.isEmpty());

        StringBuilder sb = new StringBuilder("/");
        sb.append(PATH_ROOT).append("/");
        sb.append(deploymentId).append("/");
        sb.append("packages").append("/").append(packageId).append("/");
        sb.append("units").append("/").append(unitId).append("/");
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
        HttpPut put = new HttpPut(uri);

        final MultipartEntityBuilder multipartBuilder = MultipartEntityBuilder.create();
        for (Map.Entry<String, byte[]> entry : executionProperties.entrySet()) {
            multipartBuilder.addBinaryBody(entry.getKey(), entry.getValue());
        }

        put.setEntity(multipartBuilder.build());
        if (requestConfig != null) {
            put.setConfig(requestConfig);
        }
        return put;
    }

    public String buildRestletResourceUri() {
        return "resource:" + PATH_ROOT + "/{" + DEPLOYMENT_ID + "}/packages/{" + PACKAGE_ID + "}/units/{" 
                + UNIT_ID + "}/{" + COMMAND + "}?restletMethod=" + HTTP_PUT;
    }
}
