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



public class KeyException extends GeneralSecurityException {

    private static final long serialVersionUID = -7483676942812432108L;


    public KeyException() {
        super();
    }


    public KeyException(String msg) {
        super(msg);
    }


    public KeyException(String message, Throwable cause) {
        super(message, cause);
    }


    public KeyException(Throwable cause) {
        super(cause);
    }
}
