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


/**
 * Simple vector implementation supporting multiple dimensions.
 * 
 * @author Karsten Klein
 */
public class ForceVector {
    
    private double[] forces = new double[] {0, 0};

    public double[] getForces() {
        return forces;
    }

    public void add(ForceVector forceVector) {
        for (int i = 0; i < forces.length; i++) {
            forces[i] = forces[i] + forceVector.getForces()[i];
        }
    }
    
    public ForceVector scale(double scale) {
        for (int i = 0; i < forces.length; i++) {
            forces[i] *= scale;
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < forces.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(forces[i]);
        }
        sb.append("]");
        return sb.toString();
    }

}
