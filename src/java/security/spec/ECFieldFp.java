/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.security.spec;

import java.math.BigInteger;
import java.util.Arrays;


public class ECFieldFp implements ECField {

    private BigInteger p;


    public ECFieldFp(BigInteger p) {
        if (p.signum() != 1) {
            throw new IllegalArgumentException("p is not positive");
        }
        this.p = p;
    }


    public int getFieldSize() {
        return p.bitLength();
    };


    public BigInteger getP() {
        return p;
    }


    public boolean equals(Object obj) {
        if (this == obj)  return true;
        if (obj instanceof ECFieldFp) {
            return (p.equals(((ECFieldFp)obj).p));
        }
        return false;
    }


    public int hashCode() {
        return p.hashCode();
    }
}
