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


public class CertificateExpiredException extends CertificateException {

    private static final long serialVersionUID = 9071001339691533771L;


    public CertificateExpiredException() {
        super();
    }


    public CertificateExpiredException(String message) {
        super(message);
    }
}
