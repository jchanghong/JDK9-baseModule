/*
 * Copyright (c) 1999, 2006, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.reflect;


public class UndeclaredThrowableException extends RuntimeException {
    static final long serialVersionUID = 330127114055056639L;


    private Throwable undeclaredThrowable;


    public UndeclaredThrowableException(Throwable undeclaredThrowable) {
        super((Throwable) null);  // Disallow initCause
        this.undeclaredThrowable = undeclaredThrowable;
    }


    public UndeclaredThrowableException(Throwable undeclaredThrowable,
                                        String s)
    {
        super(s, null);  // Disallow initCause
        this.undeclaredThrowable = undeclaredThrowable;
    }


    public Throwable getUndeclaredThrowable() {
        return undeclaredThrowable;
    }


    public Throwable getCause() {
        return undeclaredThrowable;
    }
}
