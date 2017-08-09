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

import java.io.File;

import org.apache.tools.ant.Project;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Current impl requires an elasitcsearch on localhost:9200")
public class HttpRequestTaskTest {
    
    @Test
    public void execute() {
        Project project = new Project();
        project.setBaseDir(new File("."));
        
        doHttpRequest(project, HttpMethod.GET, "_cat/indices?v", null);
        doHttpRequest(project, HttpMethod.DELETE, "result-store", null);

        String createIndexBody = "{" + 
            "    \"settings\": { }," +
            "    \"mappings\" : { " +
            "        \"result\" : { " +
            "            \"properties\" : { " +
            "                \"expression\" : { \"type\" : \"string\", \"index\" : \"not_analyzed\" }," +
            "                \"subjectId\" : { \"type\" : \"string\", \"index\" : \"not_analyzed\" }," +
            "                \"timestamp\" : { \"type\" : \"date\" }" +
            "            } " +
            "        } " +
            "    }" +
            "}";
        doHttpRequest(project, HttpMethod.PUT, "result-store", createIndexBody);
        doHttpRequest(project, HttpMethod.GET, "_cat/indices?v", null);
        
        doHttpRequest(project, HttpMethod.POST, "_search", "{ \"query\": { \"match_all\": {} } }");
    }

    public HttpRequestTask doHttpRequest(Project project, final HttpMethod httpMethod,
            final String uri, final String body) {
        HttpRequestTask task = new HttpRequestTask();
        task.setProject(project);
        task.setHttpMethod(httpMethod);
        
        task.setServerHostName("localhost");
        task.setServerPort(9200);
        task.setServerScheme("http");
        
        task.setUri(uri);
        task.setBody(body); 
        
        task.setResponseBodyPropertyName("http.response.body");
        task.setResponseStatusCodePropertyName("http.response.status.code");
        task.setResponseStatusReasonPropertyName("http.response.status.reason");
        
        task.execute();
        System.out.println(project.getProperty("http.response.body"));
        return task;
    }

}
