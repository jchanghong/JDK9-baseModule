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


public class UnknownServiceException extends IOException {
    private static final long serialVersionUID = -4169033248853639508L;


    public UnknownServiceException() {
    }


    public UnknownServiceException(String msg) {
        super(msg);
    }
}
