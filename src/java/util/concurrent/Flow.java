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


public final class Flow {

    private Flow() {} // uninstantiable


    @FunctionalInterface
    public static interface Publisher<T> {

        public void subscribe(Subscriber<? super T> subscriber);
    }


    public static interface Subscriber<T> {

        public void onSubscribe(Subscription subscription);


        public void onNext(T item);


        public void onError(Throwable throwable);


        public void onComplete();
    }


    public static interface Subscription {

        public void request(long n);


        public void cancel();
    }


    public static interface Processor<T,R> extends Subscriber<T>, Publisher<R> {
    }

    static final int DEFAULT_BUFFER_SIZE = 256;


    public static int defaultBufferSize() {
        return DEFAULT_BUFFER_SIZE;
    }

}
