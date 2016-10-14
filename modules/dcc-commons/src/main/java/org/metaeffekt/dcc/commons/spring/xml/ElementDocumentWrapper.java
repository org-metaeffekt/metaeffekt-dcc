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
package org.metaeffekt.dcc.commons.spring.xml;

import org.apache.commons.lang.NotImplementedException;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;

public class ElementDocumentWrapper implements Document {

    private final Element element;

    public ElementDocumentWrapper(Element element) {
        this.element = element;
    }
    
    @Override
    public Element getDocumentElement() {
        return element;
    }

    @Override
    public Node appendChild(Node arg0) throws DOMException {
        throw new NotImplementedException();
    }

    @Override
    public Node cloneNode(boolean arg0) {
        throw new NotImplementedException();
    }

    @Override
    public short compareDocumentPosition(Node arg0) throws DOMException {
        throw new NotImplementedException();
    }

    @Override
    public NamedNodeMap getAttributes() {
        throw new NotImplementedException();
    }

    @Override
    public String getBaseURI() {
        throw new NotImplementedException();
    }

    @Override
    public NodeList getChildNodes() {
        throw new NotImplementedException();
    }

    @Override
    public Object getFeature(String arg0, String arg1) {
        throw new NotImplementedException();
    }

    @Override
    public Node getFirstChild() {

        throw new NotImplementedException();
    }

    @Override
    public Node getLastChild() {

        throw new NotImplementedException();
    }

    @Override
    public String getLocalName() {

        throw new NotImplementedException();
    }

    @Override
    public String getNamespaceURI() {

        throw new NotImplementedException();
    }

    @Override
    public Node getNextSibling() {

        throw new NotImplementedException();
    }

    @Override
    public String getNodeName() {

        throw new NotImplementedException();
    }

    @Override
    public short getNodeType() {

        return 0;
    }

    @Override
    public String getNodeValue() throws DOMException {

        throw new NotImplementedException();
    }

    @Override
    public Document getOwnerDocument() {

        throw new NotImplementedException();
    }

    @Override
    public Node getParentNode() {

        throw new NotImplementedException();
    }

    @Override
    public String getPrefix() {

        throw new NotImplementedException();
    }

    @Override
    public Node getPreviousSibling() {

        throw new NotImplementedException();
    }

    @Override
    public String getTextContent() throws DOMException {

        throw new NotImplementedException();
    }

    @Override
    public Object getUserData(String arg0) {

        throw new NotImplementedException();
    }

    @Override
    public boolean hasAttributes() {

        return false;
    }

    @Override
    public boolean hasChildNodes() {

        return false;
    }

    @Override
    public Node insertBefore(Node arg0, Node arg1) throws DOMException {

        throw new NotImplementedException();
    }

    @Override
    public boolean isDefaultNamespace(String arg0) {

        return false;
    }

    @Override
    public boolean isEqualNode(Node arg0) {

        return false;
    }

    @Override
    public boolean isSameNode(Node arg0) {

        return false;
    }

    @Override
    public boolean isSupported(String arg0, String arg1) {

        return false;
    }

    @Override
    public String lookupNamespaceURI(String arg0) {

        throw new NotImplementedException();
    }

    @Override
    public String lookupPrefix(String arg0) {

        throw new NotImplementedException();
    }

    @Override
    public void normalize() {

    }

    @Override
    public Node removeChild(Node arg0) throws DOMException {

        throw new NotImplementedException();
    }

    @Override
    public Node replaceChild(Node arg0, Node arg1) throws DOMException {

        throw new NotImplementedException();
    }

    @Override
    public void setNodeValue(String arg0) throws DOMException {

    }

    @Override
    public void setPrefix(String arg0) throws DOMException {

    }

    @Override
    public void setTextContent(String arg0) throws DOMException {

    }

    @Override
    public Object setUserData(String arg0, Object arg1, UserDataHandler arg2) {

        throw new NotImplementedException();
    }

    @Override
    public Node adoptNode(Node arg0) throws DOMException {

        throw new NotImplementedException();
    }

    @Override
    public Attr createAttribute(String arg0) throws DOMException {

        throw new NotImplementedException();
    }

    @Override
    public Attr createAttributeNS(String arg0, String arg1) throws DOMException {

        throw new NotImplementedException();
    }

    @Override
    public CDATASection createCDATASection(String arg0) throws DOMException {

        throw new NotImplementedException();
    }

    @Override
    public Comment createComment(String arg0) {

        throw new NotImplementedException();
    }

    @Override
    public DocumentFragment createDocumentFragment() {

        throw new NotImplementedException();
    }

    @Override
    public Element createElement(String arg0) throws DOMException {

        throw new NotImplementedException();
    }

    @Override
    public Element createElementNS(String arg0, String arg1) throws DOMException {

        throw new NotImplementedException();
    }

    @Override
    public EntityReference createEntityReference(String arg0) throws DOMException {

        throw new NotImplementedException();
    }

    @Override
    public ProcessingInstruction createProcessingInstruction(String arg0, String arg1) throws DOMException {

        throw new NotImplementedException();
    }

    @Override
    public Text createTextNode(String arg0) {

        throw new NotImplementedException();
    }

    @Override
    public DocumentType getDoctype() {

        throw new NotImplementedException();
    }

    @Override
    public String getDocumentURI() {

        throw new NotImplementedException();
    }

    @Override
    public DOMConfiguration getDomConfig() {

        throw new NotImplementedException();
    }

    @Override
    public Element getElementById(String arg0) {

        throw new NotImplementedException();
    }

    @Override
    public NodeList getElementsByTagName(String arg0) {

        throw new NotImplementedException();
    }

    @Override
    public NodeList getElementsByTagNameNS(String arg0, String arg1) {

        throw new NotImplementedException();
    }

    @Override
    public DOMImplementation getImplementation() {

        throw new NotImplementedException();
    }

    @Override
    public String getInputEncoding() {

        throw new NotImplementedException();
    }

    @Override
    public boolean getStrictErrorChecking() {

        return false;
    }

    @Override
    public String getXmlEncoding() {

        throw new NotImplementedException();
    }

    @Override
    public boolean getXmlStandalone() {

        return false;
    }

    @Override
    public String getXmlVersion() {

        throw new NotImplementedException();
    }

    @Override
    public Node importNode(Node arg0, boolean arg1) throws DOMException {

        throw new NotImplementedException();
    }

    @Override
    public void normalizeDocument() {
    }

    @Override
    public Node renameNode(Node arg0, String arg1, String arg2) throws DOMException {
        throw new NotImplementedException();
    }

    @Override
    public void setDocumentURI(String arg0) {

    }

    @Override
    public void setStrictErrorChecking(boolean arg0) {
    }

    @Override
    public void setXmlStandalone(boolean arg0) throws DOMException {
    }

    @Override
    public void setXmlVersion(String arg0) throws DOMException {
    }

}
