/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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



public abstract class SecureRandomSpi implements java.io.Serializable {

    private static final long serialVersionUID = -2991854161009191830L;


    public SecureRandomSpi() {
        // ignored
    }


    protected SecureRandomSpi(SecureRandomParameters params) {
        // ignored
    }


    protected abstract void engineSetSeed(byte[] seed);


    protected abstract void engineNextBytes(byte[] bytes);


    protected void engineNextBytes(
            byte[] bytes, SecureRandomParameters params) {
        throw new UnsupportedOperationException();
    }


    protected abstract byte[] engineGenerateSeed(int numBytes);


    protected void engineReseed(SecureRandomParameters params) {
        throw new UnsupportedOperationException();
    }


    protected SecureRandomParameters engineGetParameters() {
        return null;
    }


    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
