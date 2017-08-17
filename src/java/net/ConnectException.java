/*
 * Copyright (c) 1996, 2008, Oracle and/or its affiliates. All rights reserved.
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


public class ConnectException extends SocketException {
    private static final long serialVersionUID = 3831404271622369215L;


    public ConnectException(String msg) {
        super(msg);
    }


    public ConnectException() {}
}
