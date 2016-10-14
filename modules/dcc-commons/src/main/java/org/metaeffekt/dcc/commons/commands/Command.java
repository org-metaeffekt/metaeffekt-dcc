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
package org.metaeffekt.dcc.commons.commands;

import java.io.IOException;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;

/**
 * Interface to specify the {@link Command} contract. A command is an operation in the life cycle
 * of a deployment profile. When executing a command it will generally be executed for all units
 * in the profile. Its execution can be controlled using additional execution parameters.
 * 
 * @author Karsten Klein
 */
public interface Command {

    /**
     * Execute the command.
     * 
     * @param force Enforces the execution of the command. Otherwise the command may be skipped if
     *    the underlying engine determines that it not required.
     *    
     * @throws IOException Throws {@link IOException} as commands usually produces effect in the 
     *    filesystem.
     */
    void execute(boolean force) throws IOException;
    
    /**
     * Execute the command.
     * 
     * @param force Enforces the execution of the command. Otherwise the command may be skipped if
     *    the underlying engine determines that it not required.
     * @param unitId The unitId can be used to restrict the command to a particular unit. If the
     *   unitId is omitted (<code>null</code>) the command will be executed to all units that
     *   provide the command.
     *    
     * @throws IOException Throws {@link IOException} as commands usually produces effect in the 
     *    filesystem.
     */
    void execute(boolean force, Id<UnitId> unitId) throws IOException;
    
    /**
     * Commands can be skipped, in case they have already been executed. Whether a skip is possible
     * for a given command can be requested by the this method.
     * 
     * @return <code>true</code> in case the command allows to be skipped.
     */
    boolean allowsToBeSkipped();
    
    /**
     * Commands are either executed on a target host or on the installation host. The latter is also
     * referred to as local execution.
     * 
     * @return Boolean indicating whether the command is a local command.
     */
    public boolean isLocal();

}
