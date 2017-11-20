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

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.io.Resource;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.metaeffekt.dcc.commons.ant.PropertyUtils;

/**
 * Conditional beans parser. See dcc-profile-1.0.xsd for details.
 * 
 * @author Karsten Klein
 */
public class IfBeanDefinitionParser implements BeanDefinitionParser {

    private static final String ATTRIBUTE_EXPRESSION = "expression";

    private ExpressionParser parser = new SpelExpressionParser();

    /**
     * {@inheritDoc}
     * 
     * <p>Parses the spring context depending on the evaluation of an expression.</p>
     */
    public BeanDefinition parse(Element element, final ParserContext parserContext) {
        final XmlReaderContext readerContext = parserContext.getReaderContext();
        final Resource resource = readerContext.getResource();
        final XmlBeanDefinitionReader reader = readerContext.getReader();

        String expression = element.getAttribute(ATTRIBUTE_EXPRESSION);

        // FIXME: move constants out of parser to make the parsers independent
        BeanDefinition beanDefinition = parserContext.getRegistry().getBeanDefinition(DCCConfigurationBeanDefinitionParser.THE_ONE_TRUE_PROFILE_BEAN_NAME);
        String deploymentProperties = (String) beanDefinition.getPropertyValues().get(DCCConfigurationBeanDefinitionParser.PROPERTY_DEPLOYMENT_PROPERTIES_PATH);
        String solutionProperties = (String) beanDefinition.getPropertyValues().get(DCCConfigurationBeanDefinitionParser.PROPERTY_SOLUTION_PROPERTIES_PATH);

        // merge system, solution and deployment properties
        Properties properties = new Properties();
        properties.putAll(System.getProperties());
        parseProperties(solutionProperties, properties);
        parseProperties(deploymentProperties, properties);

        // evaluate expression
        final Expression exp = parser.parseExpression(expression);
        final EvaluationContext evaluationContext = new EvaluationContext(parserContext, properties);
        final StandardEvaluationContext context = new StandardEvaluationContext(evaluationContext);

        final Boolean condition = Boolean.parseBoolean(String.valueOf(exp.getValue(context)));
        if (condition != null && condition) {
            // if the expression evaluated true the contained beans elements are parsed
            final List<Element> beansElements = DomUtils.getChildElements(element);
            if (beansElements != null && !beansElements.isEmpty()) {
                for (final Element beansElement : beansElements) {
                    // document wrapper enclosing one beans element
                    final Document documentWrapper = new ElementDocumentWrapper(beansElement);
                    reader.registerBeanDefinitions(documentWrapper, resource);
                }
            }
        }
        return null;
    }

    protected void parseProperties(String propertiesFilePath, Properties p) {
        if (propertiesFilePath != null) {
            File file = new File(propertiesFilePath);
            if (file.exists()) {
                Properties props = PropertyUtils.loadPropertyFile(file);
                p.putAll(props);
            }
        }
    }

}
