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


public class MalformedURLException extends IOException {
    private static final long serialVersionUID = -182787522200415866L;


    public MalformedURLException() {
    }


    public MalformedURLException(String msg) {
        super(msg);
    }
}
