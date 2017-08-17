/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.security;

import java.io.*;
import java.util.Date;


@Deprecated(since="1.2")
public interface Certificate {


    public abstract Principal getGuarantor();


    public abstract Principal getPrincipal();


    public abstract PublicKey getPublicKey();


    public abstract void encode(OutputStream stream)
        throws KeyException, IOException;


    public abstract void decode(InputStream stream)
        throws KeyException, IOException;



    public abstract String getFormat();


    public String toString(boolean detailed);
}
