/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.text;



public class Annotation {


    public Annotation(Object value) {
        this.value = value;
    }


    public Object getValue() {
        return value;
    }


    public String toString() {
        return getClass().getName() + "[value=" + value + "]";
    }

    private Object value;

};
