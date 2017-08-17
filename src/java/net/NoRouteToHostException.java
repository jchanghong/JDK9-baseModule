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


public class NoRouteToHostException extends SocketException {
    private static final long serialVersionUID = -1897550894873493790L;


    public NoRouteToHostException(String msg) {
        super(msg);
    }


    public NoRouteToHostException() {}
}
