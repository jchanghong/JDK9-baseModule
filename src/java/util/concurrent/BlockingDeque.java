/*
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

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;


public interface BlockingDeque<E> extends BlockingQueue<E>, Deque<E> {
    /*
     * We have "diamond" multiple interface inheritance here, and that
     * introduces ambiguities.  Methods might end up with different
     * specs depending on the branch chosen by javadoc.  Thus a lot of
     * methods specs here are copied from superinterfaces.
     */


    void addFirst(E e);


    void addLast(E e);


    boolean offerFirst(E e);


    boolean offerLast(E e);


    void putFirst(E e) throws InterruptedException;


    void putLast(E e) throws InterruptedException;


    boolean offerFirst(E e, long timeout, TimeUnit unit)
        throws InterruptedException;


    boolean offerLast(E e, long timeout, TimeUnit unit)
        throws InterruptedException;


    E takeFirst() throws InterruptedException;


    E takeLast() throws InterruptedException;


    E pollFirst(long timeout, TimeUnit unit)
        throws InterruptedException;


    E pollLast(long timeout, TimeUnit unit)
        throws InterruptedException;


    boolean removeFirstOccurrence(Object o);


    boolean removeLastOccurrence(Object o);

    // *** BlockingQueue methods ***


    boolean add(E e);


    boolean offer(E e);


    void put(E e) throws InterruptedException;


    boolean offer(E e, long timeout, TimeUnit unit)
        throws InterruptedException;


    E remove();


    E poll();


    E take() throws InterruptedException;


    E poll(long timeout, TimeUnit unit)
        throws InterruptedException;


    E element();


    E peek();


    boolean remove(Object o);


    boolean contains(Object o);


    int size();


    Iterator<E> iterator();

    // *** Stack methods ***


    void push(E e);
}
