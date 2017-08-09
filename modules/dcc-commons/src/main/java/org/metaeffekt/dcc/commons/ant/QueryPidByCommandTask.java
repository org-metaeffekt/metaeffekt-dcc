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

import org.apache.commons.lang3.Validate;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses out the pid for a given query. As input the task requires the result of a wmic command (windows) or the
 * result of ps -axww -o pid=,command= on linux systems.
 */
public class QueryPidByCommandTask extends Task {

    /**
     * The input is required to be organized in lines per process. The procedure checks whether a line contains both
     * executable (all checks are lower case) and the command for exact matches. The process id must be the first
     * numeric string in the line.
     */
    private String input;

    /**
     * The executable to check for.
     */
    private String executable;

    /**
     * The command to check for.
     */
    private String command;

    /**
     * The result property to use.
     */
    private String resultProperty;

    @Override
    public void execute() throws BuildException {
        Validate.notEmpty(input, "Attribute input must be set!");
        Validate.notEmpty(executable, "Attribute executable must be set!");
        Validate.notEmpty(command, "Attribute command must be set!");
        Validate.notEmpty(resultProperty, "Attribute resultProperty must be set!");

        final List<String> lines = new ArrayList<>();
        String header = null;

        // iterate lines and perform prefiltering by port
        try (Scanner scanner = new Scanner(input)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                // to get rid of boundary issues, we normalize the line
                line = "_" + line.trim().toLowerCase() + "_";
                line = line.replaceAll("\\s+", "_");

                if (!line.contains(executable.toLowerCase())) {
                    continue;
                }
                if (line.contains(command.toLowerCase())) {
                    lines.add(line);
                    continue;
                }
            }
        }

        if (lines.size() > 1) {
            throw new BuildException("Multiple matches of command=" + command + " and executable=" + executable + " detected!");
        }

        for (String line : lines) {
            Pattern pidPattern = Pattern.compile("_([0-9]+)_");
            Matcher matcher = pidPattern.matcher(line);
            final boolean foundMatch = matcher.find();
            if (foundMatch) {
                final String pid = matcher.group(1);
                PropertyUtils.setProperty(resultProperty, pid, PropertyUtils.PROPERTY_PROJECT_LEVEL, getProject());
                return;
            }
        }

        PropertyUtils.setProperty(resultProperty, "-1", PropertyUtils.PROPERTY_PROJECT_LEVEL, getProject());
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getResultProperty() {
        return resultProperty;
    }

    public void setResultProperty(String resultProperty) {
        this.resultProperty = resultProperty;
    }

    public String getExecutable() {
        return executable;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}