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


public interface TransferQueue<E> extends BlockingQueue<E> {

    boolean tryTransfer(E e);


    void transfer(E e) throws InterruptedException;


    boolean tryTransfer(E e, long timeout, TimeUnit unit)
        throws InterruptedException;


    boolean hasWaitingConsumer();


    int getWaitingConsumerCount();
}
