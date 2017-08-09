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
package org.metaeffekt.dcc.commons.ant.wrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import org.metaeffekt.dcc.commons.DccConstants;
import org.metaeffekt.dcc.commons.DccProperties;
import org.metaeffekt.dcc.commons.DccUtils;
import org.metaeffekt.dcc.commons.domain.Id;
import org.metaeffekt.dcc.commons.domain.Type.CapabilityId;
import org.metaeffekt.dcc.commons.domain.Type.UnitId;
import org.metaeffekt.dcc.commons.mapping.Attribute;
import org.metaeffekt.dcc.commons.mapping.AttributeKey;
import org.metaeffekt.dcc.commons.mapping.Capability;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Identifiable;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;

public class ProfileWrapper extends PropertiesHolderWrapper {
    
    private static String LINE_SEPARATOR = System.getProperty("line.separator");
    
    private final Profile profile;
    
    public ProfileWrapper(WrapperContext wrapperContext, Profile profile, PropertiesHolder propertiesHolder) {
        super(wrapperContext, propertiesHolder);
        this.profile = profile;
    }

    public UnitWrapper unit(String unitIdString) {
        Validate.notBlank(unitIdString, "The unitId must not be null");
        final Id<UnitId> unitId = Id.createUnitId(unitIdString);
        final ConfigurationUnit unit = profile.findUnit(unitId);
        Validate.notNull(unit, "The unit with id %s could not be found.", unitId);
        Validate.isTrue(!unit.isAbstract(), "The unit is abstract. Please use a concrete unit.", unitId);
        return new UnitWrapper(getWrapperContext(), unit, getPropertiesHolder());
    }

    public PropertiesWrapper deploymentProperties() {
        final Properties p = profile.getDeploymentProperties();
        Validate.notNull(p, "No deployment properties accessible.");
        return new PropertiesWrapper(getWrapperContext(), p);
    }

    public PropertiesWrapper solutionProperties() {
        final Properties p = profile.getSolutionProperties();
        Validate.notNull(p, "No solution properties accessible.");
        return new PropertiesWrapper(getWrapperContext(), p);
    }

    public class UnitWrapper extends PropertiesHolderWrapper {
        final ConfigurationUnit unit;
        public UnitWrapper(WrapperContext wrapperContext, ConfigurationUnit unit, PropertiesHolder p) {
            super(wrapperContext, p);
            this.unit = unit;
        }
        public PropertyWrapper get(String key, String defaultValue) {
            return get(unit, key, defaultValue);
        }
        public CapabilityWrapper capability(String capabilityIdString) {
            Validate.notBlank(capabilityIdString, "The capabilityId must not be null");
            final Id<CapabilityId> capabilityId = Id.createCapabilityId(capabilityIdString);
            final Capability capability = unit.findProvidedCapability(capabilityId);
            Validate.notNull(capability, "The provided capability with id %s could not be found.", capabilityId);
            return new CapabilityWrapper(getWrapperContext(), capability, getPropertiesHolder());
        }

        public ConfigurationUnit getUnit() {
            return this.unit;
        }
        
        public String header() {
            StringBuilder diff = new StringBuilder();
            diff.append("#").append(" ").append(unit.getUniqueId()).append(LINE_SEPARATOR);
            if (!StringUtils.isBlank(unit.getDescription())) {
                diff.append(formatComment(unit.getDescription(), "#  "));
            }
            getWrapperContext().touch(unit);
            return diff.toString();
        }
    }

    public class CapabilityWrapper extends PropertiesHolderWrapper {
        final Capability capability;
        public CapabilityWrapper(WrapperContext wrapperContext, Capability capability, PropertiesHolder p) {
            super(wrapperContext, p);
            this.capability = capability;
        }
        public PropertyWrapper get(String key, String defaultValue) {
            return get(capability, key, defaultValue);
        }
    }
    
    public String diffDeploymentProperties(ProfileWrapper targetProfileWrapper) {
        final Properties referenceProperties = getProfile().getDeploymentProperties();

        final StringBuilder diff = new StringBuilder();
        final Map<Identifiable, StringBuilder> unitDiffMap = new HashMap<>();
        
        // 1st pass: do a first run on unit attribute level
        for (ConfigurationUnit unit : profile.getUnits(false)) {
            StringBuilder unitDiff = new StringBuilder();
            diffAttributes(targetProfileWrapper, referenceProperties, unit, unitDiff);
            unitDiffMap.put(unit, unitDiff);
            
            // remove unit marker. so that headers are again included (capability diff)
            getWrapperContext().untouch(unit);
        }
        
        // then re-evaluate the profile (including the just derived unit level properties)
        final Profile targetProfile = targetProfileWrapper.getProfile();
        final PropertiesHolder targetPropertiesHolder = targetProfile.createPropertiesHolder(false);
        targetPropertiesHolder.setDeploymentProperties(getWrapperContext().getDerivedDeploymentProperties());
        targetProfile.evaluate(targetPropertiesHolder);
        
        final ProfileWrapper reevaluatedTargetProfileWrapper = new ProfileWrapper(getWrapperContext(), targetProfile, targetPropertiesHolder);

        // 2nd pass: evaluate capability level (with derived values)
        for (ConfigurationUnit unit : profile.getUnits(false)) {
            StringBuilder unitDiff = unitDiffMap.get(unit);
            diffCapabilities(reevaluatedTargetProfileWrapper, referenceProperties, unit, unitDiff);
        }
        
        // append to overall diff (organized by unit)
        for (ConfigurationUnit unit : profile.getUnits(false)) {
            appendWithHeaderIfRequired(unit, diff, unitDiffMap.get(unit));
        }
        
        diffProperties(reevaluatedTargetProfileWrapper, referenceProperties, DccProperties.DCC_DEPLOYMENT_PROPERTIES_WHITELIST, diff);
        
        return diff.toString();
    }

    public String diffDeploymentProperties(ProfileWrapper targetProfileWrapper, UnitWrapper unitWrapper) {
        StringBuilder unitDiff = new StringBuilder();
        
        diffAttributes(targetProfileWrapper, getProfile().getDeploymentProperties(), unitWrapper.getUnit(), unitDiff);

        StringBuilder diff = new StringBuilder();
        appendWithHeaderIfRequired(unitWrapper.getUnit(), diff, unitDiff);
        return diff.toString();
    }
    
    public String diffSolutionProperties(ProfileWrapper targetProfileWrapper) {
        StringBuilder diff = new StringBuilder();
        diffProperties(targetProfileWrapper, getProfile().getSolutionProperties(), DccProperties.DCC_SOLUTION_PROPERTIES_WHITELIST, diff);
        return diff.toString();
    }

    
    private void appendWithHeaderIfRequired(ConfigurationUnit unit, StringBuilder diff, StringBuilder unitDiff) {
        if (!getWrapperContext().touched(unit)) {
            if (unitDiff.length() > 0) {
                diff.append(DccConstants.PROPERTIES_SEPARATOR);
                diff.append(LINE_SEPARATOR);
                diff.append(new UnitWrapper(getWrapperContext(), unit, getPropertiesHolder()).header()).append(LINE_SEPARATOR);
            }
        }
        diff.append(unitDiff);
    }
    
    private void diffAttributes(ProfileWrapper targetProfileWrapper, Properties referenceProperties, ConfigurationUnit unit, StringBuilder diff) {
        for (Attribute attribute : unit.getAttributes()) {
            String key = attribute.getKey();
            String description = attribute.getDescription();
            diff(unit, key, description, targetProfileWrapper.getPropertiesHolder(), referenceProperties, diff);
        }
    }
    
    private void diffCapabilities(ProfileWrapper targetProfileWrapper, Properties referenceProperties, ConfigurationUnit unit, StringBuilder diff) {
        for (Capability capability : unit.getProvidedCapabilities()) {
            for (AttributeKey attribute : capability.getCapabilityDefinition().getAttributeKeys()) {
                String key = attribute.getKey();
                String description = attribute.getDescription();
                diff(capability, key, description, targetProfileWrapper.getPropertiesHolder(), referenceProperties, diff);
            }
        }
    }

    private void diff(Identifiable identifiable, String key, String description, PropertiesHolder targetPropertiesHolder, Properties referenceProperties, StringBuilder unitSummary) {
        
        String propertyKey = DccUtils.deriveAttributeIdentifier(identifiable, key);
        String targetValue = targetPropertiesHolder.getProperty(identifiable, key);
        String sourceValue = getPropertiesHolder().getProperty(identifiable, key);
        
        boolean coveredByReferenceProperties = referenceProperties.containsKey(propertyKey);
        
        boolean unequalValue = (sourceValue == null && targetValue != null) || 
                (sourceValue != null && !sourceValue.equals(targetValue));
        
        if (unequalValue || coveredByReferenceProperties) {
            if (!getWrapperContext().touched(propertyKey)) {
                if (coveredByReferenceProperties) {
                    if (!StringUtils.isBlank(description)) {
                        String formattedDescription = formatComment(description, "# ");
                        unitSummary.append(formattedDescription);
                    }
                    if (unequalValue) {
                        unitSummary.append("#").append(" Please revise the derived settings...").append(LINE_SEPARATOR);
                        unitSummary.append("#").append("   original value: ").append(sourceValue).append(LINE_SEPARATOR);
                        unitSummary.append("#").append("   proposed new value: ").append(targetValue).append(LINE_SEPARATOR);
                    }
                    unitSummary.append(get(identifiable, key, sourceValue).renderProperty(propertyKey, sourceValue, false)).append(LINE_SEPARATOR);
                    unitSummary.append(LINE_SEPARATOR);
                }
                getWrapperContext().touch(propertyKey);
            }
        }
    }

    private void diffProperties(ProfileWrapper targetProfileWrapper, Properties referenceProperties, String[] propertyWhitelist, StringBuilder diff) {
        
        Profile targetProfile = targetProfileWrapper.getProfile();
        PropertiesHolder targetPropertiesHolder = targetProfileWrapper.getPropertiesHolder();
        
        // we do not want to modify the properties directly, so we make an independent list
        // the list should preserve the internal ordering
        final List<String> obsolete = new ArrayList<>();
        final List<String> takeover = new ArrayList<>();

        if (referenceProperties != null) {
            for (Object key : referenceProperties.keySet()) {
                obsolete.add(key.toString());
            }
        }

        for (ConfigurationUnit unit : targetProfile.getUnits(false)) {
            
            // anticipate attributes
            for (Attribute attribute : unit.getAttributes()) {
                String attributeIdentifier = DccUtils.deriveAttributeIdentifier(unit, attribute.getKey());
                if (obsolete.remove(attributeIdentifier)) {
                    takeover.add(attributeIdentifier);
                }
            }

            // anticipate provided capabilities
            for (Capability capability : unit.getProvidedCapabilities()) {
                for (AttributeKey attribute : capability.getCapabilityDefinition().getAttributeKeys()) {
                    String attributeIdentifier = DccUtils.deriveAttributeIdentifier(capability, attribute.getKey());
                    if (obsolete.remove(attributeIdentifier)) {
                        takeover.add(attributeIdentifier);
                    }
                }
            }

            // second pass: check that all things covered in the evaluation
            for (Object key : targetPropertiesHolder.getProperties(unit).keySet()) {
                if (obsolete.remove(key.toString())) {
                    takeover.add(key.toString());
                }
            }
        }
        
        // eliminate those that where already handled
        for (String key : getWrapperContext().getTouchedIdentifiers()) {
            takeover.remove(key);
            obsolete.remove(key);
        }
        
        boolean dccPropertiesRendered = false;
        for (String key : propertyWhitelist) {
            // take it out for the obsolete list
            if (obsolete.remove(key)) {
                takeover.add(key);
            }
        }
        
        for (String key : takeover) {
            // nevertheless take over the property from the source
            if (!dccPropertiesRendered) {
                diff.append(DccConstants.PROPERTIES_SEPARATOR);
                diff.append(LINE_SEPARATOR);
                diff.append("# The following properties were taken over from the previous version.").append(LINE_SEPARATOR);
                diff.append(LINE_SEPARATOR);
                dccPropertiesRendered = true;
            }
            diff.append(new PropertiesWrapper(getWrapperContext(), referenceProperties).get(key).conditionalProperty(targetProfileWrapper)).append(LINE_SEPARATOR);
            diff.append(LINE_SEPARATOR);
        }

        boolean obsoleteHeaderRendered = false;
        for (String key : obsolete) {
            if (!obsoleteHeaderRendered) {
                diff.append(DccConstants.PROPERTIES_SEPARATOR);
                diff.append(LINE_SEPARATOR);
                diff.append("# The following properties were identified as obsolete. Please revise.").append(LINE_SEPARATOR);
                diff.append(LINE_SEPARATOR);
                obsoleteHeaderRendered = true;
            }
            diff.append(new PropertiesWrapper(getWrapperContext(), referenceProperties).get(key).property()).append(LINE_SEPARATOR);
        }
    }
    
    public Profile getProfile() {
        return profile;
    }
    
}
