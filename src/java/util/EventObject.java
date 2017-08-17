/*
 * Copyright (c) 1996, 2003, Oracle and/or its affiliates. All rights reserved.
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

package java.util;



public class EventObject implements java.io.Serializable {

    private static final long serialVersionUID = 5516075349620653480L;


    protected transient Object  source;


    public EventObject(Object source) {
        if (source == null)
            throw new IllegalArgumentException("null source");

        this.source = source;
    }


    public Object getSource() {
        return source;
    }


    public String toString() {
        return getClass().getName() + "[source=" + source + "]";
    }
}
