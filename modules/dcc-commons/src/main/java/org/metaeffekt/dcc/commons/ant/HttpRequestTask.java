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
package org.metaeffekt.dcc.commons.ant;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class HttpRequestTask extends Task {

    private String serverHostName = "localhost";

    private int serverPort = 8080;

    private String serverScheme = "http";

    private HttpMethod httpMethod = HttpMethod.GET;

    private String uri;

    private String body;

    private String responseStatusCodePropertyName;

    private String responseStatusReasonPropertyName;

    private String responseBodyPropertyName;
    
    private boolean failOnError = true;

    private String contentType = "text/plain";

    private String username;

    private String password;

    /**
     * Executes the task.
     * 
     * @see org.apache.tools.ant.Task#execute()
     */
    @Override
    public void execute() {

        StringBuilder sb = new StringBuilder();
        sb.append(serverScheme).append("://").append(serverHostName).append(':').append(serverPort);
        sb.append("/").append(uri);
        String url = sb.toString();

        BasicCredentialsProvider credentialsProvider = null;
        if (username != null) {
            log("User: " + username);
            credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(serverHostName, serverPort),
                    new UsernamePasswordCredentials(username, password));
        }

        HttpClient httpClient = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();

        try {
            switch (httpMethod) {
                case GET:
                    HttpGet get = new HttpGet(url);
                    doRequest(httpClient, get);
                    break;
                case PUT:
                    HttpPut put = new HttpPut(url);
                    if (body  == null) {
                        body = "";
                    }
                    log("Setting body: " + body, Project.MSG_DEBUG);
                    put.setEntity(new StringEntity(body, ContentType.create(contentType)));
                    doRequest(httpClient, put);
                    break;
                case POST:
                    HttpPost post = new HttpPost(url);
                    if (body  == null) {
                        body = "";
                    }
                    log("Setting body: " + body, Project.MSG_DEBUG);
                    post.setEntity(new StringEntity(body, ContentType.create(contentType)));
                    doRequest(httpClient, post);
                    break;
                case DELETE:
                    HttpDelete delete = new HttpDelete(url);
                    doRequest(httpClient, delete);
                    break;
                default:
                    throw new IllegalArgumentException("HttpMethod " + httpMethod
                            + " not supported!");
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    public void doRequest(HttpClient httpClient, HttpUriRequest request) throws IOException,
            ClientProtocolException {
        log("Executing request: " + request.toString());
        if (request instanceof HttpEntityEnclosingRequestBase
                && ((HttpEntityEnclosingRequestBase) request).getEntity() != null
                && StringUtils.isNotBlank(body)) {
            log("With request body: " + body);
        } else {
            log("Without request body.");
        }
        HttpResponse response = httpClient.execute(request);
        StatusLine statusLine = response.getStatusLine();
        String responseBody = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        log("Received response: " + responseBody, Project.MSG_DEBUG);

        final String statusCode = String.valueOf(statusLine.getStatusCode());
        final String reasonPhrase = statusLine.getReasonPhrase();

        if (responseStatusCodePropertyName != null) {
            getProject().setProperty(responseStatusCodePropertyName, statusCode);
        }

        if (responseStatusReasonPropertyName != null) {
            getProject().setProperty(responseStatusReasonPropertyName, reasonPhrase);
        }

        if (responseBodyPropertyName != null) {
            getProject().setProperty(responseBodyPropertyName, responseBody);
        }
        
        if (failOnError) {
            if (Integer.parseInt(statusCode) >= 400) {
                throw new BuildException(String.format(
                    "Http request failed. Status code: '%s', reason: '%s'.", statusCode, reasonPhrase));
            }
        }
    }

    public String getServerHostName() {
        return serverHostName;
    }

    public void setServerHostName(String serverHostName) {
        this.serverHostName = serverHostName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getServerScheme() {
        return serverScheme;
    }

    public void setServerScheme(String serverScheme) {
        this.serverScheme = serverScheme;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getResponseStatusCodePropertyName() {
        return responseStatusCodePropertyName;
    }

    public void setResponseStatusCodePropertyName(String responseStatusCodePropertyName) {
        this.responseStatusCodePropertyName = responseStatusCodePropertyName;
    }

    public String getResponseStatusReasonPropertyName() {
        return responseStatusReasonPropertyName;
    }

    public void setResponseStatusReasonPropertyName(String responseStatusReasonPropertyName) {
        this.responseStatusReasonPropertyName = responseStatusReasonPropertyName;
    }

    public String getResponseBodyPropertyName() {
        return responseBodyPropertyName;
    }

    public void setResponseBodyPropertyName(String responseBodyPropertyName) {
        this.responseBodyPropertyName = responseBodyPropertyName;
    }

    
    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
