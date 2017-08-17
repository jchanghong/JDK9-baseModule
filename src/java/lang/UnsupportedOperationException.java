/*
 * Copyright (c) 1997, 2006, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;


public class UnsupportedOperationException extends RuntimeException {

    public UnsupportedOperationException() {
    }


    public UnsupportedOperationException(String message) {
        super(message);
    }


    public UnsupportedOperationException(String message, Throwable cause) {
        super(message, cause);
    }


    public UnsupportedOperationException(Throwable cause) {
        super(cause);
    }

    static final long serialVersionUID = -1242599979055084673L;
}
