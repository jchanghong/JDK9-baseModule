/*
 * Copyright (c) 1996, 2001, Oracle and/or its affiliates. All rights reserved.
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




public class FileWriter extends OutputStreamWriter {


    public FileWriter(String fileName) throws IOException {
        super(new FileOutputStream(fileName));
    }


    public FileWriter(String fileName, boolean append) throws IOException {
        super(new FileOutputStream(fileName, append));
    }


    public FileWriter(File file) throws IOException {
        super(new FileOutputStream(file));
    }


    public FileWriter(File file, boolean append) throws IOException {
        super(new FileOutputStream(file, append));
    }


    public FileWriter(FileDescriptor fd) {
        super(new FileOutputStream(fd));
    }

}
