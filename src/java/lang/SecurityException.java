/*
 * Copyright (c) 1995, 2003, Oracle and/or its affiliates. All rights reserved.
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


public class SecurityException extends RuntimeException {

    private static final long serialVersionUID = 6878364983674394167L;


    public SecurityException() {
        super();
    }


    public SecurityException(String s) {
        super(s);
    }


    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }


    public SecurityException(Throwable cause) {
        super(cause);
    }
}
