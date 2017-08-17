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
import java.util.Set;
import java.io.IOException;



public interface PosixFileAttributeView
    extends BasicFileAttributeView, FileOwnerAttributeView
{

    @Override
    String name();


    @Override
    PosixFileAttributes readAttributes() throws IOException;


    void setPermissions(Set<PosixFilePermission> perms) throws IOException;


    void setGroup(GroupPrincipal group) throws IOException;
}
