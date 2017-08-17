/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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


public final class Byte extends Number implements Comparable<Byte> {


    public static final byte   MIN_VALUE = -128;


    public static final byte   MAX_VALUE = 127;


    @SuppressWarnings("unchecked")
    public static final Class<Byte>     TYPE = (Class<Byte>) Class.getPrimitiveClass("byte");


    public static String toString(byte b) {
        return Integer.toString((int)b, 10);
    }

    private static class ByteCache {
        private ByteCache(){}

        static final Byte cache[] = new Byte[-(-128) + 127 + 1];

        static {
            for(int i = 0; i < cache.length; i++)
                cache[i] = new Byte((byte)(i - 128));
        }
    }


    @HotSpotIntrinsicCandidate
    public static Byte valueOf(byte b) {
        final int offset = 128;
        return ByteCache.cache[(int)b + offset];
    }


    public static byte parseByte(String s, int radix)
        throws NumberFormatException {
        int i = Integer.parseInt(s, radix);
        if (i < MIN_VALUE || i > MAX_VALUE)
            throw new NumberFormatException(
                "Value out of range. Value:\"" + s + "\" Radix:" + radix);
        return (byte)i;
    }


    public static byte parseByte(String s) throws NumberFormatException {
        return parseByte(s, 10);
    }


    public static Byte valueOf(String s, int radix)
        throws NumberFormatException {
        return valueOf(parseByte(s, radix));
    }


    public static Byte valueOf(String s) throws NumberFormatException {
        return valueOf(s, 10);
    }


    public static Byte decode(String nm) throws NumberFormatException {
        int i = Integer.decode(nm);
        if (i < MIN_VALUE || i > MAX_VALUE)
            throw new NumberFormatException(
                    "Value " + i + " out of range from input " + nm);
        return valueOf((byte)i);
    }


    private final byte value;


    @Deprecated(since="9")
    public Byte(byte value) {
        this.value = value;
    }


    @Deprecated(since="9")
    public Byte(String s) throws NumberFormatException {
        this.value = parseByte(s, 10);
    }


    @HotSpotIntrinsicCandidate
    public byte byteValue() {
        return value;
    }


    public short shortValue() {
        return (short)value;
    }


    public int intValue() {
        return (int)value;
    }


    public long longValue() {
        return (long)value;
    }


    public float floatValue() {
        return (float)value;
    }


    public double doubleValue() {
        return (double)value;
    }


    public String toString() {
        return Integer.toString((int)value);
    }


    @Override
    public int hashCode() {
        return Byte.hashCode(value);
    }


    public static int hashCode(byte value) {
        return (int)value;
    }


    public boolean equals(Object obj) {
        if (obj instanceof Byte) {
            return value == ((Byte)obj).byteValue();
        }
        return false;
    }


    public int compareTo(Byte anotherByte) {
        return compare(this.value, anotherByte.value);
    }


    public static int compare(byte x, byte y) {
        return x - y;
    }


    public static int compareUnsigned(byte x, byte y) {
        return Byte.toUnsignedInt(x) - Byte.toUnsignedInt(y);
    }


    public static int toUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }


    public static long toUnsignedLong(byte x) {
        return ((long) x) & 0xffL;
    }



    public static final int SIZE = 8;


    public static final int BYTES = SIZE / Byte.SIZE;


    private static final long serialVersionUID = -7183698231559129828L;
}
