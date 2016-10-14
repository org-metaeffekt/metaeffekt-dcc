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
package org.metaeffekt.dcc.controller;

import org.junit.Assert;
import org.junit.Test;


public class VersionTest {

    @Test
    public void test() {
        Assert.assertFalse("1.0.0".matches(DccControllerConstants.DCC_SHELL_SUPPORTED_AGENT_VERSION_PATTERN));
        Assert.assertFalse("1.0.1".matches(DccControllerConstants.DCC_SHELL_SUPPORTED_AGENT_VERSION_PATTERN));
        Assert.assertFalse("1.0.2".matches(DccControllerConstants.DCC_SHELL_SUPPORTED_AGENT_VERSION_PATTERN));
        Assert.assertFalse("1.1.0".matches(DccControllerConstants.DCC_SHELL_SUPPORTED_AGENT_VERSION_PATTERN));
        Assert.assertTrue("1.2.0".matches(DccControllerConstants.DCC_SHELL_SUPPORTED_AGENT_VERSION_PATTERN));
        Assert.assertTrue("1.2.4".matches(DccControllerConstants.DCC_SHELL_SUPPORTED_AGENT_VERSION_PATTERN));
        Assert.assertTrue("1.3.0".matches(DccControllerConstants.DCC_SHELL_SUPPORTED_AGENT_VERSION_PATTERN));
        Assert.assertTrue("1.3.1".matches(DccControllerConstants.DCC_SHELL_SUPPORTED_AGENT_VERSION_PATTERN));
        Assert.assertTrue("1.3.2".matches(DccControllerConstants.DCC_SHELL_SUPPORTED_AGENT_VERSION_PATTERN));
        Assert.assertTrue("1.4.0".matches(DccControllerConstants.DCC_SHELL_SUPPORTED_AGENT_VERSION_PATTERN));
        Assert.assertTrue("1.31.0".matches(DccControllerConstants.DCC_SHELL_SUPPORTED_AGENT_VERSION_PATTERN));
        Assert.assertFalse("2.1.0".matches(DccControllerConstants.DCC_SHELL_SUPPORTED_AGENT_VERSION_PATTERN));
    }

}
