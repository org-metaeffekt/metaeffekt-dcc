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

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.apache.tools.ant.Project;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MergeXmlTaskTest {

    /**
     * This strategy is used for activemq contribution in dcc-activemq-5 package ... therefore
     * the test is using this context.
     * 
     * @throws IOException
     * @throws SAXException 
     * @throws ParserConfigurationException 
     * @throws XPathExpressionException 
     */
    @Test
    public void testMergeParentStrategy() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        File targetDir = new File("target/merge-xml");
        targetDir.mkdirs();
        File outputFile = new File(targetDir, "activemq-out.xml");

        MergeXmlTask mergeXmlTask = new MergeXmlTask();
        mergeXmlTask.setProject(new Project());
        
        File inputDir = new File("src/test/resources/merge-xml-test");
        
        mergeXmlTask.setInputFile(new File(inputDir, "activemq.xml"));
        mergeXmlTask.setFragmentFile(new File(inputDir, "activemq-fragment.xml"));
        mergeXmlTask.setOutputFile(outputFile);
        
        String parentNodeXpathString = "//*[local-name()='policyEntries' and namespace-uri()='http://activemq.apache.org/schema/core']";
        mergeXmlTask.setParentNode(parentNodeXpathString);
        mergeXmlTask.setFragmentNodes("//*[local-name()='policyEntry' and namespace-uri()='http://activemq.apache.org/schema/core']");

        mergeXmlTask.execute();
        
        Validate.isTrue(outputFile.exists());
        Validate.isTrue(FileUtils.readFileToString(outputFile).contains("DLQ"));
    }

    /**
     * This strategy is used for mailet contribution in dcc-james-3 package ... therefore
     * the test is using this context.
     * 
     * @throws IOException
     * @throws SAXException 
     * @throws ParserConfigurationException 
     * @throws XPathExpressionException 
     */
    @Test
    public void testMergeSuceedingSiblingStrategy() throws IOException, ParserConfigurationException, SAXException, 
        XPathExpressionException {
        File targetDir = new File("target/merge-xml");
        targetDir.mkdirs();
        File outputFile = new File(targetDir, "mailetcontainer-out.xml");

        MergeXmlTask mergeXmlTask = new MergeXmlTask();
        mergeXmlTask.setProject(new Project());
        
        File inputDir = new File("src/test/resources/merge-xml-test");
        
        mergeXmlTask.setInputFile(new File(inputDir, "mailetcontainer.xml"));
        mergeXmlTask.setFragmentFile(new File(inputDir, "mailet-fragment.xml"));
        mergeXmlTask.setOutputFile(outputFile);
        
        mergeXmlTask.setSucceedingSiblingNode("//mailet[@class='LocalDelivery']");
        String nodeToIntegrateXpathString = "//mailet[@class='org.metaeffekt.dcc.MailetToBeMerged']";
        mergeXmlTask.setFragmentNodes(nodeToIntegrateXpathString);

        mergeXmlTask.execute();
        
        Validate.isTrue(outputFile.exists());
        Validate.isTrue(FileUtils.readFileToString(outputFile).contains("value42"));

        // ensure that it is merge to the correct position: BEGIN ---->
        Document createdDocument = createDocument(outputFile);
        NodeList nodes = getNodes(createdDocument, nodeToIntegrateXpathString);
        Validate.isTrue(1 == nodes.getLength());
        
        Node mergedNode = nodes.item(0);
        Node expectedParent = getNodes(createdDocument, "//processor[@state='transport']").item(0);
        Validate.isTrue(mergedNode.getParentNode().equals(expectedParent));
        
        Node expectedPredecessorElement = getNodes(createdDocument, "//mailet[@class='ToSenderFolder']").item(0);
        Validate.isTrue(getPreviousElementNode(mergedNode).equals(expectedPredecessorElement));
        
        Node expectedSuccessor = getNodes(createdDocument, "//mailet[@class='LocalDelivery']").item(0);
        Validate.isTrue(getNextSibling(mergedNode).equals(expectedSuccessor));
        // <---- ensure that it is merge to the correct position: END
    }

    private Object getNextSibling(Node node) {
        Node currentNode = node;
        while (currentNode.getNextSibling() != null 
                && currentNode.getNextSibling().getNodeType() != Node.ELEMENT_NODE) {
            currentNode = currentNode.getNextSibling();
        }
        return currentNode.getNextSibling();
    }

    private Node getPreviousElementNode(Node node) {
        Node currentNode = node;
        while (currentNode.getPreviousSibling() != null 
                && currentNode.getPreviousSibling().getNodeType() != Node.ELEMENT_NODE) {
            currentNode = currentNode.getPreviousSibling();
        }
        return currentNode.getPreviousSibling();
    }

    private NodeList getNodes(Document document, String xpathString) throws XPathExpressionException {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath nodesToFindXPath = xPathfactory.newXPath();
        XPathExpression nodesToFindExpression = nodesToFindXPath.compile(xpathString);
        return (NodeList) nodesToFindExpression.evaluate(document, XPathConstants.NODESET);
    }

    private Document createDocument(File inputFile) throws ParserConfigurationException, SAXException,
        IOException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(inputFile);
    }
    
}
