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
package org.metaeffekt.dcc.controller.execution;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.metaeffekt.dcc.agent.DccAgentEndpoint;
import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.DccProperties;
import org.metaeffekt.dcc.commons.dependency.UnitDependencies;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.execution.Executor;
import org.metaeffekt.dcc.commons.mapping.Capability;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Identifiable;
import org.metaeffekt.dcc.commons.mapping.Profile;

/**
 * @author Alexander D.
 */
public class ExecutorFactory {

    protected static final Logger LOG = LoggerFactory.getLogger(ExecutorFactory.class);

    private static final String LOCALHOST_NAME = "localhost";
    private static final String LOCALHOST_IP = "127.0.0.1";
    
    public static Executor create(ConfigurationUnit unit, ExecutionContext executionContext) {
        Capability hostCapability = extractHostCapability(executionContext.getProfile(), unit, executionContext);
        
        if (hostCapability == null) {
            throw new IllegalStateException("No host found for unit with id " + unit.getUniqueId());
        }
        
        String hostname = getProperty(DccProperties.DCC_AGENT_HOST_NAME,  hostCapability, executionContext);
        int port = determinePort(hostCapability, executionContext);
        
        // FIXME-KKL: is this the final decision? Why not require an local agent to be started?
        //  Ok, it is inefficient (binary transfer and stuff). Are there other aspects to think of?
        //  What about user rights or other preconditions that may vary between agent and shell?
        if (hostname.equals(LOCALHOST_NAME) || hostname.equals(LOCALHOST_IP)) {
            LOG.debug("Using local proxy for host [{}].", hostname);
            return new LocalExecutor(executionContext, false);
        } else {
            LOG.debug("Using remote agent for host [{}].", hostname);
            return new RemoteExecutor(executionContext, hostname, port);
        }
    }
    
    public static Executor createInstallationHostExecutor(ExecutionContext executionContext) {
        return new LocalExecutor(executionContext, true);
    }

     private static Capability extractHostCapability(Profile profile, ConfigurationUnit unit, ExecutionContext executionContext) {
        Capability host = extractHostCapabilityFromUnit(profile, unit, executionContext);
        
        if (host != null) {
            return host;
        }
        else {
            UnitDependencies unitDependencies = profile.getUnitDependencies();
            Map<Id<UnitId>, List<Id<UnitId>>> upstreamMatrix = unitDependencies.getUpstreamMatrix();
            List<Id<UnitId>> upstreamDependencies = upstreamMatrix.get(unit.getId());
            
            for (Id<UnitId> upstreamUnitId : upstreamDependencies) {
                unit = profile.findUnit(upstreamUnitId);
                host = extractHostCapabilityFromUnit(profile, unit, executionContext);
                if (host != null) {
                    return host;
                }
            }
        }
        return null;
    }
    
    private static Capability extractHostCapabilityFromUnit(Profile profile, ConfigurationUnit unit, ExecutionContext executionContext) {
        Capability hostCapability = unit.findProvidedCapabilityWithCapabilityDefinition(DccConstants.HOST_CAPABILITY);
        if (hostCapability != null) {
            String hostname = getProperty(DccProperties.DCC_AGENT_HOST_NAME, hostCapability, executionContext);
            if (StringUtils.isNotBlank(hostname)) {
                return hostCapability;
            }
        }
       
        return null;
    }

    private static int determinePort(Capability hostCapability, ExecutionContext executionContext) {
        String sPort = getProperty(DccProperties.DCC_AGENT_PORT, hostCapability, executionContext);
        if (sPort != null) {
            return Integer.parseInt(sPort);
        }
        else {
            return DccAgentEndpoint.DEFAULT_PORT;
        }
    }
    
    private static String getProperty(String key, Identifiable hostCapability, ExecutionContext executionContext) {
        Properties unitProperties = executionContext.getPropertiesHolder().getProperties(hostCapability);
        return unitProperties.getProperty(key);
    }

}
