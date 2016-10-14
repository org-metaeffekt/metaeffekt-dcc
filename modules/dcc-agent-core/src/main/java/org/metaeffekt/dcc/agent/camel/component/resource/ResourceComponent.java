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
package org.metaeffekt.dcc.agent.camel.component.resource;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.component.restlet.RestletBinding;
import org.apache.camel.component.restlet.RestletComponent;
import org.apache.camel.component.restlet.RestletConsumer;
import org.apache.camel.component.restlet.RestletEndpoint;
import org.apache.camel.spi.HeaderFilterStrategyAware;
import org.apache.camel.util.CamelContextHelper;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * A Camel component embedded Restlet that produces and consumes exchanges.
 *
 */
public class ResourceComponent extends RestletComponent implements HeaderFilterStrategyAware,
        InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(RestletComponent.class);

    private Map<String, MethodBasedRouter> routers = new HashMap<String, MethodBasedRouter>();

    private final static String RESOURCE_URI = "resource";

    // TODO: make final once deprecations are resolved
    private Component component;

    /**
     * @deprecated Use constructor with component instead.
     */
    @Deprecated
    public ResourceComponent() {
        super();
    }

    public ResourceComponent(Component component) {
        super(component);
        
        // needs to be preserved on this level, because the component 
        // is otherwise not accessible
        this.component = component;
    }
    
    /**
     * @deprecated Use constructor with component instead.
     */
    @Deprecated
    public void setComponent(Component component) {
        this.component = component;
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters)
            throws Exception {

        RestletBinding restletBinding = null;
        // lookup binding in registry if provided
        String ref = getAndRemoveParameter(parameters, "restletBindingRef", String.class);
        if (ref != null) {
            restletBinding =
                    CamelContextHelper.mandatoryLookup(getCamelContext(), ref,
                            ResourceBinding.class);
        }

        if (restletBinding == null) {
            restletBinding = new ResourceBinding();
        }

        if (restletBinding instanceof HeaderFilterStrategyAware) {
            ((HeaderFilterStrategyAware) restletBinding)
                    .setHeaderFilterStrategy(getHeaderFilterStrategy());
        }

        Method method = getAndRemoveParameter(parameters, "restletMethod", Method.class);
        RestletEndpoint result = new RestletEndpoint(this, remaining);
        
        
        if (method != null) {
            result.setRestletMethod(method);
        }
        
        Method[] restletMethods = getAndRemoveParameter(parameters,
                "restletMethods", Method[].class);
        if (restletMethods != null) {
            result.setRestletMethods(restletMethods);
        }        
        
        result.setRestletBinding(restletBinding);
        
        return result;
    }

    public void connect(RestletConsumer consumer) throws Exception {
        RestletEndpoint endpoint = (RestletEndpoint) consumer.getEndpoint();

        String endpointUri = endpoint.getEndpointUri();
        endpointUri = endpointUri.startsWith("/") ? endpointUri : "/" + endpointUri;
        endpointUri = ensureRestletCompatibleURI(endpointUri); // 
        MethodBasedRouter router = getMethodRouter(endpointUri);

        Restlet target = consumer.getRestlet();

        if (endpoint.getRestletMethods() == null) {
            endpoint.setRestletMethods(new Method[] { endpoint.getRestletMethod() } );
        }
        
        for (Method method : endpoint.getRestletMethods()) {
            router.addRoute(method, target);
            
            LOG.debug("Attached restlet uriPattern: [{}] method: [{}]", endpointUri, method);
        }

        if (!router.hasBeenAttached()) {
            component.getDefaultHost().attach(endpointUri, router);
            LOG.debug("Attached methodRouter uriPattern: [{}]", endpointUri);
        }

    }

    private MethodBasedRouter getMethodRouter(String uriPattern) {
        synchronized (routers) {
            MethodBasedRouter result = routers.get(uriPattern);
            if (result == null) {
                result = new MethodBasedRouter(uriPattern);
                routers.put(uriPattern, result);
            }
            return result;
        }
    }

    public void afterPropertiesSet() throws Exception {
        getCamelContext().addComponent(RESOURCE_URI, this);
    }

    /*
     * Since Camel 2.9, {} in URIs are replaced by (). This has to be reversed, before calling restlet.
     */
    private static String ensureRestletCompatibleURI(String pattern) {
        return pattern == null ? null : pattern.replaceAll("\\(", "{").replaceAll("\\)", "}");
    }
}
