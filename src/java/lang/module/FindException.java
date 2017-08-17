/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.module;



public class FindException extends RuntimeException {
    private static final long serialVersionUID = -5817081036963388391L;


    public FindException() {
    }


    public FindException(String msg) {
        super(msg);
    }


    public FindException(Throwable cause) {
        super(cause);
    }


    public FindException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
