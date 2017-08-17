/*
 * Copyright (c) 1996, 2005, Oracle and/or its affiliates. All rights reserved.
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

package java.io;


public class NotSerializableException extends ObjectStreamException {

    private static final long serialVersionUID = 2906642554793891381L;


    public NotSerializableException(String classname) {
        super(classname);
    }


    public NotSerializableException() {
        super();
    }
}
