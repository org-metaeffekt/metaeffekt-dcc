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
package org.metaeffekt.dcc.commons.ant;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;


public class GeneratePasswordTask extends Task {
    private boolean numbers = true;
    private boolean alphabets = true;
    private String characters;
    private int length = 20;
    
    private String property;
    
    @Override
    public void execute() throws BuildException {
        String propertyValue = getProject().getProperty(property);
        
        if (propertyValue == null) {
            String password;
            if (characters != null) {
                password =
                        RandomStringUtils.random(length, 0, characters.length(), alphabets, numbers,
                                characters.toCharArray());
            } else {
                password = RandomStringUtils.random(length, alphabets, numbers);
            }
            
            getProject().setProperty(property, password);
        }

    }

    public boolean isNumbers() {
        return numbers;
    }

    public void setNumbers(boolean numbers) {
        this.numbers = numbers;
    }

    public boolean isAlphabets() {
        return alphabets;
    }

    public void setAlphabets(boolean alphabets) {
        this.alphabets = alphabets;
    }

    public String getCharacters() {
        return characters;
    }

    public void setCharacters(String characters) {
        this.characters = characters;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }
}
