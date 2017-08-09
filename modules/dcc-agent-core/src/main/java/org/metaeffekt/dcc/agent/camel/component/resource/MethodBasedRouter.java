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
package org.metaeffekt.dcc.agent.camel.component.resource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple router that routes rest requests.
 */
class MethodBasedRouter extends Restlet {

    private static final Logger LOG = LoggerFactory.getLogger(MethodBasedRouter.class);

    private String uriPattern;

    private Map<Method, Restlet> routes = new ConcurrentHashMap<Method, Restlet>();

    private AtomicBoolean hasBeenAttachedFlag = new AtomicBoolean(false);

    MethodBasedRouter(String uriPattern) {
        this.uriPattern = uriPattern;
    }

    @Override
    public void handle(Request request, Response response) {
        LOG.debug("MethodRouter [{}] received request method: [{}]"
                , uriPattern, request.getMethod());

        Restlet target = routes.get(request.getMethod());
        if (target != null) {
            target.handle(request, response);
        } else {
            LOG.debug("No route for request method: [{}]", request.getMethod());
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        }
    }

    void addRoute(Method method, Restlet target) {
        routes.put(method, target);
    }

    void removeRoute(Method method) {
        routes.remove(method);
    }

    /**
     * This method does "test-and-set" on the underlying flag that indicates whether this router
     * restlet has been attached to a server or not. It is the caller's responsibility to perform
     * the "attach" when this method returns false.
     * 
     * @return true only this method is called the first time.
     */
    boolean hasBeenAttached() {
        return hasBeenAttachedFlag.getAndSet(true);
    }

}
