/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.security.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;



public final class SecurityPermission extends BasicPermission {

    private static final long serialVersionUID = 5236109936224050470L;


    public SecurityPermission(String name)
    {
        super(name);
    }


    public SecurityPermission(String name, String actions)
    {
        super(name, actions);
    }
}
