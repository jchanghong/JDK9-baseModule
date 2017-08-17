/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.security.GeneralSecurityException;


public class CertStoreException extends GeneralSecurityException {

    private static final long serialVersionUID = 2395296107471573245L;


    public CertStoreException() {
        super();
    }


    public CertStoreException(String msg) {
        super(msg);
    }


    public CertStoreException(Throwable cause) {
        super(cause);
    }


    public CertStoreException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
