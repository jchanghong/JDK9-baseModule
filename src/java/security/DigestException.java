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


public class DigestException extends GeneralSecurityException {

    private static final long serialVersionUID = 5821450303093652515L;


    public DigestException() {
        super();
    }


   public DigestException(String msg) {
       super(msg);
    }


    public DigestException(String message, Throwable cause) {
        super(message, cause);
    }


    public DigestException(Throwable cause) {
        super(cause);
    }
}
