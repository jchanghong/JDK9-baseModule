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

package java.net;

import java.security.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;



public final class NetPermission extends BasicPermission {
    private static final long serialVersionUID = -8343910153355041693L;



    public NetPermission(String name)
    {
        super(name);
    }



    public NetPermission(String name, String actions)
    {
        super(name, actions);
    }
}
