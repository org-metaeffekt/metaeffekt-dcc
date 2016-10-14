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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.xml.transform.dom.DOMSource;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.StringSource;
import org.apache.camel.component.restlet.RestletBinding;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.spi.HeaderFilterStrategyAware;
import org.apache.commons.io.IOUtils;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.representation.StreamRepresentation;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceBinding implements RestletBinding,
        HeaderFilterStrategyAware {
    private HeaderFilterStrategy headerFilterStrategy;

    private static final Logger LOG = LoggerFactory.getLogger(ResourceBinding.class);

    private static final String CHARSET = "charset";
    
    /**
     * Populate Camel message from Restlet request
     * 
     * @param request message to be copied from
     * @param exchange to be populated
     * @throws Exception
     */
    @Override
    public void populateExchangeFromRestletRequest(Request request, Response response, Exchange exchange) throws Exception {

        Message inMessage = exchange.getIn();
        exchange.setPattern(ExchangePattern.InOut);
       
        // extract headers from restlet
        for (Map.Entry<String, Object> entry : request.getAttributes()
                .entrySet()) {
            if (!getHeaderFilterStrategy().applyFilterToExternalHeaders(
                    entry.getKey(), entry.getValue(), exchange)) {

                inMessage.setHeader(entry.getKey(), entry.getValue());
                LOG.debug("Populate exchange from Restlet request header: [{}] value: [{}]"
                        , entry.getKey(), entry.getValue());

            }
        }

        inMessage.setHeader(Exchange.HTTP_METHOD, request.getMethod());
        inMessage.setHeader(Exchange.CONTENT_TYPE, request.getEntity()
                .getMediaType());

        // copy query string to header
        String query = request.getResourceRef().getQuery();

        if (null != query) {
            inMessage.setHeader(Exchange.HTTP_QUERY, query);
        }

        if (request instanceof Request) {
            Request httpRequest = (Request) request;
            if (httpRequest.getHostRef() != null) {
                String host = httpRequest.getHostRef().getHostDomain();
                int port = httpRequest.getHostRef().getHostPort();
                String protocol = "http";
                if (httpRequest.getHostRef().getSchemeProtocol() != null) {
                    protocol = httpRequest.getHostRef().getSchemeProtocol().getSchemeName();
                }
                StringBuilder sb = new StringBuilder(255);
                sb.append(protocol).append("://").append(host);
                // append port only in case it is available (BAS-4218)
                // currently this still allows negative values and zero
                // as port value. But we are doing no validation here.
                // Therefore we treat '-1' explicitly as 'unset port'. 
                if (port != -1) {
                	sb.append(':').append(port);
                }
                sb.append('/');
                inMessage.setHeader(Exchange.HTTP_BASE_URI, sb.toString());
                inMessage.setHeader(Exchange.HTTP_URI, httpRequest.getOriginalRef().getPath());
            }
        }
        
        if (!request.isEntityAvailable()) {
            return;
        }

        inMessage.setBody(request);

        Form form = new Form(query);
        if (form != null) {
            for (Map.Entry<String, String> entry : form.getValuesMap()
                    .entrySet()) {
                // extract body added to the form as the key which has null
                // value
                if (!getHeaderFilterStrategy().applyFilterToExternalHeaders(
                        entry.getKey(), entry.getValue(), exchange)) {

                    inMessage.setHeader(entry.getKey(), entry.getValue());
                    LOG.debug("Populate exchange from Restlet request user header: [{}] value: [{}]"
                                    , entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * Populate Restlet request from Camel message
     * 
     * @param exchange
     *            message to be copied from
     * @param response
     *            to be populated
     */
    @Override
    public void populateRestletResponseFromExchange(Exchange exchange, Response response) {

        // get content type
        final Message out = exchange.getOut();
        MediaType mediaType = out.getHeader(Exchange.CONTENT_TYPE, MediaType.class);
        
        // in case the media type is not set, we try to infer the best
        // media type from the body. 
        // NOTE: this inference is not complete and only covers most common cases
        if (mediaType == null) {
            Object body = out.getBody();
            mediaType = MediaType.TEXT_PLAIN;
            if (body instanceof CharSequence) {
                mediaType = MediaType.TEXT_PLAIN;
            } else if (body instanceof StringSource
                    || body instanceof DOMSource) {
                mediaType = MediaType.TEXT_XML;
            }
        }

        // handle response code
        String responseCode = out.getHeader(Exchange.HTTP_RESPONSE_CODE, String.class);
        if (responseCode != null) {
            response.setStatus(Status.valueOf(Integer.valueOf(responseCode)));
        }

        // copy headers
        for (Map.Entry<String, Object> entry : out.getHeaders().entrySet()) {
            if (!getHeaderFilterStrategy().applyFilterToCamelHeaders(
                    entry.getKey(), entry.getValue(), exchange)) {
                response.getAttributes().put(entry.getKey(), entry.getValue());
            }
        }

        // infer charset (Look for charset parameter in MediaType)
        String characterSet = null;
        Series<Parameter> parameters = mediaType.getParameters();
        for (Parameter parameter : parameters) {
            if (parameter.getName().equalsIgnoreCase(CHARSET)) {
                characterSet = parameter.getValue();
                break;
            }
            if (parameter.getName().equalsIgnoreCase(Exchange.CHARSET_NAME)) {
                characterSet = parameter.getValue();
                break;
            }
        }
        
        // in case no charset can be determined from the media type we look at the
        // exchange properties
        if (characterSet == null) {
            // Use exchange setting
            characterSet = exchange.getProperty(Exchange.CHARSET_NAME, String.class);
            if (characterSet != null) {
                // no charset found, but text/* media type: select UTF-8 as default
                if (mediaType.isCompatible(MediaType.TEXT_ALL)) {
                    characterSet = CharacterSet.UTF_8.getName();
                }
            }            
        }

        final CharacterSet bodyCharSet = CharacterSet.valueOf(characterSet);

        // we do a stream representation of the body. This preserves encodings and the like
        response.setEntity(new StreamRepresentation(mediaType) {

            @Override
            public InputStream getStream() throws IOException {
                return out.getBody(InputStream.class);
            }

            @Override
            public void write(OutputStream outputStream) throws IOException {
                final InputStream stream = getStream();
                if (stream != null) {
                    IOUtils.copy(stream, outputStream);
                }
            }
            
            @Override
            public CharacterSet getCharacterSet() {
                return bodyCharSet;
            }
        });
    }
    
    @Override
    public void populateExchangeFromRestletResponse(Exchange exchange, Response response) throws Exception {
        throw new UnsupportedOperationException(
                "No producer for this component is supported");
    }
    
    @Override
    public void populateRestletRequestFromExchange(Request request, Exchange exchange) {
        throw new UnsupportedOperationException(
                "No producer for this component is supported");
    }

    public HeaderFilterStrategy getHeaderFilterStrategy() {
        return headerFilterStrategy;
    }

    public void setHeaderFilterStrategy(HeaderFilterStrategy strategy) {
        headerFilterStrategy = strategy;
    }
}
