/*
 * Copyright (c) 1995, 2010, Oracle and/or its affiliates. All rights reserved.
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
class LinkageError extends Error {
    private static final long serialVersionUID = 3579600108157160122L;


    public LinkageError() {
        super();
    }


    public LinkageError(String s) {
        super(s);
    }


    public LinkageError(String s, Throwable cause) {
        super(s, cause);
    }
}
