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

package java.io;


public class SyncFailedException extends IOException {
    private static final long serialVersionUID = -2353342684412443330L;


    public SyncFailedException(String desc) {
        super(desc);
    }
}
