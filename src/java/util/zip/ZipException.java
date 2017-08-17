/*
 * Copyright (c) 1995, 2010, Oracle and/or its affiliates. All rights reserved.
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

package java.util.zip;

import java.io.IOException;



public
class ZipException extends IOException {
    private static final long serialVersionUID = 8000196834066748623L;


    public ZipException() {
        super();
    }



    public ZipException(String s) {
        super(s);
    }
}
