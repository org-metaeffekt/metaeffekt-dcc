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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;

/**
 * Condition that allows to compare two objects of the same type.
 * 
 * @author Karsten Klein
 */
public class CompareCondition extends ConditionBase implements Condition {

    private static final String MODE_LESSTHAN = "lessthan";
    private static final String MODE_GREATERTHAN = "greaterthan";
    private static final String MODE_EQUALS = "equals";
    
    private static final String TYPE_INTEGER = "integer";
    private static final String TYPE_LONG = "long";
    private static final String TYPE_STRING = "long";

    private Object arg1;
    private Object arg2;

    private String type;

    private String mode;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public boolean eval() throws BuildException {
        Comparable convertedArg1;
        Comparable convertedArg2;
        if (TYPE_INTEGER.equalsIgnoreCase(type) || TYPE_LONG.equalsIgnoreCase(type)) {
            convertedArg1 = convertInteger(arg1);
            convertedArg2 = convertInteger(arg2);
        } else if (TYPE_STRING.equalsIgnoreCase(type)) {
            convertedArg1 = String.valueOf(arg1);
            convertedArg2 = String.valueOf(arg2);
        } else {
            throw new BuildException("Type " + type + " not supported. Supported types: "
                    + "'" + TYPE_INTEGER + "', "
                    + "'" + TYPE_LONG + "', "
                    + "'" + TYPE_STRING + "'.");
        }

        if (MODE_LESSTHAN.equalsIgnoreCase(mode)) {
            return convertedArg1.compareTo(convertedArg2) < 0;
        } else if (MODE_GREATERTHAN.equalsIgnoreCase(mode)) {
            return convertedArg1.compareTo(convertedArg2) > 0;
        } else if (MODE_EQUALS.equalsIgnoreCase(mode)) {
            return convertedArg1.compareTo(convertedArg2) == 0;
        } else {
            throw new BuildException("Mode " + mode + " not supported. Supported modes: "
                    + "'" + MODE_LESSTHAN + "', "
                    + "'" + MODE_GREATERTHAN + "', "
                    + "'" + MODE_EQUALS + "'.");
        }

    }

    private Long convertInteger(Object arg) {
        Long value;
        if (arg instanceof Integer) {
            value = ((Integer) arg).longValue();
        } else if (arg instanceof Long) {
            value = (Long) arg;
        } else if (arg instanceof String) {
            value = Long.parseLong((String) arg);
        } else {
            throw new BuildException("Cannot convert " + arg.getClass() + " to integer.");
        }
        return value;
    }

    public Object getArg1() {
        return arg1;
    }

    public void setArg1(Object arg1) {
        this.arg1 = arg1;
    }

    public Object getArg2() {
        return arg2;
    }

    public void setArg2(Object arg2) {
        this.arg2 = arg2;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

}
