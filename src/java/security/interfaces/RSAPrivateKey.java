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



public interface RSAPrivateKey extends java.security.PrivateKey, RSAKey
{


    static final long serialVersionUID = 5187144804936595022L;


    public BigInteger getPrivateExponent();
}
