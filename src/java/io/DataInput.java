/*
 * Copyright (c) 1995, 2017, Oracle and/or its affiliates. All rights reserved.
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


public
interface DataInput {

    void readFully(byte b[]) throws IOException;


    void readFully(byte b[], int off, int len) throws IOException;


    int skipBytes(int n) throws IOException;


    boolean readBoolean() throws IOException;


    byte readByte() throws IOException;


    int readUnsignedByte() throws IOException;


    short readShort() throws IOException;


    int readUnsignedShort() throws IOException;


    char readChar() throws IOException;


    int readInt() throws IOException;


    long readLong() throws IOException;


    float readFloat() throws IOException;


    double readDouble() throws IOException;


    String readLine() throws IOException;


    String readUTF() throws IOException;
}
