/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file.attribute;

import java.io.IOException;



public abstract class UserPrincipalLookupService {


    protected UserPrincipalLookupService() {
    }


    public abstract UserPrincipal lookupPrincipalByName(String name)
        throws IOException;


    public abstract GroupPrincipal lookupPrincipalByGroupName(String group)
        throws IOException;
}
