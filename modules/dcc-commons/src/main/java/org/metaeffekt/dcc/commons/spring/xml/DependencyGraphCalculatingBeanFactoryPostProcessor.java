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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import org.metaeffekt.dcc.commons.dependency.UnitDependencies;
import org.metaeffekt.dcc.commons.dependency.UnitDependencyGraphCalculator;
import org.metaeffekt.dcc.commons.mapping.Profile;

public class DependencyGraphCalculatingBeanFactoryPostProcessor implements
        BeanFactoryPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DependencyGraphCalculatingBeanFactoryPostProcessor.class);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {

        Map<String, BindingFactoryBean> allBindingFactories =
            beanFactory.getBeansOfType(BindingFactoryBean.class);

        UnitDependencies unitDependencies =
            new UnitDependencyGraphCalculator(allBindingFactories.values()).calculate();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Unit dependencies : {}", unitDependencies.toString());
        }

        Profile profile = (Profile) beanFactory.getBean(
            DCCConfigurationBeanDefinitionParser.THE_ONE_TRUE_PROFILE_BEAN_NAME);

        profile.setUnitDependencies(unitDependencies);

        // NOTE: here further static information can be precomputed and added
        //  - eg: precomputed order list for command execution

    }

}
