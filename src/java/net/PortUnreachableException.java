/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.net;



public class PortUnreachableException extends SocketException {
    private static final long serialVersionUID = 8462541992376507323L;


    public PortUnreachableException(String msg) {
        super(msg);
    }


    public PortUnreachableException() {}
}
