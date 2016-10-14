package org.metaeffekt.dcc.test;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.mapping.Capability;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;
import org.metaeffekt.dcc.commons.spring.xml.ProfileParser;

public class ProfileEvaluationTest {

    @Test
    public void testMappingViaXml() {
        Profile profile = ProfileParser.parse(new File("target/dcc/dcc-test-deployment-profile.xml"));
            
        PropertiesHolder propertiesHolder = profile.createPropertiesHolder(true);
        profile.evaluate(propertiesHolder);

        propertiesHolder.dump();
    }

}
