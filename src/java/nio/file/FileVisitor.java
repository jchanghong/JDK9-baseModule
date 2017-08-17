/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file;

import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;



public interface FileVisitor<T> {


    FileVisitResult preVisitDirectory(T dir, BasicFileAttributes attrs)
        throws IOException;


    FileVisitResult visitFile(T file, BasicFileAttributes attrs)
        throws IOException;


    FileVisitResult visitFileFailed(T file, IOException exc)
        throws IOException;


    FileVisitResult postVisitDirectory(T dir, IOException exc)
        throws IOException;
}
