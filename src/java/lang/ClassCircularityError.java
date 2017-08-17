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


public class ClassCircularityError extends LinkageError {
    private static final long serialVersionUID = 1054362542914539689L;


    public ClassCircularityError() {
        super();
    }


    public ClassCircularityError(String s) {
        super(s);
    }
}
