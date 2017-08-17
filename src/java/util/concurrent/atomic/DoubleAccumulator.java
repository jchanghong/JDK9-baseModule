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

import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;

import java.io.Serializable;
import java.util.function.DoubleBinaryOperator;


public class DoubleAccumulator extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;

    private final DoubleBinaryOperator function;
    private final long identity; // use long representation


    public DoubleAccumulator(DoubleBinaryOperator accumulatorFunction,
                             double identity) {
        this.function = accumulatorFunction;
        base = this.identity = doubleToRawLongBits(identity);
    }


    public void accumulate(double x) {
        Cell[] as; long b, v, r; int m; Cell a;
        if ((as = cells) != null
            || ((r = doubleToRawLongBits
                (function.applyAsDouble(longBitsToDouble(b = base), x))) != b
                && !casBase(b, r))) {
            boolean uncontended = true;
            if (as == null
                || (m = as.length - 1) < 0
                || (a = as[getProbe() & m]) == null
                || !(uncontended =
                     ((r = doubleToRawLongBits
                       (function.applyAsDouble
                        (longBitsToDouble(v = a.value), x))) == v)
                     || a.cas(v, r)))
                doubleAccumulate(x, function, uncontended);
        }
    }


    public double get() {
        Cell[] as = cells;
        double result = longBitsToDouble(base);
        if (as != null) {
            for (Cell a : as)
                if (a != null)
                    result = function.applyAsDouble
                        (result, longBitsToDouble(a.value));
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


    public double getThenReset() {
        Cell[] as = cells;
        double result = longBitsToDouble(base);
        base = identity;
        if (as != null) {
            for (Cell a : as) {
                if (a != null) {
                    double v = longBitsToDouble(a.value);
                    a.reset(identity);
                    result = function.applyAsDouble(result, v);
                }
            }
        }
        return result;
    }


    public String toString() {
        return Double.toString(get());
    }


    public double doubleValue() {
        return get();
    }


    public long longValue() {
        return (long)get();
    }


    public int intValue() {
        return (int)get();
    }


    public float floatValue() {
        return (float)get();
    }


    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;


        private final double value;


        private final DoubleBinaryOperator function;


        private final long identity;

        SerializationProxy(double value,
                           DoubleBinaryOperator function,
                           long identity) {
            this.value = value;
            this.function = function;
            this.identity = identity;
        }


        private Object readResolve() {
            double d = longBitsToDouble(identity);
            DoubleAccumulator a = new DoubleAccumulator(function, d);
            a.base = doubleToRawLongBits(value);
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
