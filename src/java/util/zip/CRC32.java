/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Objects;

import sun.nio.ch.DirectBuffer;
import jdk.internal.HotSpotIntrinsicCandidate;


public
class CRC32 implements Checksum {
    private int crc;


    public CRC32() {
    }



    @Override
    public void update(int b) {
        crc = update(crc, b);
    }


    @Override
    public void update(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }
        crc = updateBytes(crc, b, off, len);
    }


    @Override
    public void update(ByteBuffer buffer) {
        int pos = buffer.position();
        int limit = buffer.limit();
        assert (pos <= limit);
        int rem = limit - pos;
        if (rem <= 0)
            return;
        if (buffer instanceof DirectBuffer) {
            crc = updateByteBuffer(crc, ((DirectBuffer)buffer).address(), pos, rem);
        } else if (buffer.hasArray()) {
            crc = updateBytes(crc, buffer.array(), pos + buffer.arrayOffset(), rem);
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
        crc = 0;
    }


    @Override
    public long getValue() {
        return (long)crc & 0xffffffffL;
    }

    @HotSpotIntrinsicCandidate
    private static native int update(int crc, int b);

    private static int updateBytes(int crc, byte[] b, int off, int len) {
        updateBytesCheck(b, off, len);
        return updateBytes0(crc, b, off, len);
    }

    @HotSpotIntrinsicCandidate
    private static native int updateBytes0(int crc, byte[] b, int off, int len);

    private static void updateBytesCheck(byte[] b, int off, int len) {
        if (len <= 0) {
            return;  // not an error because updateBytesImpl won't execute if len <= 0
        }

        Objects.requireNonNull(b);

        if (off < 0 || off >= b.length) {
            throw new ArrayIndexOutOfBoundsException(off);
        }

        int endIndex = off + len - 1;
        if (endIndex < 0 || endIndex >= b.length) {
            throw new ArrayIndexOutOfBoundsException(endIndex);
        }
    }

    private static int updateByteBuffer(int alder, long addr,
                                        int off, int len) {
        updateByteBufferCheck(addr);
        return updateByteBuffer0(alder, addr, off, len);
    }

    @HotSpotIntrinsicCandidate
    private static native int updateByteBuffer0(int alder, long addr,
                                                int off, int len);

    private static void updateByteBufferCheck(long addr) {
        // Performs only a null check because bounds checks
        // are not easy to do on raw addresses.
        if (addr == 0L) {
            throw new NullPointerException();
        }
    }
}
