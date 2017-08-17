/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;


public
class SocketException extends IOException {
    private static final long serialVersionUID = -5935874303556886934L;


    public SocketException(String msg) {
        super(msg);
    }


    public SocketException() {
    }
}
