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

import java.nio.ByteBuffer;
import java.util.List;
import java.io.IOException;



public interface UserDefinedFileAttributeView
    extends FileAttributeView
{

    @Override
    String name();


    List<String> list() throws IOException;


    int size(String name) throws IOException;


    int read(String name, ByteBuffer dst) throws IOException;


    int write(String name, ByteBuffer src) throws IOException;


    void delete(String name) throws IOException;
}
