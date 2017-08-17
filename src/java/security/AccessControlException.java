/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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



public class AccessControlException extends SecurityException {

    private static final long serialVersionUID = 5138225684096988535L;

    // the permission that caused the exception to be thrown.
    private Permission perm;


    public AccessControlException(String s) {
        super(s);
    }


    public AccessControlException(String s, Permission p) {
        super(s);
        perm = p;
    }


    public Permission getPermission() {
        return perm;
    }
}
