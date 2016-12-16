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
package org.metaeffekt.dcc.shell;

import static org.metaeffekt.dcc.controller.DccControllerConstants.DCC_SHELL_HOME;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.Bootstrap;
import org.springframework.shell.core.ExitShellRequest;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.shell.core.SimpleParser;
import org.springframework.util.StopWatch;

/**
 * Entry class for the DCC Shell. Simply starts Spring Shell's {@link Bootstrap} class.
 * <p>
 * The Bootstrap class starts the Spring context which it finds under
 * {@code /META-INF/spring/spring-shell-plugin.xml}.
 */
public class DccShell {

    private static final String CMDFILE_OPTION = "--cmdfile";

    private static final Logger LOG;

    static {
        // ensure that the system property dcc.shell.home is set (the variable is used in the
        // log4j configuration and needs to be set before LOG4J initializes)
        if (System.getProperty(DCC_SHELL_HOME) == null) {
            URL resource = DccShell.class.getResource("/log4j2.xml");
            if (resource != null) {
                String file = resource.getPath();
                File configDir = new File(file).getParentFile();
                if (configDir != null && configDir.getParentFile() != null) {
                    File dccHomeDir = configDir.getParentFile();
                    System.setProperty(DCC_SHELL_HOME, dccHomeDir.getAbsolutePath());
                }
            }
        }

        // initialize logger and back-end before anything else happens
        LOG = LoggerFactory.getLogger(DccShell.class);

        // if the above did not lead to any result we ensure that the current dir is used
        if (System.getProperty(DCC_SHELL_HOME) == null) {
            System.setProperty(DCC_SHELL_HOME, ".");
        }
    }

    public static void main(String[] args) throws IOException, NoSuchFieldException, SecurityException, IllegalAccessException {
        LOG.debug("[{}]=[{}]", DCC_SHELL_HOME, System.getProperty(DCC_SHELL_HOME));

        final String[] preprocessedArguments = preprocessArguments(args);

        Field f = Bootstrap.class.getDeclaredField("sw");
        f.setAccessible(true);
        StopWatch sw = (StopWatch) f.get(null);

        Date date = new Date();
        LOG.info("Starting shell at {}.", date);

        long startTimestamp = System.currentTimeMillis();
        sw.start();

        ExitShellRequest exitShellRequest = null;

        try {
            Bootstrap bootstrap = new Bootstrap(preprocessedArguments);

            // This method suppresses the unwanted message :read history file failed ...
            manipulateLoggingHandlers();

            exitShellRequest = bootstrap.run();
        } catch (RuntimeException t) {
            LOG.error(t.getMessage(), t);
        }

        if (exitShellRequest == null || (exitShellRequest.getExitCode() != ExitShellRequest.NORMAL_EXIT.getExitCode())) {
            LOG.error("Exited abnormally. Please check the outputs.");
        }

        final long duration = System.currentTimeMillis() - startTimestamp;
        LOG.info("Terminating shell at {} with exit code {}. Execution time: {}.{} seconds.",
                new Date(), exitShellRequest.getExitCode(), duration / 1000, duration % 1000);

        System.exit(exitShellRequest.getExitCode());
    }


    private static void manipulateLoggingHandlers() {
        String[] contentFilteredLoggerNames = {
                JLineShellComponent.class.getName()
        };
        String[] filterAllMessagesLoggerNames = {
                SimpleParser.class.getName()
        };

        for (String name : contentFilteredLoggerNames) {
            java.util.logging.Logger l = java.util.logging.Logger.getLogger(name);
            l.setFilter(new Filter() {
                @Override
                public boolean isLoggable(LogRecord record) {
                    // this is a message content based filter
                    if (record.getMessage().startsWith("read history file failed.")) {
                        return false;
                    }
                    return true;
                }
            });
        }

        for (String name : filterAllMessagesLoggerNames) {
            java.util.logging.Logger l = java.util.logging.Logger.getLogger(name);
            l.setLevel(Level.OFF);
        }

    }

    private static String[] preprocessArguments(String[] args) {
        final List<String> argsList = new ArrayList<String>();

        int size = args.length;
        for (int i = 0; i < size; i++) {
            final String arg = args[i];
            if (CMDFILE_OPTION.equals(arg)) {
                if (i + 1 < size) {
                    final String fileArg = args[i + 1];
                    checkFileExists(fileArg);

                    argsList.add(arg);
                    argsList.add(fileArg);
                    i++;
                } else {
                    // ignore
                }
            } else if (i == 0) {
                checkFileExists(arg);

                argsList.add(CMDFILE_OPTION);
                argsList.add(arg);
            } else {
                // pass-through other arguments
                argsList.add(arg);
            }
        }

        return argsList.toArray(new String[argsList.size()]);
    }

    private static void checkFileExists(String fileLocation) {
        final File file = new File(fileLocation);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalStateException("The file " + file + " doesn't exists.");
        }
    }

}
