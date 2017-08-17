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



public class KeyStoreException extends GeneralSecurityException {

    private static final long serialVersionUID = -1119353179322377262L;


    public KeyStoreException() {
        super();
    }


   public KeyStoreException(String msg) {
       super(msg);
    }


    public KeyStoreException(String message, Throwable cause) {
        super(message, cause);
    }


    public KeyStoreException(Throwable cause) {
        super(cause);
    }
}
