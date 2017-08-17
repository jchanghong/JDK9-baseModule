/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;



public abstract class AlgorithmParametersSpi {


    protected abstract void engineInit(AlgorithmParameterSpec paramSpec)
        throws InvalidParameterSpecException;


    protected abstract void engineInit(byte[] params)
        throws IOException;


    protected abstract void engineInit(byte[] params, String format)
        throws IOException;


    protected abstract
        <T extends AlgorithmParameterSpec>
        T engineGetParameterSpec(Class<T> paramSpec)
        throws InvalidParameterSpecException;


    protected abstract byte[] engineGetEncoded() throws IOException;


    protected abstract byte[] engineGetEncoded(String format)
        throws IOException;


    protected abstract String engineToString();
}
