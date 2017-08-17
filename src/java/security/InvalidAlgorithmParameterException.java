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



public class InvalidAlgorithmParameterException
extends GeneralSecurityException {

    private static final long serialVersionUID = 2864672297499471472L;


    public InvalidAlgorithmParameterException() {
        super();
    }


    public InvalidAlgorithmParameterException(String msg) {
        super(msg);
    }


    public InvalidAlgorithmParameterException(String message, Throwable cause) {
        super(message, cause);
    }


    public InvalidAlgorithmParameterException(Throwable cause) {
        super(cause);
    }
}
