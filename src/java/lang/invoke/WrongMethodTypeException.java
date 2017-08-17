/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;


public class WrongMethodTypeException extends RuntimeException {
    private static final long serialVersionUID = 292L;


    public WrongMethodTypeException() {
        super();
    }


    public WrongMethodTypeException(String s) {
        super(s);
    }


    //FIXME: make this public in MR1
    /*non-public*/ WrongMethodTypeException(String s, Throwable cause) {
        super(s, cause);
    }


    //FIXME: make this public in MR1
    /*non-public*/ WrongMethodTypeException(Throwable cause) {
        super(cause);
    }
}
