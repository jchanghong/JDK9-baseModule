/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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



public interface BasicFileAttributes {


    FileTime lastModifiedTime();


    FileTime lastAccessTime();


    FileTime creationTime();


    boolean isRegularFile();


    boolean isDirectory();


    boolean isSymbolicLink();


    boolean isOther();


    long size();


    Object fileKey();
}
