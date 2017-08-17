/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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



public interface Set<E> extends Collection<E> {
    // Query Operations


    int size();


    boolean isEmpty();


    boolean contains(Object o);


    Iterator<E> iterator();


    Object[] toArray();


    <T> T[] toArray(T[] a);


    // Modification Operations


    boolean add(E e);



    boolean remove(Object o);


    // Bulk Operations


    boolean containsAll(Collection<?> c);


    boolean addAll(Collection<? extends E> c);


    boolean retainAll(Collection<?> c);


    boolean removeAll(Collection<?> c);


    void clear();


    // Comparison and hashing


    boolean equals(Object o);


    int hashCode();


    @Override
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT);
    }


    static <E> Set<E> of() {
        return ImmutableCollections.Set0.instance();
    }


    static <E> Set<E> of(E e1) {
        return new ImmutableCollections.Set1<>(e1);
    }


    static <E> Set<E> of(E e1, E e2) {
        return new ImmutableCollections.Set2<>(e1, e2);
    }


    static <E> Set<E> of(E e1, E e2, E e3) {
        return new ImmutableCollections.SetN<>(e1, e2, e3);
    }


    static <E> Set<E> of(E e1, E e2, E e3, E e4) {
        return new ImmutableCollections.SetN<>(e1, e2, e3, e4);
    }


    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5) {
        return new ImmutableCollections.SetN<>(e1, e2, e3, e4, e5);
    }


    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        return new ImmutableCollections.SetN<>(e1, e2, e3, e4, e5,
                                               e6);
    }


    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return new ImmutableCollections.SetN<>(e1, e2, e3, e4, e5,
                                               e6, e7);
    }


    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return new ImmutableCollections.SetN<>(e1, e2, e3, e4, e5,
                                               e6, e7, e8);
    }


    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return new ImmutableCollections.SetN<>(e1, e2, e3, e4, e5,
                                               e6, e7, e8, e9);
    }


    static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return new ImmutableCollections.SetN<>(e1, e2, e3, e4, e5,
                                               e6, e7, e8, e9, e10);
    }


    @SafeVarargs
    @SuppressWarnings("varargs")
    static <E> Set<E> of(E... elements) {
        switch (elements.length) { // implicit null check of elements
            case 0:
                return ImmutableCollections.Set0.instance();
            case 1:
                return new ImmutableCollections.Set1<>(elements[0]);
            case 2:
                return new ImmutableCollections.Set2<>(elements[0], elements[1]);
            default:
                return new ImmutableCollections.SetN<>(elements);
        }
    }
}
