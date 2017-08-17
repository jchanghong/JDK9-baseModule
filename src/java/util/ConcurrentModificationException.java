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

package java.util;


public class ConcurrentModificationException extends RuntimeException {
    private static final long serialVersionUID = -3666751008965953603L;


    public ConcurrentModificationException() {
    }


    public ConcurrentModificationException(String message) {
        super(message);
    }


    public ConcurrentModificationException(Throwable cause) {
        super(cause);
    }


    public ConcurrentModificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
