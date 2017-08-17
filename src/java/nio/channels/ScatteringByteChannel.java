/*
 * Copyright (c) 2000, 2006, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;




public interface ScatteringByteChannel
    extends ReadableByteChannel
{


    public long read(ByteBuffer[] dsts, int offset, int length)
        throws IOException;


    public long read(ByteBuffer[] dsts) throws IOException;

}
