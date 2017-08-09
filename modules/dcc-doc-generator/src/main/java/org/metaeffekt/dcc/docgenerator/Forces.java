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
import java.util.Arrays;
import java.util.List;


/**
 * Collects forces for different {@link ForceType}s. Each force type can be weighted independently 
 * to compose a aggregated {@link ForceVector}.
 * 
 * @author Karsten Klein
 */
public class Forces {
    
    private static final int FORCE_COUNT = 7;

    enum ForceType {
        REPULSION (0),
        IN_BINDING (1),
        IN_BINDING_OFFSET (2),
        OUT_BINDING (3),
        OUT_BINDING_OFFSET (4),
        BINDING_REPULSION (5),
        SUCTION (6);
        
        private final int index;
        private ForceType(int index) {
            this.index = index;
        }
        
        public int getIndex() {
            return index;
        }
    }
    
    private Object[] forces = new Object[FORCE_COUNT];
    private double[] forceWeights = new double[FORCE_COUNT];
    
    public Forces() {
        Arrays.fill(forceWeights, 0.0d);
        forceWeights[ForceType.REPULSION.getIndex()] = 0.1;
        forceWeights[ForceType.IN_BINDING.getIndex()] = 0.5;
        forceWeights[ForceType.IN_BINDING_OFFSET.getIndex()] = 0.7;
        forceWeights[ForceType.OUT_BINDING.getIndex()] = 0.5;
        forceWeights[ForceType.OUT_BINDING_OFFSET.getIndex()] = 0.7;
        forceWeights[ForceType.BINDING_REPULSION.getIndex()] = 0.2;
        forceWeights[ForceType.SUCTION.getIndex()] = 0.3;
        for (int i = 0; i < FORCE_COUNT; i++) {
            forces[i] = new ArrayList<ForceType>();
        }
    }
    
    public Forces[] getForces() {
        return (Forces[]) forces;
    }

    public void setForces(Forces[] forces) {
        this.forces = forces;
    }

    public double[] getForceWeights() {
        return forceWeights;
    }

    public void setForceWeights(double[] forceWeights) {
        this.forceWeights = forceWeights;
    }

    /**
     * Add a force ({@link ForceVector}) for a given {@link ForceType}.
     * 
     * @param forceType The force type.
     * @param forceVector The force vector.
     */
    public void addForce(ForceType forceType, ForceVector forceVector) {
        getForceList(forceType).add(forceVector);
    }

    @SuppressWarnings("unchecked")
    public ArrayList<ForceVector> getForceList(ForceType forceType) {
        return (ArrayList<ForceVector>) forces[forceType.getIndex()];
    }
    
    @SuppressWarnings("unchecked")
    public ArrayList<ForceVector> getForceList(int forceType) {
        return (ArrayList<ForceVector>) forces[forceType];
    }

    /**
     * Compute the aggregate, weighted {@link ForceVector}.
     *
     * @return
     */
    public ForceVector computeForceVector() {
        final ForceVector forceVector = new ForceVector();

        for (int i = 0; i < FORCE_COUNT; i++) {
            final ForceVector typedForceVector = new ForceVector();
            
            // aggregate forces
            List<ForceVector> forceList = getForceList(i);
            for (ForceVector v : forceList) {
                typedForceVector.add(v);
            }
            
            if (forceList.size() > 0) {
                typedForceVector.scale(forceWeights[i] / forceList.size());
                forceVector.add(typedForceVector);
            }
        }

        return forceVector;
    }

}
