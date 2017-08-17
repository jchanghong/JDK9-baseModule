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

package java.nio.file;

import java.util.Iterator;
import java.io.Closeable;
import java.io.IOException;



public interface DirectoryStream<T>
    extends Closeable, Iterable<T> {

    @FunctionalInterface
    public static interface Filter<T> {

        boolean accept(T entry) throws IOException;
    }


    @Override
    Iterator<T> iterator();
}
