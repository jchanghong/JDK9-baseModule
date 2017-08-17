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



public interface WatchEvent<T> {


    public static interface Kind<T> {

        String name();


        Class<T> type();
    }


    public static interface Modifier {

        String name();
    }


    Kind<T> kind();


    int count();


    T context();
}
