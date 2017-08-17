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

package java.security.spec;

import java.security.GeneralSecurityException;



public class InvalidKeySpecException extends GeneralSecurityException {

    private static final long serialVersionUID = 3546139293998810778L;


    public InvalidKeySpecException() {
        super();
    }


    public InvalidKeySpecException(String msg) {
        super(msg);
    }


    public InvalidKeySpecException(String message, Throwable cause) {
        super(message, cause);
    }


    public InvalidKeySpecException(Throwable cause) {
        super(cause);
    }
}
