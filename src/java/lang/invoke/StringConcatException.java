/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 */

package java.lang.invoke;


public class StringConcatException extends Exception {
    private static final long serialVersionUID = 292L + 9L;


    public StringConcatException(String msg) {
        super(msg);
    }


    public StringConcatException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
