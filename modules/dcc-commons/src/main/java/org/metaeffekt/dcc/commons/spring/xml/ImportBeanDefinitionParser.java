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
package org.metaeffekt.dcc.commons.spring.xml;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.ReaderContext;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.w3c.dom.Element;

public class ImportBeanDefinitionParser extends AbstractBeanDefinitionParser {

    private static final Logger LOG = LoggerFactory.getLogger(ImportBeanDefinitionParser.class);
    
    // XML elements
    static final String ELEMENT_IMPORT = "import";

    // XML attributes
    private static final String ATTRIBUTE_VAR = "var";
    private static final String ATTRIBUTE_RESOURCE = "resource";
    private static final String ATTRIBUTE_PARSING_PROPERTIES = "parsingProperties";

    private static final String PARSING_CONTEXT_BEAN = "#ParsingContext";
    public static final String ATTRIBUTE_PROCESSED_IMPORTS = "processedImports";

    @Override
    protected AbstractBeanDefinition parseInternal(Element importElement, ParserContext parserContext) {
        final ReaderContext readerContext = parserContext.getReaderContext();
        final Resource currentResource = readerContext.getResource();
        final XmlBeanDefinitionReader xmlBeanDefinitionReader = parserContext.getReaderContext().getReader();

        final String resource = importElement.getAttribute(ATTRIBUTE_RESOURCE);
        final String var = importElement.getAttribute(ATTRIBUTE_VAR);

        BeanDefinition importParserContext = getParsingContext(parserContext.getRegistry());

        final Properties properties = (Properties) importParserContext.getAttribute(ATTRIBUTE_PARSING_PROPERTIES);
        final Set<String> processedImports = (Set<String>) importParserContext.getAttribute(ATTRIBUTE_PROCESSED_IMPORTS);

        if (!processedImports.contains(resource + var)) {
            final Resource relativeResource;
            try {
                relativeResource = currentResource.createRelative(resource);
            } catch (IOException e) {
                throw new RuntimeException("Cannot parse imported profile: " + resource, e);
            }

            properties.setProperty("var", var);

            LOG.debug("Loading resource [{}] from [{}]. Relative resource is [{}]",
                resource, currentResource, relativeResource);

            xmlBeanDefinitionReader.loadBeanDefinitions(relativeResource);
            processedImports.add(resource);
        } else {
            // skip import, hence it has already been processed
        }
        return null;
    }

    // FIXME: move this out of this class; potentially a UtilityNamespaceHandler
    public static Properties getParsingContextProperties(BeanDefinitionRegistry registry) {
        BeanDefinition parserContextBeanDefinition = getParsingContext(registry);
        return (Properties) parserContextBeanDefinition.getAttribute(ATTRIBUTE_PARSING_PROPERTIES);
    }

    public static BeanDefinition getParsingContext(BeanDefinitionRegistry registry) {
        // the ImportParserContext BeanDefinition is used to store variables while parsing
        BeanDefinition parserContextBeanDefinition;
        if (!registry.containsBeanDefinition(PARSING_CONTEXT_BEAN)) {
            final BeanDefinitionBuilder beanDefBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(ImportParserContext.class);
            parserContextBeanDefinition = beanDefBuilder.getBeanDefinition();
            final Properties properties = new Properties();
            properties.putAll(System.getProperties());
            parserContextBeanDefinition.setAttribute(ATTRIBUTE_PARSING_PROPERTIES, properties);
            parserContextBeanDefinition.setAttribute(ATTRIBUTE_PROCESSED_IMPORTS, new HashSet<String>());
            registry.registerBeanDefinition(PARSING_CONTEXT_BEAN, parserContextBeanDefinition);
        } else {
            parserContextBeanDefinition = registry.getBeanDefinition(PARSING_CONTEXT_BEAN);
        }
        return parserContextBeanDefinition;
    }

}
