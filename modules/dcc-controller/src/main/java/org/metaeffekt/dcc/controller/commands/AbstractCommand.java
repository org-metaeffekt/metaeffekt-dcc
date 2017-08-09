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
package org.metaeffekt.dcc.controller.commands;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.metaeffekt.dcc.commons.DccUtils;
import org.metaeffekt.dcc.commons.commands.Command;
import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.execution.ExecutionStateHandler;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;


/**
 *  Abstract base class for all {@link Command commands}.
 *
 *  @author Karsten Klein
 */
abstract class AbstractCommand implements Command {

    protected static final int NUMBER_OF_THREADS = Integer.parseInt(System.getProperty("dcc.execution.thread.count", "20"));

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractCommand.class);

    private ExecutionContext executionContext;
    private ExecutionStateHandler executionStateHandler;

    public AbstractCommand(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    @Override
    public void execute(boolean force) {
        execute(force, false, null);
    }

    @Override
    public void execute(boolean force, boolean parallel) {
        execute(force, parallel, null);
    }

    @Override
    public void execute(boolean force, Id<UnitId> unitId) {
        execute(force, false, unitId);
    }

    @Override
    public void execute(boolean force, boolean parallel, Id<UnitId> unitId) {
        executionContext.prepareForExecution();
        beforeExecution();
        long timestamp = System.currentTimeMillis();
        doExecute(force, parallel, unitId);
        afterSuccessfulExecution("", String.format("[%s]", getCommandVerb()), timestamp);
    }

    protected abstract void doExecute(boolean force, boolean parallel, Id<UnitId> unitId);

    protected abstract Commands getCommandVerb();

    protected void beforeExecution() {}

    protected File getConfigurationTargetPath() {
        return DccUtils.workingTmpConfigDir(executionContext.getSolutionDir(), executionContext.getProfile().getDeploymentId());
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    protected ExecutionStateHandler getExecutionStateHandler() {
        if (executionStateHandler == null) {
            executionStateHandler = new ExecutionStateHandler(executionContext.getTargetDir(), executionContext.getSolutionDir());
        }
        return executionStateHandler;
    }
    
    @Override
    public String toString() {
        return "Command [" + getCommandVerb() + "]";
    }

    @Override
    public boolean allowsToBeSkipped() {
        return true;
    }
    
    protected void afterSuccessfulExecution(String indent, String context, long startTimestamp) {
        final long duration = System.currentTimeMillis() - startTimestamp;
        LOG.info("{} Success. Command {} took {}.{} seconds.", indent, context, duration / 1000, duration % 1000);
    }
    
    /**
     * Commands are either executed on a target host or on the installation host. The latter is also
     * referred to as local execution.
     * 
     * @return
     */
    @Override
    public boolean isLocal() {
        return false;
    }

    /**
     * Logs the execptions to the error log and throws an exception if exceptions have accumulated in exceptions.
     *
     *
     * @param exceptionMap The exceptions maps the
     */
    protected void handleExceptions(Map<Id<?>, Throwable> exceptionMap) {
        if (!exceptionMap.isEmpty()) {
            for (Map.Entry<Id<?>, Throwable> entry : exceptionMap.entrySet()) {
                LOG.error(String.format("Error executing command [%s] for unit [%s]: %s", getCommandVerb(),
                        entry.getKey(), entry.getValue().getMessage()), entry.getValue());
            }
            throw new IllegalStateException("Aborting execution due to previous errors.");
        }
    }

    protected void awaitTerminationOrCancelOnException(ExecutorService executor, Map<Id<?>, Throwable> exceptions) {
        // wait until all commands have finished or an error occurred
        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                executor.awaitTermination(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // nothing to do
            }

            // when there is an exception, do not attempt the commands that have not started yet
            // and try to terminate the currently running (not guaranteed)
            if (!exceptions.isEmpty()) {
                executor.shutdownNow();
            }
        }
    }

}
