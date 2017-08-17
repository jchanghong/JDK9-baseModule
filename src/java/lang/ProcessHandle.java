/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;


public interface ProcessHandle extends Comparable<ProcessHandle> {


    long pid();


    public static Optional<ProcessHandle> of(long pid) {
        return ProcessHandleImpl.get(pid);
    }


    public static ProcessHandle current() {
        return ProcessHandleImpl.current();
    }


    Optional<ProcessHandle> parent();


    Stream<ProcessHandle> children();


    Stream<ProcessHandle> descendants();


    static Stream<ProcessHandle> allProcesses() {
        return ProcessHandleImpl.children(0);
    }


    Info info();


    public interface Info {

        public Optional<String> command();


        public Optional<String> commandLine();


        public Optional<String[]> arguments();


        public Optional<Instant> startInstant();


        public Optional<Duration> totalCpuDuration();


        public Optional<String> user();
    }


    CompletableFuture<ProcessHandle> onExit();


    boolean supportsNormalTermination();


    boolean destroy();


    boolean destroyForcibly();


    boolean isAlive();


    @Override
    int hashCode();


    @Override
    boolean equals(Object other);


    @Override
    int compareTo(ProcessHandle other);

}
