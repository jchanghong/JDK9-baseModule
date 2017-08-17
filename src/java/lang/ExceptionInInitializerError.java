/*
 * Copyright (c) 1996, 2000, Oracle and/or its affiliates. All rights reserved.
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


public class ExceptionInInitializerError extends LinkageError {

    private static final long serialVersionUID = 1521711792217232256L;


    private Throwable exception;


    public ExceptionInInitializerError() {
        initCause(null);  // Disallow subsequent initCause
    }


    public ExceptionInInitializerError(Throwable thrown) {
        initCause(null);  // Disallow subsequent initCause
        this.exception = thrown;
    }


    public ExceptionInInitializerError(String s) {
        super(s);
        initCause(null);  // Disallow subsequent initCause
    }


    public Throwable getException() {
        return exception;
    }


    public Throwable getCause() {
        return exception;
    }
}
