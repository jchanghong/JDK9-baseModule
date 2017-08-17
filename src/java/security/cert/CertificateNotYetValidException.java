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


public class CertificateNotYetValidException extends CertificateException {

    static final long serialVersionUID = 4355919900041064702L;


    public CertificateNotYetValidException() {
        super();
    }


    public CertificateNotYetValidException(String message) {
        super(message);
    }
}
