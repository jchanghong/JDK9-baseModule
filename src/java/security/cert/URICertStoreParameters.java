/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;


public final class URICertStoreParameters implements CertStoreParameters {


    private final URI uri;

    /*
     * Hash code for this parameters.
     */
    private int myhash = -1;


    public URICertStoreParameters(URI uri) {
        if (uri == null) {
            throw new NullPointerException();
        }
        this.uri = uri;
    }


    public URI getURI() {
        return uri;
    }


    @Override
    public URICertStoreParameters clone() {
        try {
            return new URICertStoreParameters(uri);
        } catch (NullPointerException e) {
            /* Cannot happen */
            throw new InternalError(e.toString(), e);
        }
    }


    @Override
    public int hashCode() {
        if (myhash == -1) {
            myhash = uri.hashCode()*7;
        }
        return myhash;
    }


    @Override
    public boolean equals(Object p) {
        if (p == null || (!(p instanceof URICertStoreParameters))) {
            return false;
        }

        if (p == this) {
            return true;
        }

        URICertStoreParameters other = (URICertStoreParameters)p;
        return uri.equals(other.getURI());
    }


    @Override
    public String toString() {
        return "URICertStoreParameters: " + uri.toString();
    }
}
