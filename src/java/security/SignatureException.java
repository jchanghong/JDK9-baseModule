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

package java.security;



public class SignatureException extends GeneralSecurityException {

    private static final long serialVersionUID = 7509989324975124438L;


    public SignatureException() {
        super();
    }


    public SignatureException(String msg) {
        super(msg);
    }


    public SignatureException(String message, Throwable cause) {
        super(message, cause);
    }


    public SignatureException(Throwable cause) {
        super(cause);
    }
}
