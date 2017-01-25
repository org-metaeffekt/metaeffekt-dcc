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
package org.metaeffekt.dcc.commons.ant;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.metaeffekt.core.common.kernel.ant.log.LoggingProjectAdapter;

import java.io.File;
import java.io.IOException;

public class QueryPidByPortTaskTest {

    public static final File FILE_WINDOWS = new File("src/test/resources/query-inputs/netstat-samples/windows-netstat_de.txt");
    public static final String ENCODING_WINDOWS_1252 = "windows-1252";

    @Test
    public void test_default() throws IOException {
        QueryPidByPortTask task = createTask();
        task.setPort("50857");

        String input = FileUtils.readFileToString(FILE_WINDOWS, ENCODING_WINDOWS_1252);
        task.setInput(input);
        task.execute();

        final String pid = task.getProject().getProperty(task.getResultProperty());
        Assert.assertEquals("4548", pid);
    }

    @Test
    public void test_exactPortMatch() throws IOException {
        QueryPidByPortTask task = createTask();
        task.setPort("508");

        String input = FileUtils.readFileToString(FILE_WINDOWS, ENCODING_WINDOWS_1252);
        task.setInput(input);
        task.execute();

        final String pid = task.getProject().getProperty(task.getResultProperty());
        Assert.assertEquals("-1", pid);
    }

    @Test
    public void test_noMatch() throws IOException {
        QueryPidByPortTask task = createTask();
        task.setPort("518");

        String input = FileUtils.readFileToString(FILE_WINDOWS, ENCODING_WINDOWS_1252);
        task.setInput(input);
        task.execute();

        final String pid = task.getProject().getProperty(task.getResultProperty());
        Assert.assertEquals("-1", pid);
    }

    @Test
    public void test_onlyIpv6() throws IOException {
        QueryPidByPortTask task = createTask();
        task.setPort("52857");

        String input = FileUtils.readFileToString(FILE_WINDOWS, ENCODING_WINDOWS_1252);
        task.setInput(input);

        task.execute();

        final String pid = task.getProject().getProperty(task.getResultProperty());
        Assert.assertEquals("4549", pid);
    }

    private QueryPidByPortTask createTask() {
        QueryPidByPortTask task = new QueryPidByPortTask();
        task.setIp("0.0.0.0");
        task.setProtocol("TCP");
        task.setProject(new LoggingProjectAdapter());
        task.setResultProperty("pid");
        return task;
    }

}
