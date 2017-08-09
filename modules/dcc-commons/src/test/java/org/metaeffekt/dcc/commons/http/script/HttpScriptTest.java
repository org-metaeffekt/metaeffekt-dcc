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
package org.metaeffekt.dcc.commons.http.script;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.tools.ant.Project;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.metaeffekt.dcc.commons.ant.HttpScriptExecutionTask;

public class HttpScriptTest {

    private File testScript = new File("src/test/resources/http/script/test-script.xml");

    @Test
    public void testParsing() throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(Script.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        Object element = unmarshaller.unmarshal(testScript);
        Script script = (Script) element;
        Assert.assertEquals("localhost", script.getServer().getHost());
    }
    
    @Ignore("Currently requires a running elasticsearch instance on localhost:9200")
    @Test
    public void testExec() throws JAXBException {
        HttpScriptExecutionTask task = new HttpScriptExecutionTask();
        task.setProject(new Project());
        
        task.setHttpScript(testScript);
        
        task.execute();
    }
    
}

