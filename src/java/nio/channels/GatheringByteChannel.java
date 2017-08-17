/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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




public interface GatheringByteChannel
    extends WritableByteChannel
{


    public long write(ByteBuffer[] srcs, int offset, int length)
        throws IOException;



    public long write(ByteBuffer[] srcs) throws IOException;

}
