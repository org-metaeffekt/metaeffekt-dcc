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
package org.metaeffekt.dcc.service.wrapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Proxy class to pass on and tweak the main method invocation. The class reads the system 
 * property <code>dcc.service.wrapper.application.class</code> that is required to provide
 * the full qualified class name of the application to start using the main method.
 * </p>
 * <p>
 * A custom parameter set for invoking the main methods can be provided by a second system property
 * <code>dcc.service.wrapper.application.parameters</code>. If this property is not set the
 * original parameters are preserved. If the property is provided the proxied application will
 * be started using the parameters listed (separator is blank). For convenience the original
 * arguments can be passed through using a $* placeholder. Individual parameters can be passed
 * throw using $1, $2. 
 * </p>
 * <p>
 * The class installs a thread to monitor the lifecycle of the application. It makes heavy use of
 * the anchor file as reliable indicator that the process is supposed to terminate. 
 * </p>
 * <p>
 * The class supports a 'process started indicator' file. Which is only produces in case all
 * required threads are available. This is useful to determine whether the system has fully
 * started, in case no external service to query is available.
 * </p>
 * 
 * @author Karsten Klein
 */
public class ApplicationProxy {

    public static final String ANCHOR_FILE = "dcc.service.wrapper.application.anchor.file";
    public static final String MAIN_PARAMETERS = "dcc.service.wrapper.application.parameters";
    public static final String MAIN_CLASS = "dcc.service.wrapper.application.class";
    public static final String STOP_METHOD = "dcc.service.wrapper.application.stop.method";
    
    public static final String PROCESS_STARTED_INDICATOR_FILE = "dcc.service.wrapper.application.process.started.file";
    public static final String PROCESS_THREAD_PATTERNS = "dcc.service.wrapper.application.thread.patterns";
    
    private static final String PLACEHOLDER_FULL_LIST = "$*";
    private static final String PLACEHOLDER_PREFIX = "$";

    
    /**
     * @deprecated Use PROCESS_STARTED_INDICATOR_FILE instead. Will be removed in future versions.
     * @since 1.3.1
     */
    @Deprecated
    public static final String PROCESS_STARTED_INDICATOR_FILE__DEPRECATED = "dcc.service.wrapper.application.pid.file";
    
    public static void main(final String[] args) throws Exception {
        // resolve the strategy 
        final String className = System.getProperty(MAIN_CLASS);
        assertNotNull(className, "'" + MAIN_CLASS + "' must be defined as system property.");
        final Class<?> clazz = Class.forName(className, true, ApplicationProxy.class.getClassLoader());

        final Method mainMethod = clazz.getMethod("main", String[].class);
        assertNotNull(className, "Method main(String[]) must be defined in class '" + clazz + "'.");

        final String anchorFileName = System.getProperty(ANCHOR_FILE);
        final File anchorFile = anchorFileName != null ? new File(anchorFileName) : null;

        startLifecycleMonitorThread(clazz, args, anchorFile);
        
        Object[] objectArray = composeArguments(args);
        mainMethod.invoke(null, objectArray);
    }

    private static void startLifecycleMonitorThread(final Class<?> clazz, final String[] args, final File anchorFile) {
        final boolean activeStop = anchorFile != null && anchorFile.exists();
        final Thread monitor = new Thread() {
            @Override
            public void run() {
                super.run();
                
                log("(lifecycle monitor) polling prerequisites.");
                while (true) {
                    try {
                        sleep(2000);
                        
                        if (ApplicationProxy.checkPrerequisites()) {
                            ApplicationProxy.createProcessStartedIndicatorFile();
                            break;
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                if (activeStop) {
                    log("(lifecycle monitor) polling anchor file.");
                    while (true) {
                        try {
                            sleep(2000);
                            if (!anchorFile.exists()) {
                                break;
                            }
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                
                    // in any case invoke doStop to complete lifecycle
                    log("(lifecycle monitor) stopping.");
                    doStop(clazz, args);
                }
            }
        };
        monitor.setDaemon(true);
        monitor.start();
    }

    private static Object[] composeArguments(String[] args) {
        final String params = System.getProperty(MAIN_PARAMETERS);
        if (params != null) {
            String[] passedArgs = params.split(" ");
            
            List<String> effectiveParams = new ArrayList<>();
            for (int i = 0; i < passedArgs.length; i++) {
                if (passedArgs[i] != null && passedArgs[i].contains(PLACEHOLDER_PREFIX)) {
                    if (args != null) {
                        for (int j = args.length - 1; j >= 0; j--) {
                            passedArgs[i] = passedArgs[i].replace(PLACEHOLDER_PREFIX + (j + 1), args[j]);
                        }
                    }
                }
                if (passedArgs[i] != null && passedArgs[i].equals(PLACEHOLDER_FULL_LIST)) {
                    if (args != null) {
                        effectiveParams.addAll(Arrays.asList(args));
                    }
                } else {
                    effectiveParams.add(passedArgs[i]);
                }
            }
            args = effectiveParams.toArray(new String[effectiveParams.size()]);
        }

        Object[] objectArray = null;
        objectArray = new Object[1];
        objectArray[0] = args;
        return objectArray;
    }

    static void createProcessStartedIndicatorFile() {
        final File piFile = deriveProcessIndicatorFile();
        if (piFile != null) {
            String pid = ManagementFactory.getRuntimeMXBean().getName();
            pid = pid.substring(0, pid.indexOf("@"));
            try (FileWriter fileWriter = new FileWriter(piFile, false)) {
                fileWriter.write(pid);
                piFile.deleteOnExit();
                log("Process started indicator file created: " + piFile);
            } catch (IOException e) {
                throw new RuntimeException("Process started indicator file cannot be created: " + piFile, e);
            }
        }
    }

    static boolean checkPrerequisites() {
        final String threadPatterns = System.getProperty(PROCESS_THREAD_PATTERNS);
        if (threadPatterns != null) {
            if (!checkAllThreadNamePatternMatched(threadPatterns.split(","))) {
                return false;
            }
        }
        return true;
    }

    static File deriveProcessIndicatorFile() {
        final String piFileName_Deprecated = System.getProperty(PROCESS_STARTED_INDICATOR_FILE__DEPRECATED);
        final String piFileName = System.getProperty(PROCESS_STARTED_INDICATOR_FILE, piFileName_Deprecated);
        final File piFile;
        if (piFileName != null) {
            piFile = new File(piFileName);
        } else {
            piFile = null;
        }
        return piFile;
    }

    static void assertNotNull(Object subject, String failureMessage) {
        if (subject == null) {
            throw new IllegalArgumentException(failureMessage);
        }
    }
    
    static boolean checkAllThreadNamePatternMatched(String[] expectedThreadNamePatterns) {
        final List<String> expectedThreadNameList = Arrays.asList(expectedThreadNamePatterns);
        log("Verification of thread prerequisites: " + expectedThreadNameList);

        final Set<Thread> threads = Thread.getAllStackTraces().keySet();
        final Set<String> unmatchedPatterns = new HashSet<>();
        unmatchedPatterns.addAll(expectedThreadNameList);
        
        for (Thread thread : threads) {
            if (thread.isAlive()) {
                final String threadName = thread.getName();
                final Iterator<String> iterator = unmatchedPatterns.iterator();
                while (iterator.hasNext()) {
                    final String pattern = iterator.next().trim();
                    if (threadName.matches(pattern)) {
                        iterator.remove();
                    }
                }
            }
        }
        
        log("The following threads are not yet available: " + unmatchedPatterns);
        return unmatchedPatterns.isEmpty();
    }

    static void doStop(Class<?> clazz, String[] args) {
        try {
            final String stopMethodName = System.getProperty(STOP_METHOD);
            if (stopMethodName != null) {
                final Method stopMethod = clazz.getDeclaredMethod(stopMethodName, String[].class);
                assertNotNull(stopMethod, "Method " + stopMethodName + "(String[]) must be defined in class '" + clazz + "'.");
                stopMethod.setAccessible(true);
                stopMethod.invoke(null, composeArguments(args));
            }
        } catch (RuntimeException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log(e);
        }
    }
    
    private static void log(Throwable throwable) {
        // NOTE: the implementation uses intentionally system.err to get the best alignment in the
        //   wrapper log.
        System.err.println("ApplicationProxy: " + throwable.getMessage());
        throwable.printStackTrace(System.err);
    }

    static void log(String message) {
        // NOTE: the implementation uses intentionally system.out to get the best alignment in the
        //   wrapper log.
        System.out.println("ApplicationProxy: " + message);
    }

}
