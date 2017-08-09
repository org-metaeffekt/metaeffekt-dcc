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

import org.metaeffekt.dcc.commons.ant.wrapper.NodeWrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

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

    /**
     * Evaluates the xpathExrepssion against the file identified by the xmlFileName. If the evaluation does not result
     * in a non-null, non-empty value, the defaultValue is returned.
     * The implementation uses a cache, such that the file is not parsed multiple time when invoked several times.
     *
     * @param xmlFileName
     * @param xpathExpression
     * @param defaultValue
     *
     * @return The result of the evaluation. Contains the default value if the evaluation did not produce a proper
     * result.
     */
    public String evaluate(String xmlFileName, String xpathExpression, String defaultValue) {
        final File xmlFile = validateFile(xmlFileName);
        try {
            final Document doc = parseDocument(xmlFile);
            final XPathExpression expr = xpath.compile(xpathExpression);
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

    public NodeWrapper[] evaluateNodes(String xmlFileName, String xpathExpression) {
        final File xmlFile = validateFile(xmlFileName);
        try {
            final Document doc = parseDocument(xmlFile);
            final XPathExpression expr = xpath.compile(xpathExpression);
            final NodeList nodes  = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            if (nodes == null || nodes.getLength() == 0) {
                return new NodeWrapper[0];
            } else {
                final NodeWrapper[] nodeList = new NodeWrapper[nodes.getLength()];
                for (int i = 0; i < nodes.getLength(); i++) {
                    nodeList[i] = new NodeWrapper(nodes.item(i));
                }
                return nodeList;
            }
        } catch (Exception e) {
            throw new IllegalStateException(String.format(
                    "Cannot evaluate xpath [%s] on file [%s]!", xpathExpression, xmlFileName), e);
        }
    }

    public NodeWrapper evaluateNode(String xmlFileName, String xpathExpression) {
        final File xmlFile = validateFile(xmlFileName);
        try {
            final Document doc = parseDocument(xmlFile);
            final XPathExpression expr = xpath.compile(xpathExpression);
            final Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);
            if (node == null) {
                return null;
            }
            return new NodeWrapper(node);
        } catch (Exception e) {
            throw new IllegalStateException(String.format(
                    "Cannot evaluate xpath [%s] on file [%s]!", xpathExpression, xmlFileName), e);
        }
    }

    private Document parseDocument(File xmlFile) throws IOException, SAXException {
        final String docKey = xmlFile.getCanonicalPath();
        Document doc = documentMap.get(docKey);
        if (doc == null) {
            doc = builder.parse(xmlFile);
            documentMap.put(docKey, doc);
        }
        return doc;
    }

    private File validateFile(String xmlFileName) {
        final File xmlFile = new File(xmlFileName);
        if (!xmlFile.exists() || !xmlFile.isFile()) {
            throw new IllegalStateException(String.
                format("The reference xml source [%s] is not a file or does not exists.", xmlFileName));
        }
        return xmlFile;
    }

}
