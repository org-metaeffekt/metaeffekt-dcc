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
package org.metaeffekt.dcc.agent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.metaeffekt.dcc.commons.domain.Id;

public class DccAgent {

    private static final String TRUE = "true";

    public static final String DCC_AGENT_HOME = "dcc.agent.home";

    private static final Logger LOG;

    static {
        // ensure that the system property dcc.agent.home is set (the variable is used in the
        // log4j configuration and needs to be set before LOG4J initializes)
        if (System.getProperty("dcc.agent.home") == null) {
            URL resource = DccAgent.class.getResource("/dcc-agent.properties");
            if (resource != null) {
                String file = resource.getPath();
                File configDir = new File(file).getParentFile();
                if (configDir != null && configDir.getParentFile() != null) {
                    File dccHomeDir = configDir.getParentFile();
                    System.setProperty("dcc.agent.home", dccHomeDir.getAbsolutePath());
                }
            }
        }

        // if the above did not lead to any result we ensure that the current dir is used
        if (System.getProperty(DCC_AGENT_HOME) == null) {
            System.setProperty(DCC_AGENT_HOME, ".");
        }

        // initialize logger and back-end before anything else happens
        LOG = LoggerFactory.getLogger(DccAgent.class);

        // disable jul (restlet uses it, bridging didn't work)
        java.util.logging.LogManager.getLogManager().reset();
        java.util.logging.Logger.getGlobal().setLevel(Level.OFF);

        // dump system properties to info level
        LOG.info("System properties on startup:");
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            LOG.info("[{}] = [{}]", entry.getKey(), entry.getValue());
        }
    }

    private volatile ClassPathXmlApplicationContext ctx;

    private int port = -1;

    private boolean started = false;

    public static void main(String[] args) throws Exception {
        DccAgent agent = new DccAgent();
        agent.start();
    }

    public DccAgent() {
    }

    /**
     * Constructor which enables to overwrite the port.
     * 
     * @param port The port to use when starting the agents HTTPS endpoint.
     */
    public DccAgent(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        LOG.debug("Starting DCC Agent application context...");
        ctx = new ClassPathXmlApplicationContext("classpath:/META-INF/dcc-agent-context.xml");

        final PropertySource propertySource = ctx.getBean("propertySource", PropertySource.class);
        
        if (this.port == -1) {
            this.port = Integer.parseInt(propertySource.getProperty(
                    "dcc.agent.port", String.valueOf(DccAgentEndpoint.DEFAULT_PORT)));
            
        }

        LOG.info("Verifying DCC Agent environment...");
        String workingDir = propertySource.getProperty("dcc.agent.working.dir");
        String destinationDir = propertySource.getProperty("dcc.agent.destination.dir");
        checkDirectoryIsWritable(workingDir);
        checkDirectoryIsWritable(destinationDir);

        LOG.info("Starting DCC Agent on port [{}] ...", port);

        final Component restletComponent = ctx.getBean("restletComponent", Component.class);

        @SuppressWarnings("unchecked")
        List<Parameter> sslConfiguration = ctx.getBean("sslConfiguration", List.class);
        Server httpServer = new Server(Protocol.valueOf(DccAgentEndpoint.DEFAULT_PROTOCOL), port);
        restletComponent.getServers().getContext().getParameters()
                .add("persistingConnections", Boolean.FALSE.toString());

        restletComponent.getServers().add(httpServer);

        httpServer.getContext().getParameters().addAll(sslConfiguration);

        httpServer.start();
        restletComponent.start();

        ctx.addApplicationListener(new ApplicationListener<ContextClosedEvent>() {

            @Override
            public void onApplicationEvent(ContextClosedEvent cce) {
                try {
                    DccAgent.this.stop();
                } catch (Exception e) {
                    LOG.error("Error stopping agent.", e);
                }
            }
        });

        ctx.registerShutdownHook();
        
        if (TRUE.equalsIgnoreCase(propertySource.
                getProperty("dcc.agent.watchdog.enabled", TRUE))) {
            registerProcessWatchdog(ctx, propertySource);
        }

        started = true;
        LOG.info("DCC Agent started");
    }

    private void checkDirectoryIsWritable(String path) {
        File dir = new File(path);
        // attempt creation if folder does not exist yet
        if (!dir.exists()) {
            try {
                dir.mkdirs();
            } catch (RuntimeException e) {
                // compensated subsequently 
            }
        }
        if (!dir.exists() || !dir.canWrite()) {
            throw new IllegalStateException(String.format("DCC Agent cannot write to folder [%s]. "
                + "Please make sure the folder exists and is writable for user [%s].", path, 
                System.getProperty("user.name")));
        }

    }

    private void registerProcessWatchdog(BeanFactory beanFactory, PropertySource propertySource) {
        final AgentScriptExecutor scriptExecutor = ctx.getBean("agentScriptExecutor", AgentScriptExecutor.class);
        final long waitPeriodInSeconds = Integer.parseInt(propertySource.getProperty("dcc.agent.watchdog.interval", "60"));
        
        Runnable watchdog = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    LOG.debug("Process watchdog scanning environment...");

                    // scan the config directories of all deployments
                    DirectoryScanner scanner = new DirectoryScanner();
                    
                    scanner.setBasedir(scriptExecutor.getDestinationBaseDir());
                    scanner.setFollowSymlinks(false);
                    scanner.setIncludes(new String[] {"*/config/*/start.properties"});
                    scanner.scan();
                    
                    String[] foundFiles = scanner.getIncludedFiles();
                    
                    // order files by timestamp
                    Map<Long, File> timestampFileMap = new TreeMap<>();
                    for (String startProperties : foundFiles) {
                        final File file = new File(scriptExecutor.getDestinationBaseDir(), startProperties);
                        if (file.exists()) {
                            long timestamp = file.lastModified();
                            timestampFileMap.put(timestamp, file);
                        }
                    }
                    
                    boolean success = true;
                    for (Long key : timestampFileMap.keySet()) {
                        final File file = timestampFileMap.get(key); 
                        final String commandString = "start";
                        if (file.exists()) {
                            try {
                                String content = FileUtils.readFileToString(file);
                                try {
                                    String unitLocation = content.substring(content.indexOf("(") + 1);
                                    unitLocation = unitLocation.substring(0, unitLocation.indexOf(")"));
    
                                    String[] unitLocationSplit = unitLocation.split(":");
    
                                    String deploymentId = unitLocationSplit[0];
                                    String unitId = unitLocationSplit[1];
                                    String packageId = unitLocationSplit[2];
    
                                    try {
                                        scriptExecutor.executeScript(
                                            Id.createDeploymentId(deploymentId), 
                                            Id.createPackageId(packageId), 
                                            Id.createUnitId(unitId), 
                                            commandString, file, null);
                                    } catch (BuildException e) {
                                        LOG.error(String.format("Failed to execute command %s on unit %s", commandString, unitId), e.getMessage());
                                        success = false;
                                    }
                                } catch (Exception e1) {
                                    LOG.error("Unable to parse meta data from properties file.", file.getAbsolutePath(), e1.getMessage());
                                    success = false;
                                }
    
                            } catch (IOException e1) {
                                LOG.error("Unable to parse file: {}: {}.", file.getAbsolutePath(), e1.getMessage());
                                success = false;
                            }
                        }
                    }
                    long sleepDuration = 1000 * 10;
                    // when all scripts terminated successfully we stretch the time until we iterate
                    // the next time. In case one had an error we continue until everything is as desired.
                    if (success) {
                        LOG.debug("Process watchdog sleeping for 1 minute...");
                        sleepDuration = 1000 * waitPeriodInSeconds;
                    }
                    try {
                        Thread.sleep(sleepDuration);
                    } catch (InterruptedException e) {
                    }
                }
                
            }
        };
        Thread thread = new Thread(watchdog);
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() throws Exception {
        if (started) {
            LOG.info("Stopping DCC Agent...");
            started = false;
            if (ctx != null) {
                Component restletComponent = (Component) ctx.getBean("restletComponent");
                restletComponent.stop();
                LOG.debug("Closing Spring application context");
                ctx.close();
                ctx = null;
            }
            LOG.info("DCC Agent stopped");
        }
    }

    public boolean isStarted() {
        return started;
    }

}
