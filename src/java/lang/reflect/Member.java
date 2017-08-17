/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.reflect;


public
interface Member {


    public static final int PUBLIC = 0;


    public static final int DECLARED = 1;


    public Class<?> getDeclaringClass();


    public String getName();


    public int getModifiers();


    public boolean isSynthetic();
}
