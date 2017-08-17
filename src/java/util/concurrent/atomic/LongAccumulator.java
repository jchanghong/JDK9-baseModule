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

import java.io.Serializable;
import java.util.function.LongBinaryOperator;


public class LongAccumulator extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;

    private final LongBinaryOperator function;
    private final long identity;


    public LongAccumulator(LongBinaryOperator accumulatorFunction,
                           long identity) {
        this.function = accumulatorFunction;
        base = this.identity = identity;
    }


    public void accumulate(long x) {
        Cell[] as; long b, v, r; int m; Cell a;
        if ((as = cells) != null
            || ((r = function.applyAsLong(b = base, x)) != b
                && !casBase(b, r))) {
            boolean uncontended = true;
            if (as == null
                || (m = as.length - 1) < 0
                || (a = as[getProbe() & m]) == null
                || !(uncontended =
                     (r = function.applyAsLong(v = a.value, x)) == v
                     || a.cas(v, r)))
                longAccumulate(x, function, uncontended);
        }
    }


    public long get() {
        Cell[] as = cells;
        long result = base;
        if (as != null) {
            for (Cell a : as)
                if (a != null)
                    result = function.applyAsLong(result, a.value);
        }
        return result;
    }


    public void reset() {
        Cell[] as = cells;
        base = identity;
        if (as != null) {
            for (Cell a : as)
                if (a != null)
                    a.reset(identity);
        }
    }


    public long getThenReset() {
        Cell[] as = cells;
        long result = base;
        base = identity;
        if (as != null) {
            for (Cell a : as) {
                if (a != null) {
                    long v = a.value;
                    a.reset(identity);
                    result = function.applyAsLong(result, v);
                }
            }
        }
        return result;
    }


    public String toString() {
        return Long.toString(get());
    }


    public long longValue() {
        return get();
    }


    public int intValue() {
        return (int)get();
    }


    public float floatValue() {
        return (float)get();
    }


    public double doubleValue() {
        return (double)get();
    }


    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;


        private final long value;


        private final LongBinaryOperator function;


        private final long identity;

        SerializationProxy(long value,
                           LongBinaryOperator function,
                           long identity) {
            this.value = value;
            this.function = function;
            this.identity = identity;
        }


        private Object readResolve() {
            LongAccumulator a = new LongAccumulator(function, identity);
            a.base = value;
            return a;
        }
    }


    private Object writeReplace() {
        return new SerializationProxy(get(), function, identity);
    }


    private void readObject(java.io.ObjectInputStream s)
        throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Proxy required");
    }

}
