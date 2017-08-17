/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

import java.security.cert.Certificate;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.Principal;
import java.util.List;


public abstract class SecureCacheResponse extends CacheResponse {

    public abstract String getCipherSuite();


    public abstract List<Certificate> getLocalCertificateChain();


    public abstract List<Certificate> getServerCertificateChain()
        throws SSLPeerUnverifiedException;


     public abstract Principal getPeerPrincipal()
             throws SSLPeerUnverifiedException;


     public abstract Principal getLocalPrincipal();
}
