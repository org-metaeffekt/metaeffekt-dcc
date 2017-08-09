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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MergeXmlTask extends Task {

    private File inputFile;
    private File fragmentFile;
    private File outputFile;

    private String parentNode;              // mandatory: either "parentNode"
    private String succeedingSiblingNode;   //            or "succeedingSiblingNode"
    private String fragmentNodes;

    private DocumentBuilderFactory documentBuilderFactory;
    private DocumentBuilder documentBuilder;
    private TransformerFactory transformerFactory;
    private Transformer transformer;
    private XPathFactory xPathfactory;

    private void initialize() {
        try {
            if (documentBuilderFactory == null) {
                documentBuilderFactory = DocumentBuilderFactory.newInstance();
                documentBuilderFactory.setNamespaceAware(true);
            }
            if (documentBuilder == null) {
                documentBuilder = documentBuilderFactory.newDocumentBuilder();
            }
            if (transformerFactory == null) {
                transformerFactory = TransformerFactory.newInstance();
            }
            if (transformer == null) {
                transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            }
            if (xPathfactory == null) {
                xPathfactory = XPathFactory.newInstance();
            }
        } catch (IllegalArgumentException | ParserConfigurationException | TransformerConfigurationException
                | TransformerFactoryConfigurationError e) {
            throw new BuildException("Cannot initialize XML infrastructure.", e);
        }
    }

    @Override
    public void execute() throws BuildException {
        super.execute();

        Validate.notNull(inputFile, "inputFile");
        Validate.notNull(fragmentFile, "fragmentFile");
        Validate.notNull(outputFile, "outputFile");

        Validate.isTrue(inputFile.exists(), "Required file does not exist: %s", inputFile);
        Validate.isTrue(fragmentFile.exists(), "Required file does not exist: %s", fragmentFile);
        Validate.isTrue(
                isNotBlank(parentNode) ^ isNotBlank(succeedingSiblingNode), 
                "Either use parentNode or succeedingSiblingNode (XOR) ... this specifies the MergeStrategy: "
                + "%s, %s", parentNode, succeedingSiblingNode);
        
        initialize();

        try {
            Document inputDocument = documentBuilder.parse(inputFile);
            final NodeList nodesToMerge = extractNodesToMerge();

            createMergeProcessor(inputDocument)
                .merge(inputDocument, nodesToMerge);

            saveToFile(inputDocument, outputFile);
        } catch (SAXException | IOException | XPathExpressionException | TransformerException e) {
            throw new BuildException("Cannot merge XML documents.", e);
        }
    }

    private NodeList extractNodesToMerge() throws SAXException, IOException, XPathExpressionException {
        Document fragmentDocument = documentBuilder.parse(fragmentFile);

        XPath nodesToMergeXPath = xPathfactory.newXPath();
        XPathExpression nodesToMergeExpression = nodesToMergeXPath.compile(fragmentNodes);
        final NodeList nodesToMerge = (NodeList) nodesToMergeExpression.evaluate(fragmentDocument, XPathConstants.NODESET);
        return nodesToMerge;
    }

    protected void saveToFile(Document document, File outputFile) throws XPathExpressionException, TransformerException {
        XPath xPath = xPathfactory.newXPath();
        NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']", 
            document, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node node = nodeList.item(i);
            node.getParentNode().removeChild(node);
        }

        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(outputFile);

        transformer.transform(source, result);
    }

    protected void displayNodeList(final NodeList parentNodeList) {
        System.out.println(parentNodeList.getLength());
        for (int i = 0; i < parentNodeList.getLength(); i++) {
            System.out.println(parentNodeList.item(i));
            System.out.println(parentNodeList.item(i).getNamespaceURI());
        }
    }

    public File getInputFile() {
        return inputFile;
    }

    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    public File getFragmentFile() {
        return fragmentFile;
    }

    public void setFragmentFile(File fragmentFile) {
        this.fragmentFile = fragmentFile;
    }
    
    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public String getParentNode() {
        return parentNode;
    }

    public void setParentNode(String parentNode) {
        this.parentNode = parentNode;
    }

    public String getFragmentNodes() {
        return fragmentNodes;
    }

    public void setFragmentNodes(String fragmentNodes) {
        this.fragmentNodes = fragmentNodes;
    }

    public String getSucceedingSiblingNode() {
        return succeedingSiblingNode;
    }

    public void setSucceedingSiblingNode(String aNode) {
        this.succeedingSiblingNode = aNode;
    }

    private MergeProcessor createMergeProcessor(Document inputDocument) throws XPathExpressionException {
        if (!StringUtils.isBlank(succeedingSiblingNode)) {
            return new SuceedingSiblingMergeProcessor(
                            resolveToUniqueNode(inputDocument, succeedingSiblingNode));
        } else {
            return new ParentNodeMergeProcessor(
                            resolveToUniqueNode(inputDocument, parentNode));
        }
    }

    private Node resolveToUniqueNode(Document inputDocument, String specifiedNodeXpathString)
        throws XPathExpressionException {
        XPath xPath = xPathfactory.newXPath();
        XPathExpression nodeExpression = xPath.compile(specifiedNodeXpathString);
        final NodeList nodeList = (NodeList) nodeExpression.evaluate(inputDocument, XPathConstants.NODESET);
        Validate.isTrue(
                nodeList.getLength() == 1,
                "The xpath expression identifying the target node '%s' (parent or suceedingSibling) does not uniquely "
                + "identify a document node. Number of nodes found: '%d'", specifiedNodeXpathString, nodeList.getLength());
        return nodeList.item(0);
    }

    private interface MergeProcessor {
        
        /**
         * Merge the nodes into the document.
         * 
         * @param document
         * @param nodes
         * @return direct parentNode that contains all merged nodes
         */
        Node merge(Document document, NodeList nodes);
    }
    
    /**
     * This merge strategy is based on a ParentNode. The node to be merged will be appended (at the end) 
     * to the existing childNodes.
     */
    private class ParentNodeMergeProcessor implements MergeProcessor {
        
        private Node parentNode;
        
        public ParentNodeMergeProcessor(Node node) {
            this.parentNode = node;
        }
        
        @Override
        public Node merge(Document document, NodeList nodes) {
            return appendChildrenIntoParent(document, parentNode, nodes);
        }
        
        private Node appendChildrenIntoParent(Document inputDocument, final Node parentNode,
                final NodeList childNodeList) {
            for (int i = 0; i < childNodeList.getLength(); i++) {
                Node child = childNodeList.item(i);
                child = inputDocument.importNode(child, true);
                parentNode.appendChild(child);
            }
            return parentNode;
        }

    }
    
    /**
     * This merge strategy is based on a SiblingNode. The node to be merged will be put in front of the  
     * siblingNode.
     */
    private class SuceedingSiblingMergeProcessor implements MergeProcessor {
        
        private Node succeedingSiblingNode;
        
        public SuceedingSiblingMergeProcessor(Node node) {
            this.succeedingSiblingNode = node;
        }
        
        @Override
        public Node merge(Document document, NodeList nodes) {
            return prependSiblingsToNode(document, succeedingSiblingNode, nodes);
        }
        
        private Node prependSiblingsToNode(Document inputDocument, final Node succeedingNode,
                final NodeList siblingsToPrepend) {
            for (int i = 0; i < siblingsToPrepend.getLength(); i++) {
                Node newSibling = siblingsToPrepend.item(i);
                newSibling = inputDocument.importNode(newSibling, true);
                succeedingNode.getParentNode().insertBefore(newSibling, succeedingNode);
            }
            return succeedingNode.getParentNode();
        }

    }
}
