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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.metaeffekt.dcc.commons.mapping.Binding;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.commons.mapping.PropertiesHolder;


public class ProfileLayout {

    public static final int CAPABILITY_WIDTH = 220;
    public static final int GRID_COLUMN_WIDTH = 2 * CAPABILITY_WIDTH + 70;
    public static final int CAPABILITY_HEIGHT = 32;
    public static final int CAPABILITY_DISTANCE = 3;

    private static final int MINIMUM_LEVEL_HEIGHT = 0;

    private Profile profile;
    
    private DocGenerator docGenerator;
    
    private int maxNumberOfUnitsInLevels = 0;
    private int maxNumberOfLevels = 0;
    
    private Map<Integer, List<Node>> unitGrid = 
        new HashMap<>();

    private Map<Integer, Integer> rowHeights = new HashMap<>();

    private List<Node> nodes = new ArrayList<>();
    private Map<ConfigurationUnit, Node> unitNodeMap = new HashMap<>();
    
    public ProfileLayout(DocGenerator docGenerator, Profile profile) {
        this.docGenerator = docGenerator;
        this.profile = profile;

        List<ConfigurationUnit> units = profile.getUnits(false);
        if (units == null || units.isEmpty()) {
            return;
        }
        
        PropertiesHolder propertiesHolder = profile.createPropertiesHolder(true);

        // layout pre-processing. distributes the units to different lanes / levels, according to
        // name categories
//        String[] categories = new String[] { "host", "runtime", "instance", "war", "webapp"};
        String[] categories = new String[] { "host", "runtime", "instance", "contribution", "war", "bundle", "import", "bootstrap", "config", "configuration", "webapp", "schema" };
       
        int[] offsets = new int[categories.length];
        int laneIndex = 0;
        for (int i = 0; i < categories.length; i++) {
            boolean nodeInLane = false;
            for (Iterator<ConfigurationUnit> iterator = units.iterator(); iterator.hasNext();) {
                ConfigurationUnit unit = iterator.next();
                if (isInCategory(unit, categories[i])) {
                    String x = propertiesHolder.getBaseProperty(unit.getId() + ".layout.x", null);
                    String y = propertiesHolder.getBaseProperty(unit.getId() + ".layout.y", null);

                    boolean fixed = false;

                    if (y == null) {
                        y = String.valueOf(laneIndex);
                    } else {
                        fixed = true;
                    }

                    if (x == null) {
                        x = String.valueOf(offsets[laneIndex]++);
                    } else {
                        fixed = true;
                    }
                    
                    Node node = new Node(unit, Integer.parseInt(x), Integer.parseInt(y));
                    node.setFixed(fixed);
                    unitNodeMap.put(unit, node);
                    nodes.add(node);
                    iterator.remove();
                    nodeInLane = true;
                }
            }
            if (nodeInLane) {
                laneIndex++;
            }
        }
        
        // distribute remaining (uncategorized units) to an additional lane
        int offset = 0;
        for (ConfigurationUnit unit : units) {
            Node node = unitNodeMap.get(unit);
            if (node != null && node.isFixed()) {
                continue;
            }
            
            String x = propertiesHolder.getBaseProperty(unit.getId() + ".layout.x", null);
            String y = propertiesHolder.getBaseProperty(unit.getId() + ".layout.y", null);

            boolean fixed = false;
            if (x == null) {
                x = String.valueOf(offset++);
            } else {
                fixed = true;
            }
            if (y == null) {
                y = String.valueOf(laneIndex);
            } else {
                fixed = true;
            }
            
            node = new Node(unit, Integer.parseInt(x), Integer.parseInt(y));
            node.setFixed(fixed);
            unitNodeMap.put(unit, node);
            nodes.add(node);
        }
        
//        for (ConfigurationUnit unit : units) {
//            Node node = unitNodeMap.get(unit);
//            if (node != null) {
//                if (!node.isFixed()) {
//                    node.setVisible(false);
//                    nodes.remove(node);
//                }
//            }
//        }
        
        int minX = 0, maxX = 0;
        int minY = 0, maxY = 0;
        
        int iterations = 20;
        
        for (int i = 0; i < iterations; i++) {
            
            // sequentially apply forces to the current node
            for (Node node : nodes) {
                node.applyForces(profile, nodes);
            }
            
            // after all nodes have been processed in an iteration these are normalized to the grid
            for (Node node : nodes) {
                node.normalizeToGrid();
            }
            
            // move in grid such that topmost at y=0, leftmost at x=0
            minX = maxX = nodes.get(0).getXOnGrid();
            minY = maxY = nodes.get(0).getYOnGrid();
            for (Node node : nodes) {
                minX = Math.min(minX, node.getXOnGrid());
                maxX = Math.max(maxX, node.getXOnGrid());
                minY = Math.min(minY, node.getYOnGrid());
                maxY = Math.max(maxY, node.getYOnGrid());
            }
            for (Node node : nodes) {
                node.add(-minX, -minY);
            }
            maxX -= minX;
            maxY -= minY;
        }

        avoidCollisions();
    
        // move in grid such that topmost at y=0, leftmost at x=0
        minX = maxX = nodes.get(0).getXOnGrid();
        minY = maxY = nodes.get(0).getYOnGrid();
        for (Node node : nodes) {
            minX = Math.min(minX, node.getXOnGrid());
            maxX = Math.max(maxX, node.getXOnGrid());
            minY = Math.min(minY, node.getYOnGrid());
            maxY = Math.max(maxY, node.getYOnGrid());
        }
        for (Node node : nodes) {
            node.add(-minX, -minY);
        }
        maxX -= minX;
        maxY -= minY;
        
        for (Node node : nodes) {
            List<Node> unitsInLevel = unitGrid.get(node.getYOnGrid());
            if (unitsInLevel == null) {
                unitsInLevel = new ArrayList<>();
                unitGrid.put(node.getYOnGrid(), unitsInLevel);
            }
            unitsInLevel.add(node);
        }
        
        // determine max width and height for grid
        for (int i = 0; i < unitGrid.size(); i++) {
            int max = MINIMUM_LEVEL_HEIGHT;
            final List<Node> nodes = unitGrid.get(i);
            if (nodes != null) {
                for (Node node: nodes) {
                    max = Math.max(max, computeHeight(node.getUnit()));
                }
            } 
            rowHeights.put(i, max);
        }
        maxNumberOfLevels = maxY + 1;
        maxNumberOfUnitsInLevels = maxX + 1;
    }

    public void avoidCollisions() {
        for (Node node : nodes) {
            node.moveToEmpty(nodes);
        }
    }
    
    private int computeHeight(ConfigurationUnit unit) {
        return (CAPABILITY_HEIGHT + CAPABILITY_DISTANCE) * docGenerator.computeHeight(unit) + 150;
    }

    public int computeOffsetX(ConfigurationUnit unit) {
        Integer level = determineLevelForUnit(unit);
        if (level == null) return 0;
        List<Node> unitsInLevel = unitGrid.get(level);

        if (unitsInLevel == null) {
            return 0;
        }
        
        int positionInLevel = unitNodeMap.get(unit).getXOnGrid();
        
        int offset = 0;
        for (int i = 0; i < positionInLevel; i++) {
            offset += GRID_COLUMN_WIDTH;
        }
        return offset;
    }
    
    public int computeOffsetXOnGrid(ConfigurationUnit unit) {
        return unitNodeMap.get(unit).getXOnGrid();
    }

    public int computeOffsetYOnGrid(ConfigurationUnit unit) {
        return unitNodeMap.get(unit).getYOnGrid();
    }

    public int computeOffsetY(ConfigurationUnit unit) {
        Integer level = determineLevelForUnit(unit);
        return getHeightForLevel(level);
    }

    public int getHeightForLevel(Integer level) {
        if (level == null) return 0;
        
        int offset = 0;
        for (int i = 0; i < level; i++) {
            if (rowHeights.get(i) != null) {
                offset += rowHeights.get(i);
            } else {
                offset += 600;
            }
        }
        return offset;
    }

    private Integer determineLevelForUnit(ConfigurationUnit unit) {
        Node node = unitNodeMap.get(unit);
        return node.getYOnGrid();
    }

    public boolean isInCategory(ConfigurationUnit unit, final String category) {
        String unitIdString = unit.getId().getValue();
        return unitIdString.startsWith(category+"-") ||
                unitIdString.endsWith("-"+category);
    }
    
    public BindingLayout computeBindingLayout(Binding binding) {
        return new BindingLayout(this, binding, unitNodeMap);
    }

    public Profile getProfile() {
        return profile;
    }
    
    public int getWidth() {
        return maxNumberOfUnitsInLevels * GRID_COLUMN_WIDTH + 100;
    }
    
    public int getHeight() {
        return getHeightForLevel(maxNumberOfLevels + 1) + 400;
    }
    
    public boolean isVisible(ConfigurationUnit unit) {
        Node node = unitNodeMap.get(unit);
        return node != null && node.isVisible(); 
    }
    
}
