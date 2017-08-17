/*
 * Copyright (c) 1997, 2005, Oracle and/or its affiliates. All rights reserved.
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

package java.security;



public class UnrecoverableKeyException extends UnrecoverableEntryException {

    private static final long serialVersionUID = 7275063078190151277L;


    public UnrecoverableKeyException() {
        super();
    }


   public UnrecoverableKeyException(String msg) {
       super(msg);
    }
}
