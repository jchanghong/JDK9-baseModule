/*
 * Copyright (c) 1996, 2010, Oracle and/or its affiliates. All rights reserved.
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

package java.io;


public interface ObjectOutput extends DataOutput, AutoCloseable {

    public void writeObject(Object obj)
      throws IOException;


    public void write(int b) throws IOException;


    public void write(byte b[]) throws IOException;


    public void write(byte b[], int off, int len) throws IOException;


    public void flush() throws IOException;


    public void close() throws IOException;
}
