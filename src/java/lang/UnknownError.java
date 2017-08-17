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

package java.lang;


public
class UnknownError extends VirtualMachineError {
    private static final long serialVersionUID = 2524784860676771849L;


    public UnknownError() {
        super();
    }


    public UnknownError(String s) {
        super(s);
    }
}
