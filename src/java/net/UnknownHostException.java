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
class UnknownHostException extends IOException {
    private static final long serialVersionUID = -4639126076052875403L;


    public UnknownHostException(String message) {
        super(message);
    }


    public UnknownHostException() {
    }
}
