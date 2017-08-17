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


public interface ObjectInput extends DataInput, AutoCloseable {

    public Object readObject()
        throws ClassNotFoundException, IOException;


    public int read() throws IOException;


    public int read(byte b[]) throws IOException;


    public int read(byte b[], int off, int len) throws IOException;


    public long skip(long n) throws IOException;


    public int available() throws IOException;


    public void close() throws IOException;
}
