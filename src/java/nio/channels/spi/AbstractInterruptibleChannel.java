/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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
 */

package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.*;
import jdk.internal.misc.SharedSecrets;
import sun.nio.ch.Interruptible;




public abstract class AbstractInterruptibleChannel
    implements Channel, InterruptibleChannel
{

    private final Object closeLock = new Object();
    private volatile boolean closed;


    protected AbstractInterruptibleChannel() { }


    public final void close() throws IOException {
        synchronized (closeLock) {
            if (closed)
                return;
            closed = true;
            implCloseChannel();
        }
    }


    protected abstract void implCloseChannel() throws IOException;

    public final boolean isOpen() {
        return !closed;
    }


    // -- Interruption machinery --

    private Interruptible interruptor;
    private volatile Thread interrupted;


    protected final void begin() {
        if (interruptor == null) {
            interruptor = new Interruptible() {
                    public void interrupt(Thread target) {
                        synchronized (closeLock) {
                            if (closed)
                                return;
                            closed = true;
                            interrupted = target;
                            try {
                                AbstractInterruptibleChannel.this.implCloseChannel();
                            } catch (IOException x) { }
                        }
                    }};
        }
        blockedOn(interruptor);
        Thread me = Thread.currentThread();
        if (me.isInterrupted())
            interruptor.interrupt(me);
    }


    protected final void end(boolean completed)
        throws AsynchronousCloseException
    {
        blockedOn(null);
        Thread interrupted = this.interrupted;
        if (interrupted != null && interrupted == Thread.currentThread()) {
            this.interrupted = null;
            throw new ClosedByInterruptException();
        }
        if (!completed && closed)
            throw new AsynchronousCloseException();
    }


    // -- jdk.internal.misc.SharedSecrets --
    static void blockedOn(Interruptible intr) {         // package-private
        SharedSecrets.getJavaLangAccess().blockedOn(Thread.currentThread(), intr);
    }
}
