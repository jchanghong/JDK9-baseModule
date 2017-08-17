/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;



public interface X509Extension {


    public boolean hasUnsupportedCriticalExtension();


    public Set<String> getCriticalExtensionOIDs();


    public Set<String> getNonCriticalExtensionOIDs();


    public byte[] getExtensionValue(String oid);
}
