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
package org.metaeffekt.dcc.commons.mapping;

import java.io.File;

import org.metaeffekt.dcc.commons.domain.Id;

public class SharedProfilesAndCapabilities {

    private static final File ORIGIN = new File("SharedProfilesAndCapabilities.class");
    private static final String DESCRIPTION = "description";

    public static final CapabilityDefinition CAPABILITY_HOST = 
        new CapabilityDefinition("host").
            add(new AttributeKey("name", ORIGIN, DESCRIPTION, false)).
            add(new AttributeKey("ip", ORIGIN, DESCRIPTION, false));

    public static final CapabilityDefinition CAPABILITY_RUNTIME = 
        new CapabilityDefinition("runtime").
            add(new AttributeKey("port")).
            add(new AttributeKey("host.name")).
            add(new AttributeKey("host.ip",ORIGIN, DESCRIPTION, true));

    public static final CapabilityDefinition CAPABILITY_JDBC_ENDPOINT = 
        new CapabilityDefinition("jdbc.endpoint").
            add(new AttributeKey("url")).
            add(new AttributeKey("username")).
            add(new AttributeKey("password")).
            add(new AttributeKey("driver")).
            add(new AttributeKey("dialect")).
            add(new AttributeKey("validation.query"));

    public static Profile createHsqldbProfile(String hostId, String runtimeId, String instanceId,
            String dbId) throws Exception {
        Profile profile = new Profile();

        // units
        ConfigurationUnit host01 = createHostUnit(hostId);
        ConfigurationUnit hsqldbRuntime01 = createRuntimeUnit(runtimeId);
        ConfigurationUnit hsqldbInstance01 = createInstanceUnit(instanceId, dbId);

        // bindings
        Binding hostToRuntimeBinding = new Binding(
            host01.findProvidedCapability(Id.createCapabilityId("host")),
            hsqldbRuntime01.findRequiredCapability(Id.createCapabilityId("host")));

        Binding runtimeToInstanceBinding = new Binding(
            hsqldbRuntime01.findProvidedCapability(Id.createCapabilityId("runtime")),
            hsqldbInstance01.findRequiredCapability(Id.createCapabilityId("runtime")));

        profile.add(hostToRuntimeBinding);
        profile.add(runtimeToInstanceBinding);
        
        profile.add(host01);
        profile.add(hsqldbRuntime01);
        profile.add(hsqldbInstance01);

        return profile;
    }

    protected static ConfigurationUnit createInstanceUnit(String instanceId, String dbId) throws Exception {
        ConfigurationUnit hsqldbInstance01 = new ConfigurationUnit(instanceId);
        
        final RequiredCapability hsqldbInstanceRequiredRuntimeCapability = 
            new RequiredCapability(Id.createCapabilityId("runtime"), CAPABILITY_RUNTIME, hsqldbInstance01);
        final Capability hsqldbInstanceProvidedJdbcEndpointCapability = 
            new RequiredCapability(Id.createCapabilityId("jdbc.endpoint"), CAPABILITY_JDBC_ENDPOINT, hsqldbInstance01);

        hsqldbInstance01.getRequiredCapabilities().add(hsqldbInstanceRequiredRuntimeCapability);
        hsqldbInstance01.getProvidedCapabilities().add(hsqldbInstanceProvidedJdbcEndpointCapability);
        
        hsqldbInstance01.add(new Attribute("database.username", "sa", ORIGIN));
        hsqldbInstance01.add(new Attribute("database.password", "", ORIGIN));
        hsqldbInstance01.add(new Attribute("database.id", dbId, ORIGIN));
        hsqldbInstance01.add(new Attribute("database.dialect", "org.hibernate.dialect.HSQLDialect", ORIGIN));
        hsqldbInstance01.add(new Attribute("database.driver", "org.hsqldb.jdbcDriver", ORIGIN));
        hsqldbInstance01.add(new Attribute("database.validation.query", "select count(TYPE_NAME) from INFORMATION_SCHEMA.SYSTEM_TABLES", ORIGIN));

        Mapping dbTojdbcEndpointMapping = new Mapping(hsqldbInstanceProvidedJdbcEndpointCapability.getId());
        dbTojdbcEndpointMapping.add(new ExpressionAttributeMapper("username", "${database.username}"));
        dbTojdbcEndpointMapping.add(new ExpressionAttributeMapper("password", "${database.password}"));
        dbTojdbcEndpointMapping.add(new ExpressionAttributeMapper("dialect", "${database.dialect}"));
        dbTojdbcEndpointMapping.add(new ExpressionAttributeMapper("driver", "${database.driver}"));
        dbTojdbcEndpointMapping.add(new ExpressionAttributeMapper("validation.query", "${database.validation.query}"));
        dbTojdbcEndpointMapping.add(new ExpressionAttributeMapper("url", "jdbc:hsqldb:hsql://${runtime[host.name]}:${runtime[port]}/${database.id}"));
        hsqldbInstance01.add(dbTojdbcEndpointMapping);
        hsqldbInstance01.afterPropertiesSet();
        return hsqldbInstance01;
    }

    protected static ConfigurationUnit createRuntimeUnit(String runtimeId) throws Exception {
        ConfigurationUnit hsqldbRuntime01 = new ConfigurationUnit(runtimeId);
        
        RequiredCapability requiredHostCapability = new RequiredCapability(Id.createCapabilityId("host"), CAPABILITY_HOST, hsqldbRuntime01);
        RequiredCapability providedRuntimeCapability = new RequiredCapability(Id.createCapabilityId("runtime"), CAPABILITY_RUNTIME, hsqldbRuntime01);
        
        hsqldbRuntime01.getRequiredCapabilities().add(requiredHostCapability);
        hsqldbRuntime01.getProvidedCapabilities().add(providedRuntimeCapability);
        
        hsqldbRuntime01.add(new Attribute("port", "9999", ORIGIN));

        Mapping hostMapping = new Mapping(providedRuntimeCapability.getId());
        hostMapping.add(new ExpressionAttributeMapper("host.name", "${host[name]}"));
        hostMapping.add(new ExpressionAttributeMapper("host.ip", "${host[ip]}"));
        hsqldbRuntime01.add(hostMapping);

        hsqldbRuntime01.add(new Attribute("installation.package", "hsqldb-2.0.8.tgz", ORIGIN));
        hsqldbRuntime01.add(new Attribute("name", "localhost", ORIGIN));
        hsqldbRuntime01.afterPropertiesSet();
        return hsqldbRuntime01;
    }

    protected static ConfigurationUnit createHostUnit(String hostId) throws Exception {
        final ConfigurationUnit host01 = new ConfigurationUnit(hostId);
        final Capability hsqldbHostProvidedHostCapability = new Capability(Id.createCapabilityId("host"), CAPABILITY_HOST, host01);
        host01.getProvidedCapabilities().add(hsqldbHostProvidedHostCapability);
        host01.afterPropertiesSet();
        return host01;
    }

}
