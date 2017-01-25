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

public class QueryPidByCommandTaskTest {

    public static final File FILE_WINDOWS = new File("src/test/resources/query-inputs/wmic-samples/wmic.txt");
    public static final String ENCODING_WINDOWS_1252 = "windows-1252";

    public static final File FILE_MACOSX = new File("src/test/resources/query-inputs/ps-samples/macosx.txt");
    public static final String ENCODING_UTF8 = "UTF8";

    @Test
    public void test_wmic() throws IOException {
        QueryPidByCommandTask task = createTask();
        task.setCommand("xyz-4811");
        task.setExecutable("java");

        String input = FileUtils.readFileToString(FILE_WINDOWS, ENCODING_WINDOWS_1252);
        task.setInput(input);
        task.execute();

        final String pid = task.getProject().getProperty(task.getResultProperty());
        Assert.assertEquals("11616", pid);
    }

    @Test
    public void test_wmic_noMatch() throws IOException {
        QueryPidByCommandTask task = createTask();
        task.setCommand("xyz-0815");
        task.setExecutable("java");

        String input = FileUtils.readFileToString(FILE_WINDOWS, ENCODING_WINDOWS_1252);
        task.setInput(input);
        task.execute();

        final String pid = task.getProject().getProperty(task.getResultProperty());
        Assert.assertEquals("-1", pid);
    }

    @Test
    public void test_macosxPs() throws IOException {
        QueryPidByCommandTask task = createTask();
        task.setCommand(".pid");
        task.setExecutable("openvpn");

        String input = FileUtils.readFileToString(FILE_MACOSX, ENCODING_UTF8);
        task.setInput(input);
        task.execute();

        final String pid = task.getProject().getProperty(task.getResultProperty());
        Assert.assertEquals("62", pid);
    }

    @Test
    public void test_macosxPs_noMatch() throws IOException {
        QueryPidByCommandTask task = createTask();
        task.setCommand("pids");
        task.setExecutable("java");

        String input = FileUtils.readFileToString(FILE_MACOSX, ENCODING_UTF8);
        task.setInput(input);
        task.execute();

        final String pid = task.getProject().getProperty(task.getResultProperty());
        Assert.assertEquals("-1", pid);
    }

    private QueryPidByCommandTask createTask() {
        QueryPidByCommandTask task = new QueryPidByCommandTask();
        task.setProject(new LoggingProjectAdapter());
        task.setResultProperty("pid");
        return task;
    }

}
