/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package java.lang.invoke;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import static java.lang.invoke.MethodHandleStatics.UNSAFE;


abstract class VarHandleByteArrayBase {
    // Buffer.address
    static final long BUFFER_ADDRESS;
    // Buffer.limit
    static final long BUFFER_LIMIT;
    // ByteBuffer.hb
    static final long BYTE_BUFFER_HB;
    // ByteBuffer.isReadOnly
    static final long BYTE_BUFFER_IS_READ_ONLY;

    static {
        try {
            BUFFER_ADDRESS = UNSAFE.objectFieldOffset(
                    Buffer.class.getDeclaredField("address"));

            BUFFER_LIMIT = UNSAFE.objectFieldOffset(
                    Buffer.class.getDeclaredField("limit"));

            BYTE_BUFFER_HB = UNSAFE.objectFieldOffset(
                    ByteBuffer.class.getDeclaredField("hb"));

            BYTE_BUFFER_IS_READ_ONLY = UNSAFE.objectFieldOffset(
                    ByteBuffer.class.getDeclaredField("isReadOnly"));
        }
        catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    static final boolean BE = UNSAFE.isBigEndian();

    static IllegalStateException newIllegalStateExceptionForMisalignedAccess(int index) {
        return new IllegalStateException("Misaligned access at index: " + index);
    }
}
