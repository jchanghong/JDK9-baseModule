/*
 * Copyright (c) 1995, 2004, Oracle and/or its affiliates. All rights reserved.
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


public class ClassNotFoundException extends ReflectiveOperationException {

     private static final long serialVersionUID = 9176873029745254542L;


    private Throwable ex;


    public ClassNotFoundException() {
        super((Throwable)null);  // Disallow initCause
    }


    public ClassNotFoundException(String s) {
        super(s, null);  //  Disallow initCause
    }


    public ClassNotFoundException(String s, Throwable ex) {
        super(s, null);  //  Disallow initCause
        this.ex = ex;
    }


    public Throwable getException() {
        return ex;
    }


    public Throwable getCause() {
        return ex;
    }
}
