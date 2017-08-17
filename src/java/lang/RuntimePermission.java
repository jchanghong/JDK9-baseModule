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

package java.lang;

import java.security.*;
import java.lang.module.ModuleFinder;



public final class RuntimePermission extends BasicPermission {

    private static final long serialVersionUID = 7399184964622342223L;



    public RuntimePermission(String name)
    {
        super(name);
    }



    public RuntimePermission(String name, String actions)
    {
        super(name, actions);
    }
}
