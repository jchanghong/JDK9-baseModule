/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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



public interface FileOwnerAttributeView
    extends FileAttributeView
{

    @Override
    String name();


    UserPrincipal getOwner() throws IOException;


    void setOwner(UserPrincipal owner) throws IOException;
}
