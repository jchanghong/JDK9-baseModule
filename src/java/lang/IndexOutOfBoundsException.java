/*
 * Copyright (c) 1995, 2015, Oracle and/or its affiliates. All rights reserved.
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


public class IndexOutOfBoundsException extends RuntimeException {
    private static final long serialVersionUID = 234122996006267687L;


    public IndexOutOfBoundsException() {
        super();
    }


    public IndexOutOfBoundsException(String s) {
        super(s);
    }


    public IndexOutOfBoundsException(int index) {
        super("Index out of range: " + index);
    }
}
