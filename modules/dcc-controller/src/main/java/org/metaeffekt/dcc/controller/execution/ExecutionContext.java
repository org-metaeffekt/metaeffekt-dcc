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
package org.metaeffekt.dcc.controller.execution;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.metaeffekt.dcc.commons.commands.Commands;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.HostName;
import org.metaeffekt.dcc.commons.domain.Type.PackageId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.execution.Executor;
import org.metaeffekt.dcc.commons.mapping.Binding;
import org.metaeffekt.dcc.commons.mapping.Capability;
import org.metaeffekt.dcc.commons.mapping.CommandDefinition;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Identifiable;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.mapping.RequiredCapability;
import org.metaeffekt.dcc.controller.DccControllerConstants;

// FIXME move to dcc-commons (resolve compile time dependency to ExecutorFactory)
// FIXME-KKL this class is rather a command execution context.
//  The unitsExecutors themselves should be able to cope with the ScriptExecutionContext. Split.
/**
 * @author Douglas B.
 */
public class ExecutionContext {
    
    protected static final Logger LOG = LoggerFactory.getLogger(ExecutionContext.class);
            
    private Profile profile;
    
    private PropertiesHolder propertiesHolder;
    
    // The solutionDir is where profile.xml and /packages reside (presumption is that this is the 
    // folder structure of a solution)
    private File solutionDir;
    
    private File targetBaseDir;
    private File targetDir;

    private Map<Id<HostName>, Executor> hostsExecutors;
    private Map<Id<UnitId>, Id<HostName>> unitsHosts;
    private Executor installationHostExecutor;
    
    private Map<Id<UnitId>, Id<UnitId>> unitToUnitHosts;
    
    private boolean failOnError = false;
    private boolean force = false;

    private SSLConfiguration sslConfiguration;

    public ExecutionContext(SSLConfiguration sslConfiguration) {
        Validate.notNull(sslConfiguration);
        this.sslConfiguration = sslConfiguration;
    }

    public ExecutionContext() {
    }

    public SSLConfiguration getSslConfiguration() {
        return sslConfiguration;
    }

    public boolean containsProfile() {
        return profile != null;
    }
    
    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public synchronized void prepareForExecution() {
        if (propertiesHolder == null) {
            propertiesHolder = profile.createPropertiesHolder(true);
            profile.evaluate(propertiesHolder);
        }
    }
    
    public Properties getSolutionProperties() {
        return profile.getSolutionProperties();
    }

    public Properties getDeploymentProperties() {
        return profile != null ? profile.getDeploymentProperties() : null;
    }

    public File getSolutionDir() {
        return solutionDir;
    }

    public void setSolutionDir(File solutionDir) {
        this.solutionDir = solutionDir;
    }
    
    public void resetContext() {
        this.profile = null;
        this.solutionDir = null;
        this.unitsHosts = null;
        this.unitToUnitHosts = null;
        this.targetDir = null;
        this.targetBaseDir = null;
    }
    
    public Map<Id<HostName>, Executor> getHostsExecutors() {
        initializeExecutors(false);
        return Collections.unmodifiableMap(hostsExecutors);
    }
    
    public File getTargetBaseDir() {
        return targetBaseDir;
    }
    
    public void setTargetBaseDir(File targetBaseDir) {
        this.targetBaseDir = targetBaseDir;
    }

    public void setTargetDir(File targetDir) {
        this.targetDir = targetDir;
    }
    
    public File getTargetDir() {
        if (targetBaseDir == null) {
            throw new IllegalStateException("No targetBaseDir has been configured.");
        }

        if (targetDir == null) {
            final File targetBaseDir = getTargetBaseDir();
            if (profile != null && profile.getDeploymentId() != null) {
                // integrate deployment id in targetDir
                targetDir = new File(targetBaseDir, profile.getDeploymentId().getValue());
            } else {
                targetDir = targetBaseDir;
            }
        }

        return targetDir;
    }
    
    public String getProperty(Identifiable identifiable, String key) {
        return propertiesHolder.getProperty(identifiable, key);
    }

    public Id<PackageId> getPackageId(ConfigurationUnit unit, Commands commandId) {
        final CommandDefinition command = unit.getCommand(commandId);
        return command != null ? command.getPackageId() : null;
    }

    public Executor getExecutorForUnit(Id<UnitId> unitId) {
        initializeExecutors(false);
        Id<HostName> hostName = unitsHosts.get(unitId);
        Executor executor = hostsExecutors.get(hostName);
        if (executor == null) {
            throw new IllegalStateException("No executor found for host ["+hostName+"] of unit ["+unitId+"]");
        }
        return executor;
    }

    public Executor getExecutorForUnitIfExists(Id<UnitId> unitId) {
        if (unitsHosts != null) {
            Id<HostName> hostName = unitsHosts.get(unitId);
            if (hostName != null) {
                return hostsExecutors.get(hostName);
            }
        }
        return null;
    }

    public Executor getExecutorForHost(Id<HostName> hostname) {
        initializeExecutors(false);
        return hostsExecutors.get(hostname);
    }

    public Id<HostName> getHostForUnit(Id<UnitId> unitId) {
        initializeExecutors(false);
        return unitsHosts.get(unitId);
    }

    public Id<UnitId> getHostUnitForUnit(Id<UnitId> unitId) {
        initializeExecutors(false);
        return unitToUnitHosts.get(unitId);
    }
    
    public synchronized void initializeExecutors(boolean force) {
        if (hostsExecutors == null || force) {
            hostsExecutors = new HashMap<>();
            unitsHosts = new HashMap<>();
            unitToUnitHosts = new HashMap<>();
            
            List<ConfigurationUnit> agents = profile.findUnitsWithProvidedCapabilityDefinition("agent.endpoint");
            if (agents != null) {
                Map<Id<UnitId>, Id<HostName>> agentsHostsMappings = createExecutorsForHosts(agents);
                mapAllUnitsToHosts(agentsHostsMappings);
            }
        }
    }

    private void mapAllUnitsToHosts(Map<Id<UnitId>, Id<HostName>> agentsHostsMappings) {
        final List<ConfigurationUnit> allUnits = profile.getUnits();
        for (ConfigurationUnit unit : allUnits) {
            final Id<UnitId> unitId = unit.getId();
            if (!unitToUnitHosts.containsKey(unitId)) {
                Id<UnitId> hostUnitId = mapUnitToHostUnit(unit, agentsHostsMappings);
                unitsHosts.put(unitId, agentsHostsMappings.get(hostUnitId));
                unitToUnitHosts.put(unitId, hostUnitId);
            }
        }
    }

    public Id<UnitId> mapUnitToHostUnit(ConfigurationUnit unit, Map<Id<UnitId>, Id<HostName>> agentsHostsMappings) {
        final Id<UnitId> unitId = unit.getId();

        // first the upstream dependencies are analyzed
        final List<Id<UnitId>> dependencies = profile.getUnitDependencies().getUpstreamMatrix().get(unitId);
        
        final Set<Id<UnitId>> mappedHostUnitIds = new HashSet<>();
        // all possible target host are collected
        for (Id<UnitId> agentId : agentsHostsMappings.keySet()) {
            if (agentId.equals(unitId) || (dependencies != null && dependencies.contains(agentId))) {
                mappedHostUnitIds.add(agentId);
            }
        }

        if (mappedHostUnitIds.contains(unitId)) {
            // the unit is a host unit itself
            return unitId;
        }
        
        if (mappedHostUnitIds.size() == 1) {
            // in case the target host can be uniquely identified we are done
            return mappedHostUnitIds.iterator().next();
        } else {
            if (mappedHostUnitIds.size() > 1) {
                // check whether there is a qualified capability inside the unit
                // it is assumes that is is only one here. This is not further validated.
                for (RequiredCapability requiredCapability : unit.getRequiredCapabilities()) {
                    if (requiredCapability.identifiesHost()) {
                        final Collection<Binding> bindings = profile.findBindings(requiredCapability);
                        for (Binding binding : bindings) {
                            final ConfigurationUnit sourceUnit = binding.getSourceCapability().getUnit();
                            // NOTE we use a recursions here
                            Id<UnitId> hostUnitId = mapUnitToHostUnit(sourceUnit, agentsHostsMappings);
                            if (hostUnitId != null) {
                                return hostUnitId;
                            }
                        }
                    }
                }
                
                // if the above failed and we still do not have identified the host an exception is
                // thrown.
                throw new IllegalStateException(String.format(
                    "The unit [%s] cannot be uniquely mapped to a host: %s", unitId, mappedHostUnitIds));
            } else {
                // ignore the units that have no host relationship (config contributions and so on)
                return null;
            }
        }
    }
    
    public Executor getInstallationHostExecutor(Id<UnitId> unit) {
        if (installationHostExecutor == null) {
            installationHostExecutor = ExecutorFactory.createInstallationHostExecutor(this);
        }
        return installationHostExecutor;
    }
    
    private Map<Id<UnitId>, Id<HostName>> createExecutorsForHosts(List<ConfigurationUnit> unitsWithAgent) {
        Map<Id<HostName>, Executor> hostsExecutors = new HashMap<>();
        Map<Id<UnitId>, Id<HostName>> agentsHosts = new HashMap<>();

        List<Id<UnitId>> agentsWithoutHostNames = new LinkedList<>();
        
        for (ConfigurationUnit unit : unitsWithAgent) {
            Capability hostCapability = unit.findProvidedCapability(Id.createCapabilityId("agent.endpoint"));
            if (hostCapability != null) {
                Properties capabilityProperties = propertiesHolder.getProperties(hostCapability);
                String hostNameString = capabilityProperties.getProperty("name");
                if (StringUtils.isBlank(hostNameString)) {
                    agentsWithoutHostNames.add(unit.getId());
                    continue;
                }
                LOG.debug("Unit [{}] has capability [agent.endpoint] on host [{}].", 
                    unit.getId(), hostNameString);
    
                Id<HostName> hostName = Id.createHostName(hostNameString);
                agentsHosts.put(unit.getId(), hostName);
                
                Executor executor = hostsExecutors.get(hostName);
                if (executor == null) {
                    executor = ExecutorFactory.create(unit, this);
    
                    if (executor.hostAvailable()) {
                        hostsExecutors.put(hostName, executor);
                    } else {
                        throw new IllegalStateException(String.format(
                            "Unable to connect to unit [%s] on host [%s]. Please ensure an agent "
                            + "compatible with version [%s] is installed and running on the host.", 
                            unit.getId(), hostName, DccControllerConstants.DCC_SHELL_VERSION));
                    }
                }
            }
        }
        
        if (!agentsWithoutHostNames.isEmpty()) {
            StringBuilder msg = new StringBuilder("Following units provide agent endpoint but have no host name: [");
            String sep = "";
            for (Id<UnitId> unitId : agentsWithoutHostNames) {
                msg.append(sep);
                msg.append(unitId.toString());
                sep = ", ";
            }
            msg.append("].");
            throw new IllegalStateException(msg.toString());
        }
        
        this.hostsExecutors = hostsExecutors;
        return agentsHosts;
    }
    

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public PropertiesHolder getPropertiesHolder() {
        return propertiesHolder;
    }
    
    public File getLogsBaseDir() {
        return new File(getSolutionDir(), "logs");
    }

}
