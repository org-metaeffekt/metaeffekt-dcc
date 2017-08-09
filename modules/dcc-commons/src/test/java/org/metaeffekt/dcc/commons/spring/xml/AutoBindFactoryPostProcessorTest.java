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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.metaeffekt.dcc.commons.mapping.Binding;

/**
 * Created by i001450 on 11.08.14.
 */
public class AutoBindFactoryPostProcessorTest {

    private static final String PROFILE_BASE_DIR = "classpath:autoBind-test";

    @Test
    public void parseAutoBindDeploymentProfile() throws Exception {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                PROFILE_BASE_DIR + File.separator + "autoBind-deployment.xml")) {

            Map<String, Binding> allBindings = context.getBeansOfType(Binding.class);
            assertEquals(4, allBindings.values().size());

            for (String beanName : allBindings.keySet()) {

                Binding binding = allBindings.get(beanName);

                if (beanName.startsWith("autoBound")) {
                    assertTrue("Binding was not auto bound", binding.isAutoBound());
                }
                else {
                    assertFalse("Binding was auto bound", binding.isAutoBound());
                }
            }
        }

    }

    @Test
    public void parseAutoBindDeploymentProfileWithOneExplicitBinding() throws Exception {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                PROFILE_BASE_DIR + File.separator + "autoBind-deployment-one-explicit.xml")) {

            Map<String, Binding> allBindings = context.getBeansOfType(Binding.class);
            assertEquals(2, allBindings.values().size());

            for (String beanName : allBindings.keySet()) {

                Binding binding = allBindings.get(beanName);

                if (beanName.startsWith("autoBound")) {
                    assertTrue("Binding was not auto bound", binding.isAutoBound());
                }
                else {
                    assertFalse("Binding was auto bound", binding.isAutoBound());
                }
            }
        }

    }

    @Test
    public void parseAutoBindDeploymentProfileWithMultipleRequiredCapabilitiesWithOneExplicitBinding() throws Exception {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                PROFILE_BASE_DIR + File.separator + "autoBind-deployment-multiple-required-one-explicit.xml")) {

            Map<String, Binding> allBindings = context.getBeansOfType(Binding.class);
            assertEquals(4, allBindings.values().size());

            for (String beanName : allBindings.keySet()) {

                Binding binding = allBindings.get(beanName);

                if (beanName.startsWith("autoBound")) {
                    assertTrue("Binding was not auto bound", binding.isAutoBound());
                }
                else {
                    assertFalse("Binding was auto bound", binding.isAutoBound());
                }
            }
        }

    }

    @Test
    public void parseAutoBindDeploymentProfileWithMultipleProvidedCapabilities() throws Exception {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                PROFILE_BASE_DIR + File.separator + "autoBind-deployment-multiple-provided.xml")) {

            Map<String, Binding> allBindings = context.getBeansOfType(Binding.class);
            assertEquals(4, allBindings.values().size());

            for (String beanName : allBindings.keySet()) {

                Binding binding = allBindings.get(beanName);

                if (beanName.startsWith("autoBound")) {
                    assertTrue("Binding was not auto bound", binding.isAutoBound());
                }
                else {
                    assertFalse("Binding was auto bound", binding.isAutoBound());
                }
            }
        }

    }

    @Test
    public void parseAutoBindAmbiguousDeploymentProfile() throws Exception {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                PROFILE_BASE_DIR + File.separator + "autoBind-deployment-ambiguous.xml")) {
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            assertTrue("Unexpected error message: \"" + e.getMessage() + "\"",
                    e.getMessage().startsWith("Auto-bind found an ambiguity: multiple provided capabilities of type 'capDef1' found in 2 different units"));
        }

    }

    @Test
    public void parseNoAutoBindDeploymentProfile() throws Exception {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                PROFILE_BASE_DIR + File.separator + "noAutoBind-deployment.xml")) {
            fail("Validation errors expected");
        } catch (BeanDefinitionValidationException e) {
            assertEquals("Stopping profile evaluation due to validation errors.", e.getMessage());
        }

    }

    @Test
    public void parseAutoBindSolutionProfile() throws Exception {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                PROFILE_BASE_DIR + File.separator + "autoBind-solution.xml")) {

            Map<String, Binding> allBindings = context.getBeansOfType(Binding.class);
            assertEquals(1, allBindings.values().size());
        }

    }

    @Test
    public void parseNoAutoBindSolutionProfile() throws Exception {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                PROFILE_BASE_DIR + File.separator + "noAutoBind-solution.xml")) {

            Map<String, Binding> allBindings = context.getBeansOfType(Binding.class);
            assertEquals(1, allBindings.values().size());
        }

    }

    @Test
    public void parseNoAutoBindBaseProfile() throws Exception {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                PROFILE_BASE_DIR + File.separator + "noAutoBind-base.xml")) {

            Map<String, Binding> allBindings = context.getBeansOfType(Binding.class);
            assertEquals(1, allBindings.values().size());
        }
    }

    @Test
    public void parseAutoBindBaseProfile() throws Exception {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                PROFILE_BASE_DIR + File.separator + "autoBind-base.xml")) {

            Map<String, Binding> allBindings = context.getBeansOfType(Binding.class);
            assertEquals(1, allBindings.values().size());
        }
    }
}
