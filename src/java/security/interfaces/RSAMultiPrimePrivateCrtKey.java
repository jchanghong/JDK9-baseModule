/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.security.spec.RSAOtherPrimeInfo;



public interface RSAMultiPrimePrivateCrtKey extends RSAPrivateKey {


    static final long serialVersionUID = 618058533534628008L;


    public BigInteger getPublicExponent();


    public BigInteger getPrimeP();


    public BigInteger getPrimeQ();


    public BigInteger getPrimeExponentP();


    public BigInteger getPrimeExponentQ();


    public BigInteger getCrtCoefficient();


    public RSAOtherPrimeInfo[] getOtherPrimeInfo();
}
