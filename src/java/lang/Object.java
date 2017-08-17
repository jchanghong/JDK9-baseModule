/*
 * Copyright (c) 1994, 2017, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.HotSpotIntrinsicCandidate;


public class Object {

    private static native void registerNatives();
    static {
        registerNatives();
    }


    @HotSpotIntrinsicCandidate
    public Object() {}


    @HotSpotIntrinsicCandidate
    public final native Class<?> getClass();


    @HotSpotIntrinsicCandidate
    public native int hashCode();


    public boolean equals(Object obj) {
        return (this == obj);
    }


    @HotSpotIntrinsicCandidate
    protected native Object clone() throws CloneNotSupportedException;


    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }


    @HotSpotIntrinsicCandidate
    public final native void notify();


    @HotSpotIntrinsicCandidate
    public final native void notifyAll();


    public final native void wait(long timeout) throws InterruptedException;


    public final void wait(long timeout, int nanos) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                                "nanosecond timeout value out of range");
        }

        if (nanos > 0) {
            timeout++;
        }

        wait(timeout);
    }


    public final void wait() throws InterruptedException {
        wait(0);
    }


    @Deprecated(since="9")
    protected void finalize() throws Throwable { }
}
