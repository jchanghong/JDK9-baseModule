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



public class FileReader extends InputStreamReader {


    public FileReader(String fileName) throws FileNotFoundException {
        super(new FileInputStream(fileName));
    }


    public FileReader(File file) throws FileNotFoundException {
        super(new FileInputStream(file));
    }


    public FileReader(FileDescriptor fd) {
        super(new FileInputStream(fd));
    }

}
