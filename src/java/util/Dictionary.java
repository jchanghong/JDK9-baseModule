/*
 * Copyright (c) 1995, 2004, Oracle and/or its affiliates. All rights reserved.
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

package java.util;


public abstract
class Dictionary<K,V> {

    public Dictionary() {
    }


    public abstract int size();


    public abstract boolean isEmpty();


    public abstract Enumeration<K> keys();


    public abstract Enumeration<V> elements();


    public abstract V get(Object key);


    public abstract V put(K key, V value);


    public abstract V remove(Object key);
}
