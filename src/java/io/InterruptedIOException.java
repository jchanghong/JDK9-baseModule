/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
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

package java.io;


public
class InterruptedIOException extends IOException {
    private static final long serialVersionUID = 4020568460727500567L;


    public InterruptedIOException() {
        super();
    }


    public InterruptedIOException(String s) {
        super(s);
    }


    public int bytesTransferred = 0;
}
