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



public interface RSAPrivateCrtKey extends RSAPrivateKey {


    static final long serialVersionUID = -5682214253527700368L;


    public BigInteger getPublicExponent();


    public BigInteger getPrimeP();


    public BigInteger getPrimeQ();


    public BigInteger getPrimeExponentP();


    public BigInteger getPrimeExponentQ();


    public BigInteger getCrtCoefficient();
}
