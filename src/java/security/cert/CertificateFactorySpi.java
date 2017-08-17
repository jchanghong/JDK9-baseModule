/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.security.Provider;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;



public abstract class CertificateFactorySpi {


    public abstract Certificate engineGenerateCertificate(InputStream inStream)
        throws CertificateException;


    public CertPath engineGenerateCertPath(InputStream inStream)
        throws CertificateException
    {
        throw new UnsupportedOperationException();
    }


    public CertPath engineGenerateCertPath(InputStream inStream,
        String encoding) throws CertificateException
    {
        throw new UnsupportedOperationException();
    }


    public CertPath
        engineGenerateCertPath(List<? extends Certificate> certificates)
        throws CertificateException
    {
        throw new UnsupportedOperationException();
    }


    public Iterator<String> engineGetCertPathEncodings() {
        throw new UnsupportedOperationException();
    }


    public abstract Collection<? extends Certificate>
            engineGenerateCertificates(InputStream inStream)
            throws CertificateException;


    public abstract CRL engineGenerateCRL(InputStream inStream)
        throws CRLException;


    public abstract Collection<? extends CRL> engineGenerateCRLs
            (InputStream inStream) throws CRLException;
}
