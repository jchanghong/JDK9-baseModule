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

package java.security.cert;


public class CertificateEncodingException extends CertificateException {

    private static final long serialVersionUID = 6219492851589449162L;


    public CertificateEncodingException() {
        super();
    }


    public CertificateEncodingException(String message) {
        super(message);
    }


    public CertificateEncodingException(String message, Throwable cause) {
        super(message, cause);
    }


    public CertificateEncodingException(Throwable cause) {
        super(cause);
    }
}
