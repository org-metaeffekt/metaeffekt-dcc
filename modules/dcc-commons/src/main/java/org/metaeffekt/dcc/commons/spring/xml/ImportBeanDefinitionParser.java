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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.parsing.ReaderContext;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.w3c.dom.Element;

public class ImportBeanDefinitionParser extends AbstractBeanDefinitionParser {

    private static final Logger LOG = LoggerFactory.getLogger(ImportBeanDefinitionParser.class);
    
    // XML elements
    static final String ELEMENT_IMPORT = "import";

    static final Set<String> processedImports = new HashSet<>();
    
    public static final void resetProcessedImports() {
        processedImports.clear();
    }

    @Override
    protected AbstractBeanDefinition parseInternal(Element importElement, ParserContext parserContext) {
        ReaderContext readerContext = parserContext.getReaderContext();
        Resource currentResource = readerContext.getResource();
        XmlBeanDefinitionReader xmlBeanDefinitionReader = parserContext.getReaderContext().getReader();
        String resource = importElement.getAttribute("resource");
        if (!processedImports.contains(resource)) {
            Resource relativeResource;
            try {
                relativeResource = currentResource.createRelative(resource);
            } catch (IOException e) {
                throw new RuntimeException("Cannot parse imported profile: " + resource, e);
            }
            
            LOG.debug("Loading resource [{}] from [{}]. Relative resource is [{}]", 
                resource, currentResource, relativeResource);
                
            xmlBeanDefinitionReader.loadBeanDefinitions(relativeResource);
            processedImports.add(resource);
        } else {
            // skip import, hence it has already been processed
        }
        return null;
    }

}
