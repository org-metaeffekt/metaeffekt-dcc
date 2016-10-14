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
package org.metaeffekt.dcc.controller.commands;

import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.execution.Executor;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;

/**
 * @author Alexander D.
 */
public class InitializeCommand extends AbstractHostBasedCommand {

    public InitializeCommand(ExecutionContext executionContext) {
        super(executionContext);
    }

    @Override
    public Commands getCommandVerb() {
        return Commands.INITIALIZE;
    }

    @Override
    protected void doExecuteCommand(Executor executor) {
        // execute an implicit clean before performing the initialize
        // this is a secondary fallback for older agents, which do 
        // not implicitly clean on initialize. See DCC-293
        executor.clean();

        executor.initialize();
    }

}
