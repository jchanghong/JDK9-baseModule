/*
 * Copyright (c) 1994, 2015, Oracle and/or its affiliates. All rights reserved.
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


public class StringIndexOutOfBoundsException extends IndexOutOfBoundsException {
    private static final long serialVersionUID = -6762910422159637258L;


    public StringIndexOutOfBoundsException() {
        super();
    }


    public StringIndexOutOfBoundsException(String s) {
        super(s);
    }


    public StringIndexOutOfBoundsException(int index) {
        super("String index out of range: " + index);
    }
}
