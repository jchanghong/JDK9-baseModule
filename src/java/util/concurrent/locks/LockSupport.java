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

package java.util.concurrent.locks;

import jdk.internal.misc.Unsafe;


public class LockSupport {
    private LockSupport() {} // Cannot be instantiated.

    private static void setBlocker(Thread t, Object arg) {
        // Even though volatile, hotspot doesn't need a write barrier here.
        U.putObject(t, PARKBLOCKER, arg);
    }


    public static void unpark(Thread thread) {
        if (thread != null)
            U.unpark(thread);
    }


    public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        U.park(false, 0L);
        setBlocker(t, null);
    }


    public static void parkNanos(Object blocker, long nanos) {
        if (nanos > 0) {
            Thread t = Thread.currentThread();
            setBlocker(t, blocker);
            U.park(false, nanos);
            setBlocker(t, null);
        }
    }


    public static void parkUntil(Object blocker, long deadline) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        U.park(true, deadline);
        setBlocker(t, null);
    }


    public static Object getBlocker(Thread t) {
        if (t == null)
            throw new NullPointerException();
        return U.getObjectVolatile(t, PARKBLOCKER);
    }


    public static void park() {
        U.park(false, 0L);
    }


    public static void parkNanos(long nanos) {
        if (nanos > 0)
            U.park(false, nanos);
    }


    public static void parkUntil(long deadline) {
        U.park(true, deadline);
    }


    static final int nextSecondarySeed() {
        int r;
        Thread t = Thread.currentThread();
        if ((r = U.getInt(t, SECONDARY)) != 0) {
            r ^= r << 13;   // xorshift
            r ^= r >>> 17;
            r ^= r << 5;
        }
        else if ((r = java.util.concurrent.ThreadLocalRandom.current().nextInt()) == 0)
            r = 1; // avoid zero
        U.putInt(t, SECONDARY, r);
        return r;
    }


    static final long getThreadId(Thread thread) {
        return U.getLongVolatile(thread, TID);
    }

    // Hotspot implementation via intrinsics API
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long PARKBLOCKER;
    private static final long SECONDARY;
    private static final long TID;
    static {
        try {
            PARKBLOCKER = U.objectFieldOffset
                (Thread.class.getDeclaredField("parkBlocker"));
            SECONDARY = U.objectFieldOffset
                (Thread.class.getDeclaredField("threadLocalRandomSecondarySeed"));
            TID = U.objectFieldOffset
                (Thread.class.getDeclaredField("tid"));

        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

}
