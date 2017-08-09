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

import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.AccessControlException;
import java.util.HashMap;
import java.util.Map;

public class HttpErrorMappingRoutePolicy implements org.apache.camel.spi.RoutePolicy {

    private static final Logger LOG = LoggerFactory.getLogger(HttpErrorMappingRoutePolicy.class);

    private static final int DEFAULT_HTTP_ERROR_CODE = 404;

    private final static Map<String, Integer> DEFAULT_EXCEPTION_MAPPING = new HashMap<String, Integer>();
    static {
        DEFAULT_EXCEPTION_MAPPING.put(AccessControlException.class.getName(), 401);
        DEFAULT_EXCEPTION_MAPPING.put(IllegalArgumentException.class.getName(), 400);
    }

    private Map<String, Integer> exceptionMapping;

    private int defaultErrorCode = DEFAULT_HTTP_ERROR_CODE;

    /**
     * {@inheritDoc}
     */
    protected void doHandleFailedExchange(Exchange exchange, Throwable t) {
        final int errorCode = mapToHttpErrorCode(t);
        exchange.getOut().setBody(String.format("Exception [%s] occured. Message was [%s]", t.getClass().getName(), t.getMessage()));
        exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, (int) errorCode);
    }

    protected Integer mapToHttpErrorCode(Throwable t) {
        final Map<String, Integer> exceptionMapping = getExceptionMapping();
        Integer mappedCode;
        if (exceptionMapping != null) {
            mappedCode = exceptionMapping.get(t.getClass().getName());
        } else {
            mappedCode = DEFAULT_EXCEPTION_MAPPING.get(t.getClass().getName());
        }
        if (mappedCode != null) {
            return mappedCode;
        }
        return defaultErrorCode;
    }

    public Map<String, Integer> getExceptionMapping() {
        return exceptionMapping;
    }

    public void setExceptionMapping(Map<String, Integer> exceptionMapping) {
        this.exceptionMapping = exceptionMapping;
    }

    public int getDefaultErrorCode() {
        return defaultErrorCode;
    }

    public void setDefaultErrorCode(int defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public void onInit(Route route) {

    }

    @Override
    public void onRemove(Route route) {

    }

    @Override
    public void onStart(Route route) {

    }

    @Override
    public void onStop(Route route) {

    }

    @Override
    public void onSuspend(Route route) {

    }

    @Override
    public void onResume(Route route) {

    }

    @Override
    public void onExchangeBegin(Route route, Exchange exchange) {

    }

    @Override
    public void onExchangeDone(Route route, Exchange exchange) {
        if (exchange != null && exchange.isFailed()) {

            Throwable t = exchange.getException();
            if (t == null) {
                // this handles an alternative representation for exceptions in camel
                try {
                    t = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
                } catch (RuntimeException e) {
                    LOG.warn("Unable to handle exception in exchange in route: [{}]", exchange.getFromRouteId());
                    t = e;
                }
            }
            if (t != null) {
                doHandleFailedExchange(exchange, t);
            }
        }
    }
}
