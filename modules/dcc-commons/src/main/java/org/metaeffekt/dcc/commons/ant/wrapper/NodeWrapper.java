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
package org.metaeffekt.dcc.commons.ant.wrapper;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Wrappes a {@link org.w3c.dom.Node} for convenient access.
 */
public class NodeWrapper {

    private Node node;

    public NodeWrapper(Node node) {
        this.node = node;
    }

    public String getAttribute(String attribute) {
        return getAttribute(attribute, null);
    }

    public String getAttribute(String attribute, String defaultValue) {
        return getAttribute(attribute, node.getAttributes(), defaultValue);
    }

    private String getAttribute(String attribute, NamedNodeMap attributes, String defaultValue) {
        if (attributes != null) {
            final Node namedItem = attributes.getNamedItem(attribute);
            return getAttribute(namedItem, defaultValue);
        }
        return defaultValue;
    }

    private String getAttribute(Node namedItem, String defaultValue) {
        if (namedItem != null) {
            String value = namedItem.getNodeValue();
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return defaultValue;
    }

}
