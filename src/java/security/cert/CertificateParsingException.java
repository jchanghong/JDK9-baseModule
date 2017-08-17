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


public class CertificateParsingException extends CertificateException {

    private static final long serialVersionUID = -7989222416793322029L;


    public CertificateParsingException() {
        super();
    }


    public CertificateParsingException(String message) {
        super(message);
    }


    public CertificateParsingException(String message, Throwable cause) {
        super(message, cause);
    }


    public CertificateParsingException(Throwable cause) {
        super(cause);
    }
}
