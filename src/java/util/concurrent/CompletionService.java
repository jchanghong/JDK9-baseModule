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


public interface CompletionService<V> {

    Future<V> submit(Callable<V> task);


    Future<V> submit(Runnable task, V result);


    Future<V> take() throws InterruptedException;


    Future<V> poll();


    Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException;
}
