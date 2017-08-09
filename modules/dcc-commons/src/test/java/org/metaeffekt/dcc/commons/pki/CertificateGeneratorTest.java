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
package org.metaeffekt.dcc.commons.pki;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.operator.OperatorException;
import org.junit.Test;
import org.metaeffekt.core.common.kernel.ant.log.LoggingProjectAdapter;

/**
 * Generates the certificate required for using SSL. 
 */
public class CertificateGeneratorTest {

    @Test
    public void testGenerateKeyAndTrustStore() throws GeneralSecurityException, IOException, OperatorException {
        final File componentsDir = new File("target/test/certificates");

        Delete delete = new Delete();
        delete.setDir(componentsDir);
        delete.execute();
        
        final File testResourcesDir = new File("src/test/resources/certificates/definition-01");
        if (!testResourcesDir.exists()) {
            throw new IllegalArgumentException("Source directory does not exist.");
        }

        Copy copy = new Copy();
        copy.setProject(getAntProject());
        FileSet testSet = new FileSet();
        testSet.setDir(testResourcesDir);
        testSet.setIncludes("**/*");
        copy.add(testSet);

        copy.setTodir(componentsDir);
        copy.execute();

        String[] components = { "ext", "xyz-ca", "xyz-root", "xyz-app", "xyz-idm", "xyz-directory", "xyz-csr" };
        
        for (String componentName : components) {
            CertificateManager certificateManager = new CertificateManager(componentsDir, componentName);
            certificateManager.createOrComplete();
            certificateManager.setAntProject(getAntProject());
        }
    }
    
    protected Project getAntProject() {
        return new LoggingProjectAdapter();
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void listSupportedSubjectAttributes() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Class<?> clazz = BCStyle.class;
        
        Field field = clazz.getDeclaredField("DefaultSymbols");
        
        field.setAccessible(true);
        final Map<?, ?> object = (Map<?, ?>) field.get(null);
        final ArrayList arrayList = new ArrayList(object.values());
        Collections.sort(arrayList);
        System.out.println(arrayList);
        
    }

}
