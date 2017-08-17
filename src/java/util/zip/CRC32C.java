/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package java.util.zip;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jdk.internal.HotSpotIntrinsicCandidate;
import jdk.internal.misc.Unsafe;
import sun.nio.ch.DirectBuffer;


public final class CRC32C implements Checksum {

    /*
     * This CRC-32C implementation uses the 'slicing-by-8' algorithm described
     * in the paper "A Systematic Approach to Building High Performance
     * Software-Based CRC Generators" by Michael E. Kounavis and Frank L. Berry,
     * Intel Research and Development
     */


    private static final int CRC32C_POLY = 0x1EDC6F41;
    private static final int REVERSED_CRC32C_POLY = Integer.reverse(CRC32C_POLY);

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    // Lookup tables
    // Lookup table for single byte calculations
    private static final int[] byteTable;
    // Lookup tables for bulk operations in 'slicing-by-8' algorithm
    private static final int[][] byteTables = new int[8][256];
    private static final int[] byteTable0 = byteTables[0];
    private static final int[] byteTable1 = byteTables[1];
    private static final int[] byteTable2 = byteTables[2];
    private static final int[] byteTable3 = byteTables[3];
    private static final int[] byteTable4 = byteTables[4];
    private static final int[] byteTable5 = byteTables[5];
    private static final int[] byteTable6 = byteTables[6];
    private static final int[] byteTable7 = byteTables[7];

    static {
        // Generate lookup tables
        // High-order polynomial term stored in LSB of r.
        for (int index = 0; index < byteTables[0].length; index++) {
           int r = index;
            for (int i = 0; i < Byte.SIZE; i++) {
                if ((r & 1) != 0) {
                    r = (r >>> 1) ^ REVERSED_CRC32C_POLY;
                } else {
                    r >>>= 1;
                }
            }
            byteTables[0][index] = r;
        }

        for (int index = 0; index < byteTables[0].length; index++) {
            int r = byteTables[0][index];

            for (int k = 1; k < byteTables.length; k++) {
                r = byteTables[0][r & 0xFF] ^ (r >>> 8);
                byteTables[k][index] = r;
            }
        }

        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            byteTable = byteTables[0];
        } else { // ByteOrder.BIG_ENDIAN
            byteTable = new int[byteTable0.length];
            System.arraycopy(byteTable0, 0, byteTable, 0, byteTable0.length);
            for (int[] table : byteTables) {
                for (int index = 0; index < table.length; index++) {
                    table[index] = Integer.reverseBytes(table[index]);
                }
            }
        }
    }


    private int crc = 0xFFFFFFFF;


    public CRC32C() {
    }


    @Override
    public void update(int b) {
        crc = (crc >>> 8) ^ byteTable[(crc ^ (b & 0xFF)) & 0xFF];
    }


    @Override
    public void update(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }
        crc = updateBytes(crc, b, off, (off + len));
    }


    @Override
    public void update(ByteBuffer buffer) {
        int pos = buffer.position();
        int limit = buffer.limit();
        assert (pos <= limit);
        int rem = limit - pos;
        if (rem <= 0) {
            return;
        }

        if (buffer instanceof DirectBuffer) {
            crc = updateDirectByteBuffer(crc, ((DirectBuffer) buffer).address(),
                                         pos, limit);
        } else if (buffer.hasArray()) {
            crc = updateBytes(crc, buffer.array(), pos + buffer.arrayOffset(),
                              limit + buffer.arrayOffset());
        } else {
            byte[] b = new byte[Math.min(buffer.remaining(), 4096)];
            while (buffer.hasRemaining()) {
                int length = Math.min(buffer.remaining(), b.length);
                buffer.get(b, 0, length);
                update(b, 0, length);
            }
        }
        buffer.position(limit);
    }


    @Override
    public void reset() {
        crc = 0xFFFFFFFF;
    }


    @Override
    public long getValue() {
        return (~crc) & 0xFFFFFFFFL;
    }


    @HotSpotIntrinsicCandidate
    private static int updateBytes(int crc, byte[] b, int off, int end) {

        // Do only byte reads for arrays so short they can't be aligned
        // or if bytes are stored with a larger witdh than one byte.,%
        if (end - off >= 8 && Unsafe.ARRAY_BYTE_INDEX_SCALE == 1) {

            // align on 8 bytes
            int alignLength
                    = (8 - ((Unsafe.ARRAY_BYTE_BASE_OFFSET + off) & 0x7)) & 0x7;
            for (int alignEnd = off + alignLength; off < alignEnd; off++) {
                crc = (crc >>> 8) ^ byteTable[(crc ^ b[off]) & 0xFF];
            }

            if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                crc = Integer.reverseBytes(crc);
            }

            // slicing-by-8
            for (; off < (end - Long.BYTES); off += Long.BYTES) {
                int firstHalf;
                int secondHalf;
                if (Unsafe.ADDRESS_SIZE == 4) {
                    // On 32 bit platforms read two ints instead of a single 64bit long
                    firstHalf = UNSAFE.getInt(b, (long)Unsafe.ARRAY_BYTE_BASE_OFFSET + off);
                    secondHalf = UNSAFE.getInt(b, (long)Unsafe.ARRAY_BYTE_BASE_OFFSET + off
                                               + Integer.BYTES);
                } else {
                    long value = UNSAFE.getLong(b, (long)Unsafe.ARRAY_BYTE_BASE_OFFSET + off);
                    if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                        firstHalf = (int) value;
                        secondHalf = (int) (value >>> 32);
                    } else { // ByteOrder.BIG_ENDIAN
                        firstHalf = (int) (value >>> 32);
                        secondHalf = (int) value;
                    }
                }
                crc ^= firstHalf;
                if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                    crc = byteTable7[crc & 0xFF]
                            ^ byteTable6[(crc >>> 8) & 0xFF]
                            ^ byteTable5[(crc >>> 16) & 0xFF]
                            ^ byteTable4[crc >>> 24]
                            ^ byteTable3[secondHalf & 0xFF]
                            ^ byteTable2[(secondHalf >>> 8) & 0xFF]
                            ^ byteTable1[(secondHalf >>> 16) & 0xFF]
                            ^ byteTable0[secondHalf >>> 24];
                } else { // ByteOrder.BIG_ENDIAN
                    crc = byteTable0[secondHalf & 0xFF]
                            ^ byteTable1[(secondHalf >>> 8) & 0xFF]
                            ^ byteTable2[(secondHalf >>> 16) & 0xFF]
                            ^ byteTable3[secondHalf >>> 24]
                            ^ byteTable4[crc & 0xFF]
                            ^ byteTable5[(crc >>> 8) & 0xFF]
                            ^ byteTable6[(crc >>> 16) & 0xFF]
                            ^ byteTable7[crc >>> 24];
                }
            }

            if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                crc = Integer.reverseBytes(crc);
            }
        }

        // Tail
        for (; off < end; off++) {
            crc = (crc >>> 8) ^ byteTable[(crc ^ b[off]) & 0xFF];
        }

        return crc;
    }


    @HotSpotIntrinsicCandidate
    private static int updateDirectByteBuffer(int crc, long address,
                                              int off, int end) {

        // Do only byte reads for arrays so short they can't be aligned
        if (end - off >= 8) {

            // align on 8 bytes
            int alignLength = (8 - (int) ((address + off) & 0x7)) & 0x7;
            for (int alignEnd = off + alignLength; off < alignEnd; off++) {
                crc = (crc >>> 8)
                        ^ byteTable[(crc ^ UNSAFE.getByte(address + off)) & 0xFF];
            }

            if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                crc = Integer.reverseBytes(crc);
            }

            // slicing-by-8
            for (; off <= (end - Long.BYTES); off += Long.BYTES) {
                // Always reading two ints as reading a long followed by
                // shifting and casting was slower.
                int firstHalf = UNSAFE.getInt(address + off);
                int secondHalf = UNSAFE.getInt(address + off + Integer.BYTES);
                crc ^= firstHalf;
                if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                    crc = byteTable7[crc & 0xFF]
                            ^ byteTable6[(crc >>> 8) & 0xFF]
                            ^ byteTable5[(crc >>> 16) & 0xFF]
                            ^ byteTable4[crc >>> 24]
                            ^ byteTable3[secondHalf & 0xFF]
                            ^ byteTable2[(secondHalf >>> 8) & 0xFF]
                            ^ byteTable1[(secondHalf >>> 16) & 0xFF]
                            ^ byteTable0[secondHalf >>> 24];
                } else { // ByteOrder.BIG_ENDIAN
                    crc = byteTable0[secondHalf & 0xFF]
                            ^ byteTable1[(secondHalf >>> 8) & 0xFF]
                            ^ byteTable2[(secondHalf >>> 16) & 0xFF]
                            ^ byteTable3[secondHalf >>> 24]
                            ^ byteTable4[crc & 0xFF]
                            ^ byteTable5[(crc >>> 8) & 0xFF]
                            ^ byteTable6[(crc >>> 16) & 0xFF]
                            ^ byteTable7[crc >>> 24];
                }
            }

            if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                crc = Integer.reverseBytes(crc);
            }
        }

        // Tail
        for (; off < end; off++) {
            crc = (crc >>> 8)
                    ^ byteTable[(crc ^ UNSAFE.getByte(address + off)) & 0xFF];
        }

        return crc;
    }
}
