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
package org.metaeffekt.dcc.commons.ant;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.metaeffekt.dcc.commons.http.script.Command;
import org.metaeffekt.dcc.commons.http.script.Credentials;
import org.metaeffekt.dcc.commons.http.script.Script;


public class HttpScriptExecutionTask extends Task {

    private File httpScript;

    /**
     * Executes the task.
     * 
     * @see org.apache.tools.ant.Task#execute()
     */
    @Override
    public void execute() {
        try {
            JAXBContext jc = JAXBContext.newInstance(Script.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            Object element = unmarshaller.unmarshal(httpScript);
            Script script = (Script) element;

            log("Executing [" + script.getCommand().size() + "] requests.");
            // construct individual tasks:
            for (Command command : script.getCommand()) {
                HttpRequestTask httpRequestTask = new HttpRequestTask();
                httpRequestTask.setProject(getProject());
                httpRequestTask.setTaskName(getTaskName());
    
                httpRequestTask.setUri(command.getUri());
                Command.Body body = command.getBody();
                httpRequestTask.setBody(body == null ? null : body.getValue());
                httpRequestTask.setHttpMethod(Enum.valueOf(HttpMethod.class, command.getMethod()));
                
                httpRequestTask.setServerHostName(script.getServer().getHost());
                httpRequestTask.setServerPort(script.getServer().getPort().intValue());
                httpRequestTask.setServerScheme(script.getServer().getScheme());
                
                httpRequestTask.setFailOnError(Boolean.parseBoolean(command.getFailOnError()));

                if (body != null) {
                    httpRequestTask.setContentType(body.getContentType());
                }

                Credentials credentials = command.getCredentials();
                if (credentials == null) {
                    credentials = script.getServer().getCredentials();
                }
                if (credentials != null) {
                    httpRequestTask.setUsername(credentials.getUsername());
                    httpRequestTask.setPassword(credentials.getPassword());
                }
                httpRequestTask.execute();
            }
            log("Finished executing [" + script.getCommand().size() + "] requests.");
        } catch (JAXBException e) {
            throw new BuildException(e);
        }
    }

    public File getHttpScript() {
        return httpScript;
    }

    public void setHttpScript(File httpScript) {
        this.httpScript = httpScript;
    }

}
