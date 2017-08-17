/*
 * Copyright (c) 1996, 2015, Oracle and/or its affiliates. All rights reserved.
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
import sun.nio.ch.DirectBuffer;

import jdk.internal.HotSpotIntrinsicCandidate;


public
class Adler32 implements Checksum {

    private int adler = 1;


    public Adler32() {
    }


    @Override
    public void update(int b) {
        adler = update(adler, b);
    }


    @Override
    public void update(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }
        adler = updateBytes(adler, b, off, len);
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
            adler = updateByteBuffer(adler, ((DirectBuffer)buffer).address(), pos, rem);
        } else if (buffer.hasArray()) {
            adler = updateBytes(adler, buffer.array(), pos + buffer.arrayOffset(), rem);
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
        adler = 1;
    }


    @Override
    public long getValue() {
        return (long)adler & 0xffffffffL;
    }

    private static native int update(int adler, int b);

    @HotSpotIntrinsicCandidate
    private static native int updateBytes(int adler, byte[] b, int off,
                                          int len);
    @HotSpotIntrinsicCandidate
    private static native int updateByteBuffer(int adler, long addr,
                                               int off, int len);
}
