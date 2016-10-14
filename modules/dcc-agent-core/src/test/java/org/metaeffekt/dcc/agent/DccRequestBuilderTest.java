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

import static org.junit.Assert.assertEquals;

import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Test;

public class DccRequestBuilderTest {

    private static final String SCHEME = "https";
    private static final String HOST = "someHost";
    private static final int PORT = 33036;
    
    private DccAgentEndpoint builder;
    
    @Before
    public void prepare() {
        builder = new DccAgentEndpoint(HOST);
    }
    
    @Test
    public void version() {

        HttpUriRequest request = builder.createRequest("version", null);

        assertCommonParts(request);
        assertEquals("GET", request.getMethod());        
        assertEquals("/dcc/version", request.getURI().getPath());
    
    }
    
    @Test
    public void logs() {

        HttpUriRequest request = builder.createRequest("logs/logId", null);

        assertCommonParts(request);
        assertEquals("GET", request.getMethod());
        assertEquals("/dcc/logs/logId", request.getURI().getPath());
        
    }
    
    @Test
    public void state() {

        HttpUriRequest request = builder.createRequest("state", "depId");

        assertCommonParts(request);
        assertEquals("GET", request.getMethod());
        assertEquals("/dcc/depId/state", request.getURI().getPath());

    }
    
    @Test
    public void initialize() {
        
        HttpUriRequest request = builder.createRequest("initialize", "depId", "somePackage", "someUnit");

        assertCommonParts(request);
        assertEquals("PUT", request.getMethod());        
        assertEquals("/dcc/depId/packages/somePackage/units/someUnit/initialize", request.getURI().getPath());
    }
    
    @Test
    public void clean() {
        
        HttpUriRequest request = builder.createRequest("clean", "depId");

        assertCommonParts(request);
        assertEquals("PUT", request.getMethod());        
        assertEquals("/dcc/depId/clean", request.getURI().getPath());
    }
    
    @Test
    public void install() {
        
        HttpUriRequest request = builder.createRequest("install", "depId", "somePackage", "someUnit");

        assertCommonParts(request);
        assertEquals("PUT", request.getMethod());        
        assertEquals("/dcc/depId/packages/somePackage/units/someUnit/install", request.getURI().getPath());
    }
    
    @Test
    public void configure() {
        
        HttpUriRequest request = builder.createRequest("configure", "depId", "somePackage", "someUnit");

        assertCommonParts(request);
        assertEquals("PUT", request.getMethod());
        assertEquals("/dcc/depId/packages/somePackage/units/someUnit/configure", request.getURI().getPath());
    }

    @Test
    public void bootstrap() {

        HttpUriRequest request = builder.createRequest("bootstrap", "depId", "somePackage", "someUnit");

        assertCommonParts(request);
        assertEquals("PUT", request.getMethod());
        assertEquals("/dcc/depId/packages/somePackage/units/someUnit/bootstrap", request.getURI().getPath());
    }
    
    @Test
    public void start() {
        
        HttpUriRequest request = builder.createRequest("start", "depId", "somePackage", "someUnit");
        
        assertCommonParts(request);
        assertEquals("PUT", request.getMethod());
        assertEquals("/dcc/depId/packages/somePackage/units/someUnit/start", request.getURI().getPath());
    }
    
    @Test
    public void stop() {
        
        HttpUriRequest request = builder.createRequest("stop", "depId", "somePackage", "someUnit");
        
        assertCommonParts(request);
        assertEquals("PUT", request.getMethod());
        assertEquals("/dcc/depId/packages/somePackage/units/someUnit/stop", request.getURI().getPath());
    }

    private void assertCommonParts(HttpUriRequest request) {

        assertEquals(SCHEME, request.getURI().getScheme());
        assertEquals(HOST, request.getURI().getHost());
        assertEquals(PORT, request.getURI().getPort());
    }
}
