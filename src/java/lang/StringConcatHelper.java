/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 */

package java.lang;


final class StringConcatHelper {

    private StringConcatHelper() {
        // no instantiation
    }


    private static int checkOverflow(int len) {
        if (len < 0) {
            throw new OutOfMemoryError("Overflow: String length out of range");
        }
        return len;
    }


    static int mixLen(int current, boolean value) {
        return checkOverflow(current + (value ? 4 : 5));
    }


    static int mixLen(int current, byte value) {
        return mixLen(current, (int)value);
    }


    static int mixLen(int current, char value) {
        return checkOverflow(current + 1);
    }


    static int mixLen(int current, short value) {
        return mixLen(current, (int)value);
    }


    static int mixLen(int current, int value) {
        return checkOverflow(current + Integer.stringSize(value));
    }


    static int mixLen(int current, long value) {
        return checkOverflow(current + Long.stringSize(value));
    }


    static int mixLen(int current, String value) {
        return checkOverflow(current + value.length());
    }


    static byte mixCoder(byte current, char value) {
        return (byte)(current | (StringLatin1.canEncode(value) ? 0 : 1));
    }


    static byte mixCoder(byte current, String value) {
        return (byte)(current | value.coder());
    }


    static byte mixCoder(byte current, boolean value) {
        // Booleans are represented with Latin1
        return current;
    }


    static byte mixCoder(byte current, byte value) {
        // Bytes are represented with Latin1
        return current;
    }


    static byte mixCoder(byte current, short value) {
        // Shorts are represented with Latin1
        return current;
    }


    static byte mixCoder(byte current, int value) {
        // Ints are represented with Latin1
        return current;
    }


    static byte mixCoder(byte current, long value) {
        // Longs are represented with Latin1
        return current;
    }


    static int prepend(int index, byte[] buf, byte coder, boolean value) {
        if (coder == String.LATIN1) {
            if (value) {
                buf[--index] = 'e';
                buf[--index] = 'u';
                buf[--index] = 'r';
                buf[--index] = 't';
            } else {
                buf[--index] = 'e';
                buf[--index] = 's';
                buf[--index] = 'l';
                buf[--index] = 'a';
                buf[--index] = 'f';
            }
        } else {
            if (value) {
                StringUTF16.putChar(buf, --index, 'e');
                StringUTF16.putChar(buf, --index, 'u');
                StringUTF16.putChar(buf, --index, 'r');
                StringUTF16.putChar(buf, --index, 't');
            } else {
                StringUTF16.putChar(buf, --index, 'e');
                StringUTF16.putChar(buf, --index, 's');
                StringUTF16.putChar(buf, --index, 'l');
                StringUTF16.putChar(buf, --index, 'a');
                StringUTF16.putChar(buf, --index, 'f');
            }
        }
        return index;
    }


    static int prepend(int index, byte[] buf, byte coder, byte value) {
        return prepend(index, buf, coder, (int)value);
    }


    static int prepend(int index, byte[] buf, byte coder, char value) {
        if (coder == String.LATIN1) {
            buf[--index] = (byte) (value & 0xFF);
        } else {
            StringUTF16.putChar(buf, --index, value);
        }
        return index;
    }


    static int prepend(int index, byte[] buf, byte coder, short value) {
        return prepend(index, buf, coder, (int)value);
    }


    static int prepend(int index, byte[] buf, byte coder, int value) {
        if (coder == String.LATIN1) {
            return Integer.getChars(value, index, buf);
        } else {
            return StringUTF16.getChars(value, index, buf);
        }
    }


    static int prepend(int index, byte[] buf, byte coder, long value) {
        if (coder == String.LATIN1) {
            return Long.getChars(value, index, buf);
        } else {
            return StringUTF16.getChars(value, index, buf);
        }
    }


    static int prepend(int index, byte[] buf, byte coder, String value) {
        index -= value.length();
        value.getBytes(buf, index, coder);
        return index;
    }


    static String newString(byte[] buf, int index, byte coder) {
        // Use the private, non-copying constructor (unsafe!)
        if (index != 0) {
            throw new InternalError("Storage is not completely initialized, " + index + " bytes left");
        }
        return new String(buf, coder);
    }


    static byte initialCoder() {
        return String.COMPACT_STRINGS ? String.LATIN1 : String.UTF16;
    }

}
