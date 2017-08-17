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


public class DoubleAdder extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;

    /*
     * Note that we must use "long" for underlying representations,
     * because there is no compareAndSet for double, due to the fact
     * that the bitwise equals used in any CAS implementation is not
     * the same as double-precision equals.  However, we use CAS only
     * to detect and alleviate contention, for which bitwise equals
     * works best anyway. In principle, the long/double conversions
     * used here should be essentially free on most platforms since
     * they just re-interpret bits.
     */


    public DoubleAdder() {
    }


    public void add(double x) {
        Cell[] as; long b, v; int m; Cell a;
        if ((as = cells) != null ||
            !casBase(b = base,
                     Double.doubleToRawLongBits
                     (Double.longBitsToDouble(b) + x))) {
            boolean uncontended = true;
            if (as == null || (m = as.length - 1) < 0 ||
                (a = as[getProbe() & m]) == null ||
                !(uncontended = a.cas(v = a.value,
                                      Double.doubleToRawLongBits
                                      (Double.longBitsToDouble(v) + x))))
                doubleAccumulate(x, null, uncontended);
        }
    }


    public double sum() {
        Cell[] as = cells;
        double sum = Double.longBitsToDouble(base);
        if (as != null) {
            for (Cell a : as)
                if (a != null)
                    sum += Double.longBitsToDouble(a.value);
        }
        return sum;
    }


    public void reset() {
        Cell[] as = cells;
        base = 0L; // relies on fact that double 0 must have same rep as long
        if (as != null) {
            for (Cell a : as)
                if (a != null)
                    a.reset();
        }
    }


    public double sumThenReset() {
        Cell[] as = cells;
        double sum = Double.longBitsToDouble(base);
        base = 0L;
        if (as != null) {
            for (Cell a : as) {
                if (a != null) {
                    long v = a.value;
                    a.reset();
                    sum += Double.longBitsToDouble(v);
                }
            }
        }
        return sum;
    }


    public String toString() {
        return Double.toString(sum());
    }


    public double doubleValue() {
        return sum();
    }


    public long longValue() {
        return (long)sum();
    }


    public int intValue() {
        return (int)sum();
    }


    public float floatValue() {
        return (float)sum();
    }


    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;


        private final double value;

        SerializationProxy(DoubleAdder a) {
            value = a.sum();
        }


        private Object readResolve() {
            DoubleAdder a = new DoubleAdder();
            a.base = Double.doubleToRawLongBits(value);
            return a;
        }
    }


    private Object writeReplace() {
        return new SerializationProxy(this);
    }


    private void readObject(java.io.ObjectInputStream s)
        throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Proxy required");
    }

}
