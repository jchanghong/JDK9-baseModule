/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.*;
import java.util.List;
import java.io.IOException;



public interface AclFileAttributeView
    extends FileOwnerAttributeView
{

    @Override
    String name();


    List<AclEntry> getAcl() throws IOException;


    void setAcl(List<AclEntry> acl) throws IOException;
}
