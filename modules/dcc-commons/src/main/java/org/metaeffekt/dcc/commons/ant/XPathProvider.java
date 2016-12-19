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
package org.metaeffekt.dcc.commons.ant;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enables to evaluate xpaths from a velocity template.
 */
public class XPathProvider {

    private final DocumentBuilderFactory factory;
    private final DocumentBuilder builder;
    private final XPathFactory xPathfactory;
    private final XPath xpath;

    public XPathProvider() {
        try {
            factory = DocumentBuilderFactory.newInstance();
            builder = factory.newDocumentBuilder();
            xPathfactory = XPathFactory.newInstance();
            xpath = xPathfactory.newXPath();
        } catch(ParserConfigurationException e) {
            throw new IllegalStateException("Cannot intitialize XML parser.", e);
        }
    }

    private Map<String, Document> documentMap = Collections.synchronizedMap(new HashMap<>());

    public String evaluate(String xmlFileName, String xpathExpression) {
        return evaluate(xmlFileName, xpathExpression, null);
    }

    public String evaluate(String xmlFileName, String xpathExpression, String defaultValue) {

        final File xmlFile = new File(xmlFileName);
        if (!xmlFile.exists() || !xmlFile.isFile()) {
            throw new IllegalStateException(String.
                format("The reference xml source [%s] is not a file or does not exists.", xmlFileName));
        }

        try {
            final String docKey = xmlFile.getCanonicalPath();
            Document doc = documentMap.get(docKey);
            if (doc == null) {
                doc = builder.parse(xmlFile);
                documentMap.put(docKey, doc);
            }
            XPathExpression expr = xpath.compile(xpathExpression);
            String value = (String) expr.evaluate(doc, XPathConstants.STRING);

            if (value == null || value.isEmpty()) {
                return defaultValue;
            } else {
                return value;
            }
        } catch (Exception e) {
            throw new IllegalStateException(String.format(
                    "Cannot evaluate xpath [%s] on file [%s]!", xpathExpression, xmlFileName), e);
        }
    }

}
