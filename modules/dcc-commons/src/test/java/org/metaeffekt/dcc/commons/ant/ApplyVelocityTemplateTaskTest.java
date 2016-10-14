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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Test;

public class ApplyVelocityTemplateTaskTest {

    private static final String TARGET_SOLUTION_FOLDER = "target/velocity-file-task-test";

    private static final String EXECUTION_PROPERTIES_FILENAME =
        "src/test/resources/velocity-file-task-test/server-input.properties";

    private static final String TEMPLATE_FILENAME =
        "src/test/resources/velocity-file-task-test/templates/server.properties.vt";

    private static final String TEST_EXECUTION_PROPERTIES_FILENAME =
        "src/test/resources/velocity-file-task-test/test.properties";

    private static final String TEST_TEMPLATE_FILENAME =
        "src/test/resources/velocity-file-task-test/templates/test.properties.vt";

    private Project project;

    @Before
    public void setUp() throws IOException {
        project = new Project();
    }

    @Test
    public void execute() {
        ApplyVelocityTemplateTask task;
        task = new ApplyVelocityTemplateTask();
        task.setProject(project);
        task.setPropertyFile(new File(EXECUTION_PROPERTIES_FILENAME));
        task.setTemplateFile(new File(TEMPLATE_FILENAME));
        final File targetFile = new File(TARGET_SOLUTION_FOLDER, "test-file.properties");
        task.setTargetFile(targetFile);

        task.execute();

        assertTrue(targetFile.canRead());

        Properties p = PropertyUtils.loadPropertyFile(targetFile);
        assertEquals("cryptodb;sql.enforce_strict_size=true;", p.getProperty("server.database.0"));
        assertEquals("cryptodb", p.getProperty("server.dbname.0"));
        assertEquals("defaultdb;sql.enforce_strict_size=true;", p.getProperty("server.database.1"));
        assertEquals("defaultdb", p.getProperty("server.dbname.1"));
    }

    @Test
    public void execute1() {
        ApplyVelocityTemplateTask task;
        task = new ApplyVelocityTemplateTask();
        task.setProject(project);
        task.setPropertyFile(new File(TEST_EXECUTION_PROPERTIES_FILENAME));
        task.setTemplateFile(new File(TEST_TEMPLATE_FILENAME));
        final File targetFile = new File(TARGET_SOLUTION_FOLDER, "test-output.properties");
        task.setTargetFile(targetFile);

        task.execute();

        assertTrue(targetFile.canRead());

        Properties p = PropertyUtils.loadPropertyFile(targetFile);
        assertEquals("unit1", p.getProperty("contribution.1.0"));
        assertEquals("unit2", p.getProperty("contribution.1.1"));

        assertEquals("unit3", p.getProperty("contribution.2.0"));
        assertEquals("unit4", p.getProperty("contribution.2.1"));
        assertEquals("unit5", p.getProperty("contribution.2.2"));

        assertEquals("req1", p.getProperty("requisition.1.0"));
        assertEquals("req2", p.getProperty("requisition.1.1"));

        assertEquals("req3", p.getProperty("requisition.2.0"));
        assertEquals("req4", p.getProperty("requisition.2.1"));
        assertEquals("req5", p.getProperty("requisition.2.2"));
    }

}
