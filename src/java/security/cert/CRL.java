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



public abstract class CRL {

    // the CRL type
    private String type;


    protected CRL(String type) {
        this.type = type;
    }


    public final String getType() {
        return this.type;
    }


    public abstract String toString();


    public abstract boolean isRevoked(Certificate cert);
}
