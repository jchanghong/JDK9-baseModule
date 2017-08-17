/*
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
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




public abstract class FilterWriter extends Writer {


    protected Writer out;


    protected FilterWriter(Writer out) {
        super(out);
        this.out = out;
    }


    public void write(int c) throws IOException {
        out.write(c);
    }


    public void write(char cbuf[], int off, int len) throws IOException {
        out.write(cbuf, off, len);
    }


    public void write(String str, int off, int len) throws IOException {
        out.write(str, off, len);
    }


    public void flush() throws IOException {
        out.flush();
    }

    public void close() throws IOException {
        out.close();
    }

}
