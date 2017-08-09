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
package org.metaeffekt.dcc.docgenerator;

import java.util.List;
import java.util.Map;

import org.metaeffekt.dcc.commons.mapping.Binding;
import org.metaeffekt.dcc.commons.mapping.Capability;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.RequiredCapability;

public class BindingLayout {

    public static final int PROVIDED_CAPABILITY_UNIT_OFFSET = 2 * ProfileLayout.CAPABILITY_WIDTH;

    private int sourceX;
    private int sourceY;

    private int intermediateY;

    private int targetX;
    private int targetY;

    private int sourceRoutingOffsetX;
    private int targetRoutingOffsetX;

    private boolean visible = true;

    Map<ConfigurationUnit, Node> unitNodeMap;

    public BindingLayout(ProfileLayout profileLayout, Binding binding, Map<ConfigurationUnit, Node> unitNodeMap) {
        this.unitNodeMap = unitNodeMap;
        if (profileLayout == null) {
            throw new IllegalArgumentException("ProfileLayout is null");
        }
        if (binding == null) {
            throw new IllegalArgumentException(
                String.format("A binding in profile %s is invalid or missing", profileLayout.getProfile().getId()));
        }

        final ConfigurationUnit sourceUnit = binding.getSourceCapability().getUnit();
        final ConfigurationUnit targetUnit = binding.getTargetCapability().getUnit();

        Node sourceNode = unitNodeMap.get(sourceUnit);
        Node targetNode = unitNodeMap.get(targetUnit);

        visible = sourceNode.isVisible() && targetNode.isVisible();

        int sourceXOnGrid = profileLayout.computeOffsetXOnGrid(sourceUnit);
        int sourceYOnGrid = profileLayout.computeOffsetYOnGrid(sourceUnit);
        int sourceOffsetX = profileLayout.computeOffsetX(sourceUnit);
        int sourceOffsetY = profileLayout.computeOffsetY(sourceUnit);

        int targetXOnGrid = profileLayout.computeOffsetXOnGrid(targetUnit);
        int targetYOnGrid = profileLayout.computeOffsetYOnGrid(targetUnit);
        int targetOffsetX = profileLayout.computeOffsetX(targetUnit);
        int targetOffsetY = profileLayout.computeOffsetY(targetUnit);

        sourceX = sourceOffsetX + PROVIDED_CAPABILITY_UNIT_OFFSET;
        sourceY = sourceOffsetY;

        DocGenerator docGenerator = new DocGenerator();

        List<Capability> providedCapabilites = sourceUnit.getProvidedCapabilities();
        providedCapabilites = docGenerator.sortCapabilities(providedCapabilites);
        int capabilityPos = providedCapabilites.indexOf(binding.getSourceCapability());

        sourceY += 62 + 34 * capabilityPos;

        targetX = targetOffsetX;
        targetY = targetOffsetY;

        List<RequiredCapability> requiredCapabilites = targetUnit.getRequiredCapabilities();
        requiredCapabilites = docGenerator.sortCapabilities(requiredCapabilites);
        int reqcapabilityPos = requiredCapabilites.indexOf(binding.getTargetCapability());

        targetY += 62 + 34 * reqcapabilityPos;

        int delta = 4 * targetUnit.getRequiredCapabilities().indexOf(binding.getTargetCapability());
        if (sourceY > targetY) {
            delta *= -1;
        }
        if (sourceY == targetY) {
            delta = 0;
        }

        if (targetYOnGrid == sourceYOnGrid && sourceXOnGrid + 1 == targetXOnGrid) {
            intermediateY = sourceY - delta;
        } else {
            // if (targetXOnGrid == sourceXOnGrid) {
            // intermediateY = (sourceY + targetY) / 2 - delta;
            // } else {
            if (targetOffsetY > sourceOffsetY) {
                    intermediateY = targetOffsetY - 10 - 4 * targetUnit.getRequiredCapabilities().indexOf(binding.getTargetCapability());;
            } else {
                    intermediateY = sourceOffsetY - 10 - 4 * targetUnit.getRequiredCapabilities().indexOf(binding.getTargetCapability());;
            }
            // }
        }

        int providedCapabilityCount = sourceUnit.getProvidedCapabilities().size();
        sourceRoutingOffsetX = 30 + 4 * (providedCapabilityCount - sourceUnit.getProvidedCapabilities().indexOf(binding.getSourceCapability()));
        targetRoutingOffsetX = 10 + 4 * targetUnit.getRequiredCapabilities().indexOf(binding.getTargetCapability());
    }

    public int getSourceX() {
        return sourceX;
    }

    public int getSourceX(boolean withOffset) {
        return sourceX + sourceRoutingOffsetX;
    }

    public void setSourceX(int sourceX) {
        this.sourceX = sourceX;
    }

    public int getSourceY() {
        return sourceY;
    }

    public void setSourceY(int sourceY) {
        this.sourceY = sourceY;
    }

    public int getTargetX() {
        return targetX;
    }

    public int getTargetX(boolean withOffset) {
        return targetX - targetRoutingOffsetX;
    }

    public void setTargetX(int targetX) {
        this.targetX = targetX;
    }

    public int getTargetY() {
        return targetY;
    }

    public void setTargetY(int targetY) {
        this.targetY = targetY;
    }

    public int getIntermediateY() {
        return intermediateY;
    }

    public void setIntermediateY(int intermediateY) {
        this.intermediateY = intermediateY;
    }

    public int getSourceRoutingOffsetX() {
        return sourceRoutingOffsetX;
    }

    public void setSourceRoutingOffsetX(int offsetX) {
        this.sourceRoutingOffsetX = offsetX;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}
