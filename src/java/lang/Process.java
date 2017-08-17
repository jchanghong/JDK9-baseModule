/*
 * Copyright (c) 1995, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;


public abstract class Process {

    public Process() {}


    public abstract OutputStream getOutputStream();


    public abstract InputStream getInputStream();


    public abstract InputStream getErrorStream();


    public abstract int waitFor() throws InterruptedException;


    public boolean waitFor(long timeout, TimeUnit unit)
        throws InterruptedException
    {
        long startTime = System.nanoTime();
        long rem = unit.toNanos(timeout);

        do {
            try {
                exitValue();
                return true;
            } catch(IllegalThreadStateException ex) {
                if (rem > 0)
                    Thread.sleep(
                        Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
            }
            rem = unit.toNanos(timeout) - (System.nanoTime() - startTime);
        } while (rem > 0);
        return false;
    }


    public abstract int exitValue();


    public abstract void destroy();


    public Process destroyForcibly() {
        destroy();
        return this;
    }


    public boolean supportsNormalTermination() {
        throw new UnsupportedOperationException(this.getClass()
                + ".supportsNormalTermination() not supported" );
    }


    public boolean isAlive() {
        try {
            exitValue();
            return false;
        } catch(IllegalThreadStateException e) {
            return true;
        }
    }


    public long pid() {
        return toHandle().pid();
    }


    public CompletableFuture<Process> onExit() {
        return CompletableFuture.supplyAsync(this::waitForInternal);
    }


    private Process waitForInternal() {
        boolean interrupted = false;
        while (true) {
            try {
                ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
                    @Override
                    public boolean block() throws InterruptedException {
                        waitFor();
                        return true;
                    }

                    @Override
                    public boolean isReleasable() {
                        return !isAlive();
                    }
                });
                break;
            } catch (InterruptedException x) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return this;
    }


    public ProcessHandle toHandle() {
        throw new UnsupportedOperationException(this.getClass()
                + ".toHandle() not supported");
    }


    public ProcessHandle.Info info() {
        return toHandle().info();
    }


    public Stream<ProcessHandle> children() {
        return toHandle().children();
    }


    public Stream<ProcessHandle> descendants() {
        return toHandle().descendants();
    }


    static class PipeInputStream extends FileInputStream {

        PipeInputStream(FileDescriptor fd) {
            super(fd);
        }

        @Override
        public long skip(long n) throws IOException {
            long remaining = n;
            int nr;

            if (n <= 0) {
                return 0;
            }

            int size = (int)Math.min(2048, remaining);
            byte[] skipBuffer = new byte[size];
            while (remaining > 0) {
                nr = read(skipBuffer, 0, (int)Math.min(size, remaining));
                if (nr < 0) {
                    break;
                }
                remaining -= nr;
            }

            return n - remaining;
        }
    }
}
