/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;



public interface WatchService
    extends Closeable
{


    @Override
    void close() throws IOException;


    WatchKey poll();


    WatchKey poll(long timeout, TimeUnit unit)
        throws InterruptedException;


    WatchKey take() throws InterruptedException;
}
