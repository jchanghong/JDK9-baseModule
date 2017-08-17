/*
 * Copyright (c) 1994, 2012, Oracle and/or its affiliates. All rights reserved.
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


public
class IllegalArgumentException extends RuntimeException {

    public IllegalArgumentException() {
        super();
    }


    public IllegalArgumentException(String s) {
        super(s);
    }


    public IllegalArgumentException(String message, Throwable cause) {
        super(message, cause);
    }


    public IllegalArgumentException(Throwable cause) {
        super(cause);
    }

    private static final long serialVersionUID = -5365630128856068164L;
}
