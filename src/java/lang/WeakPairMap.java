/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package java.lang;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;


final class WeakPairMap<K1, K2, V> {

    private final ConcurrentHashMap<Pair<K1, K2>, V> map = new ConcurrentHashMap<>();
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();


    public boolean containsKeyPair(K1 k1, K2 k2) {
        expungeStaleAssociations();
        return map.containsKey(Pair.lookup(k1, k2));
    }


    public V get(K1 k1, K2 k2) {
        expungeStaleAssociations();
        return map.get(Pair.lookup(k1, k2));
    }


    public V put(K1 k1, K2 k2, V v) {
        expungeStaleAssociations();
        return map.put(Pair.weak(k1, k2, queue), v);
    }


    public V putIfAbsent(K1 k1, K2 k2, V v) {
        expungeStaleAssociations();
        return map.putIfAbsent(Pair.weak(k1, k2, queue), v);
    }


    public V computeIfAbsent(K1 k1, K2 k2,
                             BiFunction<? super K1, ? super K2, ? extends V>
                                 mappingFunction) {
        expungeStaleAssociations();
        try {
            return map.computeIfAbsent(
                Pair.weak(k1, k2, queue),
                pair -> mappingFunction.apply(pair.first(), pair.second()));
        } finally {
            Reference.reachabilityFence(k1);
            Reference.reachabilityFence(k2);
        }
    }


    public Collection<V> values() {
        expungeStaleAssociations();
        return map.values();
    }


    private void expungeStaleAssociations() {
        WeakRefPeer<?> peer;
        while ((peer = (WeakRefPeer<?>) queue.poll()) != null) {
            map.remove(peer.weakPair());
        }
    }


    private interface Pair<K1, K2> {

        static <K1, K2> Pair<K1, K2> weak(K1 k1, K2 k2,
                                          ReferenceQueue<Object> queue) {
            return new Weak<>(k1, k2, queue);
        }

        static <K1, K2> Pair<K1, K2> lookup(K1 k1, K2 k2) {
            return new Lookup<>(k1, k2);
        }


        K1 first();


        K2 second();

        static int hashCode(Object first, Object second) {
            // assert first != null && second != null;
            return System.identityHashCode(first) ^
                   System.identityHashCode(second);
        }

        static boolean equals(Object first, Object second, Pair<?, ?> p) {
            return first != null && second != null &&
                   first == p.first() && second == p.second();
        }


        final class Weak<K1, K2> extends WeakRefPeer<K1> implements Pair<K1, K2> {

            // saved hash so it can be retrieved after the reference is cleared
            private final int hash;
            // link to <K2> peer
            private final WeakRefPeer<K2> peer;

            Weak(K1 k1, K2 k2, ReferenceQueue<Object> queue) {
                super(k1, queue);
                hash = Pair.hashCode(k1, k2);
                peer = new WeakRefPeer<>(k2, queue) {
                    // link back to <K1> peer
                    @Override
                    Weak<?, ?> weakPair() { return Weak.this; }
                };
            }

            @Override
            Weak<?, ?> weakPair() {
                return this;
            }

            @Override
            public K1 first() {
                return get();
            }

            @Override
            public K2 second() {
                return peer.get();
            }

            @Override
            public int hashCode() {
                return hash;
            }

            @Override
            public boolean equals(Object obj) {
                return this == obj ||
                       (obj instanceof Pair &&
                        Pair.equals(first(), second(), (Pair<?, ?>) obj));
            }
        }


        final class Lookup<K1, K2> implements Pair<K1, K2> {
            private final K1 k1;
            private final K2 k2;

            Lookup(K1 k1, K2 k2) {
                this.k1 = Objects.requireNonNull(k1);
                this.k2 = Objects.requireNonNull(k2);
            }

            @Override
            public K1 first() {
                return k1;
            }

            @Override
            public K2 second() {
                return k2;
            }

            @Override
            public int hashCode() {
                return Pair.hashCode(k1, k2);
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Pair &&
                       Pair.equals(k1, k2, (Pair<?, ?>) obj);
            }
        }
    }


    private static abstract class WeakRefPeer<K> extends WeakReference<K> {

        WeakRefPeer(K k, ReferenceQueue<Object> queue) {
            super(Objects.requireNonNull(k), queue);
        }


        abstract Pair.Weak<?, ?> weakPair();
    }
}
