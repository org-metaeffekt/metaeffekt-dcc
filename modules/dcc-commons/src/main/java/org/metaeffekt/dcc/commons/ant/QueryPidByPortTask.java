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

import org.apache.commons.lang3.Validate;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Queries the pid of process with the given protocol, ip and port. The task requires inputs from netstat. MacOSX is
 * currently not supported, but can be integrated using lsof (lsof -i:_port_).
 */
public class QueryPidByPortTask extends Task {

    /**
     * Line-based input.
     */
    private String input;

    /**
     * The protocol.
     */
    private String protocol;

    /**
     * The ip of the networkadapter to use for the query. If this specific IP is not found the task searches implicitly
     * for alternative network addresses (0.0.0.0, 127.0.0.1, ::).
     */
    private String ip;

    /**
     * The port to query for.
     */
    private String port;

    /**
     * The result property to set the found pid. In case no pid was found this property will be set to -1;
     */
    private String resultProperty;

    @Override
    public void execute() throws BuildException {
        Validate.notEmpty(input, "Attribute input must be set!");
        Validate.notEmpty(protocol, "Attribute protocol must be set!");
        Validate.notEmpty(ip, "Attribute ip must be set!");
        Validate.notEmpty(port, "Attribute port must be set!");
        Validate.notEmpty(resultProperty, "Attribute resultProperty must be set!");

        final Pattern portMatcher = Pattern.compile("\\:([0-9]+)_");
        final Pattern pidPattern = Pattern.compile(".*_([0-9]+)_");

        final List<String> lines = new ArrayList<>();

        // iterate lines and perform prefiltering by port
        try (Scanner scanner = new Scanner(input)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                // to get rid of boundary issues, we prefix and suffix the line with underscores
                line = "_" + line.trim().toLowerCase() + "_";
                // ... and we replace all whitepaces with underscores.
                line = line.replaceAll("\\s+", "_");

                // now we can use simple exact matching to identify the line(s) of interest
                if (!line.startsWith("_" + protocol.toLowerCase())) {
                    continue;
                }

                // the line contains from and to address; we must skip the line, when out port is used in the
                // two address. This also makes us completely independent of the connection status (which is
                // internationalized and even contains umlauts in german.
                final Matcher matcher = portMatcher.matcher(line);
                if (matcher.find()) {
                    final String firstPort = matcher.group(1);
                    if (!port.equals(firstPort)) {
                        continue;
                    }
                }

                if (line.contains("_" + ip + ":" + port + "_")) {
                    lines.add(line);
                    continue;
                }
                if (line.contains("_" + "127.0.0.1" + ":" + port + "_")) {
                    lines.add(line);
                    continue;
                }
                if (line.contains("_" + "0.0.0.0" + ":" + port + "_")) {
                    lines.add(line);
                    continue;
                }
                if (line.contains("_" + ":::" + port + "_")) {
                    lines.add(line);
                    continue;
                }
            }
        }

        for (String line : lines) {
            Matcher matcher = pidPattern.matcher(line);
            final boolean foundMatch = matcher.find();
            if (foundMatch) {
                final String pid = matcher.group(1);

                // we nevertheless get a null pid, we skip the line here
                if (!"0".equals(pid)) {
                    PropertyUtils.setProperty(resultProperty, pid, PropertyUtils.PROPERTY_PROJECT_LEVEL, getProject());
                    return;
                }
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getResultProperty() {
        return resultProperty;
    }

    public void setResultProperty(String resultProperty) {
        this.resultProperty = resultProperty;
    }
}