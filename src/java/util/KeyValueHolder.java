/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.vm.annotation.Stable;


final class KeyValueHolder<K,V> implements Map.Entry<K,V> {
    @Stable
    final K key;
    @Stable
    final V value;

    KeyValueHolder(K k, V v) {
        key = Objects.requireNonNull(k);
        value = Objects.requireNonNull(v);
    }


    @Override
    public K getKey() {
        return key;
    }


    @Override
    public V getValue() {
        return value;
    }


    @Override
    public V setValue(V value) {
        throw new UnsupportedOperationException("not supported");
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> e = (Map.Entry<?,?>)o;
        return key.equals(e.getKey()) && value.equals(e.getValue());
    }


    @Override
    public int hashCode() {
        return key.hashCode() ^ value.hashCode();
    }


    @Override
    public String toString() {
        return key + "=" + value;
    }
}
