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
package org.metaeffekt.dcc.service.wrapper;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class ApplicationProxyTest {

    private static String RESULT_STRING;
    
    static {
        System.setProperty(ApplicationProxy.MAIN_CLASS, MiniMe.class.getName());
    }
    
    @Before
    public void resetResultString() {
        RESULT_STRING = "UNDEFINED";
    }
    
    @Test
    public void testReplacements() throws Exception {
        System.setProperty(ApplicationProxy.MAIN_PARAMETERS, "agent $* -f config.d $1 $2 $1");
        ApplicationProxy.main(new String[] {"argument1", "argument2"} );
        Assert.assertEquals("[agent, argument1, argument2, -f, config.d, argument1, argument2, argument1]", RESULT_STRING);
    }
    
    @Test
    public void testNoArgs() throws Exception {
        System.setProperty(ApplicationProxy.MAIN_PARAMETERS, "agent $* -f config.d $1 $2 $1");
        ApplicationProxy.main(null);
        Assert.assertEquals("[agent, -f, config.d, $1, $2, $1]", RESULT_STRING);
    }

    @Test
    public void testEmptyArgs() throws Exception {
        System.setProperty(ApplicationProxy.MAIN_PARAMETERS, "agent $* -f config.d $1 $2 $1");
        ApplicationProxy.main(new String[0]);
        Assert.assertEquals("[agent, -f, config.d, $1, $2, $1]", RESULT_STRING);
    }
    
    @Test
    public void test_NoMainParams() throws Exception {
        System.getProperties().remove(ApplicationProxy.MAIN_PARAMETERS);
        ApplicationProxy.main(new String[] {"argument1", "argument2"} );
        Assert.assertEquals("[argument1, argument2]", RESULT_STRING);
    }
    
    @Test
    public void testNoArgs_NoMainParams() throws Exception {
        System.getProperties().remove(ApplicationProxy.MAIN_PARAMETERS);
        ApplicationProxy.main(null);
        Assert.assertEquals(null, RESULT_STRING);
    }

    @Test
    public void testEmptyArgs_NoMainParams() throws Exception {
        System.getProperties().remove(ApplicationProxy.MAIN_PARAMETERS);
        ApplicationProxy.main(new String[0]);
        Assert.assertEquals("[]", RESULT_STRING);
    }
    
    public static class MiniMe {
        public static void main(String[] args) {
            if (args != null) {
                ApplicationProxyTest.RESULT_STRING = String.valueOf(Arrays.asList(args));
            } else {
                ApplicationProxyTest.RESULT_STRING = null;
            }
        }
    };
    
    @Test
    public void testThreadIntrospection() {
        String[] expectedThreadNamePatterns = new String[] { "main" };
        
        boolean allPatternsMatched =
            ApplicationProxy.checkAllThreadNamePatternMatched(expectedThreadNamePatterns);
        
        Assert.assertTrue(allPatternsMatched);
        
    }

}
