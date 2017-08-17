/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.security.interfaces;

import java.math.BigInteger;



public interface RSAPublicKey extends java.security.PublicKey, RSAKey
{

    static final long serialVersionUID = -8727434096241101194L;


    public BigInteger getPublicExponent();
}
