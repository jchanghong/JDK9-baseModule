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


public abstract class ObjectStreamException extends IOException {

    private static final long serialVersionUID = 7260898174833392607L;


    protected ObjectStreamException(String message) {
        super(message);
    }


    protected ObjectStreamException() {
        super();
    }
}
