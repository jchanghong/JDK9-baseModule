/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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


public class IllegalCallerException extends RuntimeException {

    public IllegalCallerException() {
        super();
    }


    public IllegalCallerException(String s) {
        super(s);
    }


    public IllegalCallerException(String message, Throwable cause) {
        super(message, cause);
    }


    public IllegalCallerException(Throwable cause) {
        super(cause);
    }

    static final long serialVersionUID = -2349421918363102232L;
}
