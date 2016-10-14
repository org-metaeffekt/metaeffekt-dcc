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
package org.metaeffekt.dcc.docgenerator;

import java.util.List;

import org.metaeffekt.dcc.commons.mapping.Binding;
import org.metaeffekt.dcc.commons.mapping.ConfigurationUnit;
import org.metaeffekt.dcc.commons.mapping.Profile;
import org.metaeffekt.dcc.docgenerator.Forces.ForceType;

/**
 * A node is used to perform automatic layouting. The class adds x and y coordinates to a 
 * configuration unit and encapsulates layouting-related functions. It's important to node that
 * x and y are aligned to a grid.
 * 
 * @author Karsten Klein
 */
public class Node {

    private final ConfigurationUnit unit;

    private double x;
    private double y;
    
    private boolean fixed = false;
    
    private boolean visible = true;
    
    public Node(ConfigurationUnit unit, double x, double y) {
        this.unit = unit;
        this.x = x;
        this.y = y;
    }

    public void applyForces(Profile profile, List<Node> nodes) {
        if (!fixed) {
            Forces forces = new Forces();
            for (Node node : nodes) {
                if (node != this) {
                    computeForces(profile, node, nodes, forces);
                }
            }
            applyForceVector(forces.computeForceVector());
        }
    }
    
    public void applyForceVector(ForceVector forceVector) {
        if (!fixed) {
            final double[] forces = forceVector.getForces();
            double max = 100;
            for (int j = 0; j < forces.length; j++) {
                if (forces[j] > max) forces[j] = max;
                if (forces[j] < -max) forces[j] = -max;
            }
            this.x += forces[0];
//            this.y += forces[1];
        }
    }

    public void computeForces(Profile profile, Node node, List<Node> nodes, Forces forces) {
        // NOTE-KKL forces are currently aggregated and weighted. However no forces are added that
        //  want to keep the node in it's current position. Adding this aspect may have positive
        //  effects on the layout. Just an idea that should not be forgotten.
        
        if (getX() == node.getX() && getY() == node.getY()) {
            ForceVector forceVector = new ForceVector();
            moveToEmpty(nodes, forceVector, 1.0, false, false);
            forces.addForce(ForceType.REPULSION, forceVector);
            return;
        }
        
        // check whether a binding between the two units exist
        ConfigurationUnit thisUnit = this.getUnit();
        ConfigurationUnit otherUnit = node.getUnit();
        boolean hasBinding = false;
        final List<Binding> bindings = profile.getBindings();
        for (Binding binding : bindings) {
            // compute distance in 2d grid
            double dx = node.getX() - getX();
            double dy = node.getY() - getY();
            
            if (isDirectedBinding(binding, thisUnit, otherUnit)) {
                ForceVector forceVector = new ForceVector();
                forceVector.getForces()[0] += normalize(dx);
                forceVector.getForces()[1] += normalize(dy);
                forces.addForce(ForceType.OUT_BINDING, forceVector);
                
                ForceVector offsetForceVector = new ForceVector();
                // we try to get dy to -1
                if (dy < -1) {
                    offsetForceVector.getForces()[1] = 1;
                }
                if (dy > -1) {
                    offsetForceVector.getForces()[1] = -1;
                }
                forces.addForce(ForceType.OUT_BINDING_OFFSET, offsetForceVector);
                hasBinding = true;
            }
            if (isDirectedBinding(binding, otherUnit, thisUnit)) {
                ForceVector forceVector = new ForceVector();
                forceVector.getForces()[0] += normalize(dx);
                forceVector.getForces()[1] += normalize(dy);
                forces.addForce(ForceType.IN_BINDING, forceVector);

                ForceVector offsetForceVector = new ForceVector();
                // we try to get dy to 1
                if (dy < 1) {
                    offsetForceVector.getForces()[1] = 1;
                }
                if (dy > 1) {
                    offsetForceVector.getForces()[1] = -1;
                }
                forces.addForce(ForceType.IN_BINDING_OFFSET, offsetForceVector);
                hasBinding = true;
            }
            if (hasBinding) {
                return;
            }
        }
        if (!hasBinding) {
            ForceVector forceVector = new ForceVector();
            moveToEmpty(nodes, forceVector, 1.0, false, false);
            forces.addForce(ForceType.BINDING_REPULSION, forceVector);
            return;
        }

        ForceVector forceVector = new ForceVector();
        moveToEmpty(nodes, forceVector, 1.0, true, true);
        forces.addForce(ForceType.SUCTION, forceVector);
    }

    public double normalize(double d) {
        // NOTE-KKL we use the full force (which may be too forceful)
//        if (d > 0) return 1;
//        if (d < 0) return -1;
        return d / 1.5;
    }

    public void moveToEmpty(List<Node> nodes, ForceVector forceVector, double weight, boolean contrained, boolean align) {
        if (fixed) return;
        boolean aboveOccupied = false;
        boolean leftOccupied = false;
        boolean rightOccupied = false;
        boolean bellowOccupied = false;
        
        for (Node n : nodes) {
            if (n != this) {
                double  dx = n.getX() - getX();
                double  dy = n.getY() - getY();
                
                if (dx == -1 && dy == 0) { 
                    leftOccupied = true;
                }
                if (dx == 1 && dy == 0) { 
                    rightOccupied = true;
                }
                if (dx == 0 && dy == -1) { 
                    aboveOccupied = true;
                }
                if (dx == 0 && dy == 1) { 
                    bellowOccupied = true;
                }
            }
        }
        
        boolean moved = false;
        if (!aboveOccupied && (!contrained || getY() > 0)) {
            forceVector.getForces()[1] -= weight;
            moved = true;
        }
        if (!moved && !leftOccupied && (!contrained || getX() > 0)) {
            forceVector.getForces()[0] -= weight;
            moved = true;
        }
        if (!align) {
            if (!contrained) {
                if (!moved && !bellowOccupied) {
                    forceVector.getForces()[1] += weight;
                    moved = true;
                }
                if (!moved && !rightOccupied) {
                    forceVector.getForces()[0] += weight;
                    moved = true;
                }
            }
            
//            // could not move anywhere --> jump out
//            if (!moved) {
//                forceVector.getForces()[1] -= weight;
//            }
        }
    }
    
    public void moveToEmpty(List<Node> nodes) {
        if (fixed) return;
        boolean collision = false;
        for (Node n : nodes) {
            if (n != this) {
                double  dx = n.getX() - getX();
                double  dy = n.getY() - getY();
                
                if (dx == 0 && dy == 0) { 
                    collision = true;
                }
            }
        }
        
        if (collision) {
            double maxX = this.x;
            double maxY = this.y;
            
            for (Node n : nodes) {
                if (n != this) {
                    double  dy = n.getY() - getY();
                    if (dy == 0) { 
                        maxX = Math.max(maxX, n.getX());
                    }
                    double  dx = n.getX() - getX();
                    if (dx == 0) { 
                        maxY = Math.max(maxY, n.getY());
                    }
                }
            }

            if (maxY - getY() < maxX - getX()) {
                this.y = maxY + 1.0;
            } else {
                this.x = maxX + 1.0;
            }
        }
    }

    public boolean isDirectedBinding(Binding binding, ConfigurationUnit sourceUnit,
            ConfigurationUnit targetUnit) {
        if (binding.getSourceCapability().getUnit() == sourceUnit &&
            binding.getTargetCapability().getUnit() == targetUnit) {
                return true;
        }
        return false;
    }
    
    public ConfigurationUnit getUnit() {
        return unit;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getXOnGrid() {
        return (int) Math.round(this.x);
    }

    public int getYOnGrid() {
        return (int) Math.round(this.y);
    }

    public void add(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    public void normalizeToGrid() {
        this.x = getXOnGrid();
        this.y = getYOnGrid();
    }

    public boolean isFixed() {
        return fixed;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}
