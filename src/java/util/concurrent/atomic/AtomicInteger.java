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

package java.util.concurrent.atomic;

import java.lang.invoke.VarHandle;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;


public class AtomicInteger extends Number implements java.io.Serializable {
    private static final long serialVersionUID = 6214790243416807050L;

    /*
     * This class intended to be implemented using VarHandles, but there
     * are unresolved cyclic startup dependencies.
     */
    private static final jdk.internal.misc.Unsafe U = jdk.internal.misc.Unsafe.getUnsafe();
    private static final long VALUE;

    static {
        try {
            VALUE = U.objectFieldOffset
                (AtomicInteger.class.getDeclaredField("value"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private volatile int value;


    public AtomicInteger(int initialValue) {
        value = initialValue;
    }


    public AtomicInteger() {
    }


    public final int get() {
        return value;
    }


    public final void set(int newValue) {
        value = newValue;
    }


    public final void lazySet(int newValue) {
        U.putIntRelease(this, VALUE, newValue);
    }


    public final int getAndSet(int newValue) {
        return U.getAndSetInt(this, VALUE, newValue);
    }


    public final boolean compareAndSet(int expectedValue, int newValue) {
        return U.compareAndSetInt(this, VALUE, expectedValue, newValue);
    }


    @Deprecated(since="9")
    public final boolean weakCompareAndSet(int expectedValue, int newValue) {
        return U.weakCompareAndSetIntPlain(this, VALUE, expectedValue, newValue);
    }


    public final boolean weakCompareAndSetPlain(int expectedValue, int newValue) {
        return U.weakCompareAndSetIntPlain(this, VALUE, expectedValue, newValue);
    }


    public final int getAndIncrement() {
        return U.getAndAddInt(this, VALUE, 1);
    }


    public final int getAndDecrement() {
        return U.getAndAddInt(this, VALUE, -1);
    }


    public final int getAndAdd(int delta) {
        return U.getAndAddInt(this, VALUE, delta);
    }


    public final int incrementAndGet() {
        return U.getAndAddInt(this, VALUE, 1) + 1;
    }


    public final int decrementAndGet() {
        return U.getAndAddInt(this, VALUE, -1) - 1;
    }


    public final int addAndGet(int delta) {
        return U.getAndAddInt(this, VALUE, delta) + delta;
    }


    public final int getAndUpdate(IntUnaryOperator updateFunction) {
        int prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext)
                next = updateFunction.applyAsInt(prev);
            if (weakCompareAndSetVolatile(prev, next))
                return prev;
            haveNext = (prev == (prev = get()));
        }
    }


    public final int updateAndGet(IntUnaryOperator updateFunction) {
        int prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext)
                next = updateFunction.applyAsInt(prev);
            if (weakCompareAndSetVolatile(prev, next))
                return next;
            haveNext = (prev == (prev = get()));
        }
    }


    public final int getAndAccumulate(int x,
                                      IntBinaryOperator accumulatorFunction) {
        int prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext)
                next = accumulatorFunction.applyAsInt(prev, x);
            if (weakCompareAndSetVolatile(prev, next))
                return prev;
            haveNext = (prev == (prev = get()));
        }
    }


    public final int accumulateAndGet(int x,
                                      IntBinaryOperator accumulatorFunction) {
        int prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext)
                next = accumulatorFunction.applyAsInt(prev, x);
            if (weakCompareAndSetVolatile(prev, next))
                return next;
            haveNext = (prev == (prev = get()));
        }
    }


    public String toString() {
        return Integer.toString(get());
    }


    public int intValue() {
        return get();
    }


    public long longValue() {
        return (long)get();
    }


    public float floatValue() {
        return (float)get();
    }


    public double doubleValue() {
        return (double)get();
    }

    // jdk9


    public final int getPlain() {
        return U.getInt(this, VALUE);
    }


    public final void setPlain(int newValue) {
        U.putInt(this, VALUE, newValue);
    }


    public final int getOpaque() {
        return U.getIntOpaque(this, VALUE);
    }


    public final void setOpaque(int newValue) {
        U.putIntOpaque(this, VALUE, newValue);
    }


    public final int getAcquire() {
        return U.getIntAcquire(this, VALUE);
    }


    public final void setRelease(int newValue) {
        U.putIntRelease(this, VALUE, newValue);
    }


    public final int compareAndExchange(int expectedValue, int newValue) {
        return U.compareAndExchangeInt(this, VALUE, expectedValue, newValue);
    }


    public final int compareAndExchangeAcquire(int expectedValue, int newValue) {
        return U.compareAndExchangeIntAcquire(this, VALUE, expectedValue, newValue);
    }


    public final int compareAndExchangeRelease(int expectedValue, int newValue) {
        return U.compareAndExchangeIntRelease(this, VALUE, expectedValue, newValue);
    }


    public final boolean weakCompareAndSetVolatile(int expectedValue, int newValue) {
        return U.weakCompareAndSetInt(this, VALUE, expectedValue, newValue);
    }


    public final boolean weakCompareAndSetAcquire(int expectedValue, int newValue) {
        return U.weakCompareAndSetIntAcquire(this, VALUE, expectedValue, newValue);
    }


    public final boolean weakCompareAndSetRelease(int expectedValue, int newValue) {
        return U.weakCompareAndSetIntRelease(this, VALUE, expectedValue, newValue);
    }

}
