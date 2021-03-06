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

import java.util.List;

import com.google.common.collect.Lists;
import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.controller.execution.ExecutionContext;


/**
 * @author Jochen K.
 */
public class UninstallCommand extends AbstractUnitBasedCommand {

    public UninstallCommand(ExecutionContext executionContext) {
        super(executionContext);
    }

    @Override
    public Commands getCommandVerb() {
        return Commands.UNINSTALL;
    }

    @Override
    protected boolean isReversive() {
        return true;
    }

    @Override
    public boolean allowsToBeSkipped() {
        return false;
    }

    @Override
    public boolean isSequential() {
        return false;
    }

}
