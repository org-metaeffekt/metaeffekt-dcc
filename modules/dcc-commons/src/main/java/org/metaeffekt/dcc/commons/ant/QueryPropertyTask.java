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
package org.metaeffekt.dcc.commons.ant;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.Task;

public class QueryPropertyTask extends Task {

    private String property;
    private String queryKey;

    private Map<String, Query> keyQueryMap;
    
    /**
     * Executes the task.
     * 
     * @see org.apache.tools.ant.Task#execute()
     */
    @Override
    public void execute() {
        keyQueryMap = new HashMap<>();
        
        keyQueryMap.put("runtime.memory.free", new Query() {
            @Override
            public String queryValue(String key) {
                return String.valueOf(Runtime.getRuntime().freeMemory());
            }
        });

        keyQueryMap.put("runtime.memory.total", new Query() {
            @Override
            public String queryValue(String key) {
                return String.valueOf(Runtime.getRuntime().totalMemory());
            }
        });

        keyQueryMap.put("system.memory.physical.total", new Query() {
            @Override
            public String queryValue(String key) {
                // we need to access a oracle / sun specific implementation to get the total
                // physical memory. THIS WORKS ONLY WITH ORACLE JVMs
                Long totalPhysicalMem = getValueOfMethod("getTotalPhysicalMemorySize");
                return String.valueOf(totalPhysicalMem);
            }

        });

        keyQueryMap.put("system.memory.physical.free", new Query() {
            @Override
            public String queryValue(String key) {
                Long totalPhysicalMem = getValueOfMethod("getFreePhysicalMemorySize");
                
                // cannot determine (let the recipient handle it)
                return String.valueOf(totalPhysicalMem);
            }
        });
        
        keyQueryMap.put("system.memory.virtual.total", new Query() {
            @Override
            public String queryValue(String key) {
                // we need to access a oracle / sun specific implementation to get the total
                // physical memory. THIS WORKS ONLY WITH ORACLE JVMs
                Long totalPhysicalMem = getValueOfMethod("getTotalPhysicalMemorySize");
                Long totalSwapMem = getValueOfMethod("getTotalSwapSpaceSize");
                
                if (totalPhysicalMem != null && totalSwapMem != null) {
                    return String.valueOf(totalPhysicalMem.longValue() + totalSwapMem.longValue());
                }
                return null;
            }

        });

        keyQueryMap.put("system.memory.virtual.free", new Query() {
            @Override
            public String queryValue(String key) {
                Long freePhysicalMem = getValueOfMethod("getFreePhysicalMemorySize");
                Long freeSwapMem = getValueOfMethod("getFreeSwapSpaceSize");
                
                if (freePhysicalMem != null && freeSwapMem != null) {
                    return String.valueOf(freePhysicalMem.longValue() + freeSwapMem.longValue());
                }
                return null;
            }
        });

        
        Query query = keyQueryMap.get(queryKey);
        if (query != null) {
            // overwrite the property in place:
            PropertyUtils.setProperty(property, query.queryValue(queryKey), 
                PropertyUtils.PROPERTY_PROJECT_LEVEL, getProject());
        }
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getQueryKey() {
        return queryKey;
    }

    public void setQueryKey(String queryKey) {
        this.queryKey = queryKey;
    }

    private abstract static class Query {
        public abstract String queryValue(String key);
    }

    private Long getValueOfMethod(String methodName) {
        OperatingSystemMXBean osMBean = ManagementFactory.getOperatingSystemMXBean();
        try {
//            for (Method m : osMBean.getClass().getDeclaredMethods()) {
//                System.out.println(m);
//            }
            
            Method method = osMBean.getClass().getDeclaredMethod(methodName, (Class<?>[]) null);
            method.setAccessible(true);
            Long value = (Long) method.invoke(osMBean);
            if (value != null) {
                // NOTE: we use the factor 1 / 1024 / 1024 to be accurate with
                //  common definitions and tools. However we get the real capacity minus
                //  some overhead (the usable capacity) returned. We need to compensate
                //  this difference, when defining thresholds.
                return value.longValue() / 1024 / 1024;
            }
        } catch (Exception e) {
            // cannot determine (let the recipient handle it)
        }
        // in case we cannot access the method we do not do anything and return null
        return null;
    }
    
}
