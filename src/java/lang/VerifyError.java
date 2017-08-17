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
class VerifyError extends LinkageError {
    private static final long serialVersionUID = 7001962396098498785L;


    public VerifyError() {
        super();
    }


    public VerifyError(String s) {
        super(s);
    }
}
