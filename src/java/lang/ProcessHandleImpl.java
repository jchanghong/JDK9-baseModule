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

import java.lang.annotation.Native;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.security.AccessController.doPrivileged;


final class ProcessHandleImpl implements ProcessHandle {

    private static long REAPER_DEFAULT_STACKSIZE = 128 * 1024;


    @Native
    private static final int NOT_A_CHILD = -2;


    private static final ProcessHandleImpl current;


    private static final ConcurrentMap<Long, ExitCompletion>
            completions = new ConcurrentHashMap<>();

    static {
        initNative();
        long pid = getCurrentPid0();
        current = new ProcessHandleImpl(pid, isAlive0(pid));
    }

    private static native void initNative();


    private static final Executor processReaperExecutor =
            doPrivileged((PrivilegedAction<Executor>) () -> {

                ThreadGroup tg = Thread.currentThread().getThreadGroup();
                while (tg.getParent() != null) tg = tg.getParent();
                ThreadGroup systemThreadGroup = tg;
                final long stackSize = Boolean.getBoolean("jdk.lang.processReaperUseDefaultStackSize")
                        ? 0 : REAPER_DEFAULT_STACKSIZE;

                ThreadFactory threadFactory = grimReaper -> {
                    Thread t = new Thread(systemThreadGroup, grimReaper,
                            "process reaper", stackSize, false);
                    t.setDaemon(true);
                    // A small attempt (probably futile) to avoid priority inversion
                    t.setPriority(Thread.MAX_PRIORITY);
                    return t;
                };

                return Executors.newCachedThreadPool(threadFactory);
            });

    private static class ExitCompletion extends CompletableFuture<Integer> {
        final boolean isReaping;

        ExitCompletion(boolean isReaping) {
            this.isReaping = isReaping;
        }
    }


    static CompletableFuture<Integer> completion(long pid, boolean shouldReap) {
        // check canonicalizing cache 1st
        ExitCompletion completion = completions.get(pid);
        // re-try until we get a completion that shouldReap => isReaping
        while (completion == null || (shouldReap && !completion.isReaping)) {
            ExitCompletion newCompletion = new ExitCompletion(shouldReap);
            if (completion == null) {
                completion = completions.putIfAbsent(pid, newCompletion);
            } else {
                completion = completions.replace(pid, completion, newCompletion)
                    ? null : completions.get(pid);
            }
            if (completion == null) {
                // newCompletion has just been installed successfully
                completion = newCompletion;
                // spawn a thread to wait for and deliver the exit value
                processReaperExecutor.execute(() -> {
                    int exitValue = waitForProcessExit0(pid, shouldReap);
                    if (exitValue == NOT_A_CHILD) {
                        // pid not alive or not a child of this process
                        // If it is alive wait for it to terminate
                        long sleep = 300;     // initial milliseconds to sleep
                        int incr = 30;        // increment to the sleep time

                        long startTime = isAlive0(pid);
                        long origStart = startTime;
                        while (startTime >= 0) {
                            try {
                                Thread.sleep(Math.min(sleep, 5000L)); // no more than 5 sec
                                sleep += incr;
                            } catch (InterruptedException ie) {
                                // ignore and retry
                            }
                            startTime = isAlive0(pid);  // recheck if is alive
                            if (origStart > 0 && startTime != origStart) {
                                // start time changed, pid is not the same process
                                break;
                            }
                        }
                        exitValue = 0;
                    }
                    newCompletion.complete(exitValue);
                    // remove from cache afterwards
                    completions.remove(pid, newCompletion);
                });
            }
        }
        return completion;
    }

    @Override
    public CompletableFuture<ProcessHandle> onExit() {
        if (this.equals(current)) {
            throw new IllegalStateException("onExit for current process not allowed");
        }

        return ProcessHandleImpl.completion(pid(), false)
                .handleAsync((exitStatus, unusedThrowable) -> this);
    }


    private static native int waitForProcessExit0(long pid, boolean reapvalue);


    private final long pid;


    private final long startTime;

    /* The start time should match any value.
     * Typically, this is because the OS can not supply it.
     * The process is known to exist but not the exact start time.
     */
    private final long STARTTIME_ANY = 0L;

    /* The start time of a Process that does not exist. */
    private final long STARTTIME_PROCESS_UNKNOWN = -1;


    private ProcessHandleImpl(long pid, long startTime) {
        this.pid = pid;
        this.startTime = startTime;
    }


    static Optional<ProcessHandle> get(long pid) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
        long start = isAlive0(pid);
        return (start >= 0)
                ? Optional.of(new ProcessHandleImpl(pid, start))
                : Optional.empty();
    }


    static ProcessHandleImpl getInternal(long pid) {
        return new ProcessHandleImpl(pid, isAlive0(pid));
    }


    @Override
    public long pid() {
        return pid;
    }


    public static ProcessHandleImpl current() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
        return current;
    }


    private static native long getCurrentPid0();


    public Optional<ProcessHandle> parent() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
        long ppid = parent0(pid, startTime);
        if (ppid <= 0) {
            return Optional.empty();
        }
        return get(ppid);
    }


    private static native long parent0(long pid, long startTime);


    private static native int getProcessPids0(long pid, long[] pids,
                                              long[] ppids, long[] starttimes);


    boolean destroyProcess(boolean force) {
        if (this.equals(current)) {
            throw new IllegalStateException("destroy of current process not allowed");
        }
        return destroy0(pid, startTime, force);
    }


    private static native boolean destroy0(long pid, long startTime, boolean forcibly);

    @Override
    public boolean destroy() {
        return destroyProcess(false);
    }

    @Override
    public boolean destroyForcibly() {
        return destroyProcess(true);
    }


    @Override
    public boolean supportsNormalTermination() {
        return ProcessImpl.SUPPORTS_NORMAL_TERMINATION;
    }


    @Override
    public boolean isAlive() {
        long start = isAlive0(pid);
        return (start >= 0 && (start == startTime || start == 0 || startTime == 0));
    }


    private static native long isAlive0(long pid);

    @Override
    public Stream<ProcessHandle> children() {
        // The native OS code selects based on matching the requested parent pid.
        // If the original parent exits, the pid may have been re-used for
        // this newer process.
        // Processes started by the original parent (now dead) will all have
        // start times less than the start of this newer parent.
        // Processes started by this newer parent will have start times equal
        // or after this parent.
        return children(pid).filter(ph -> startTime <= ((ProcessHandleImpl)ph).startTime);
    }


    static Stream<ProcessHandle> children(long pid) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
        int size = 100;
        long[] childpids = null;
        long[] starttimes = null;
        while (childpids == null || size > childpids.length) {
            childpids = new long[size];
            starttimes = new long[size];
            size = getProcessPids0(pid, childpids, null, starttimes);
        }

        final long[] cpids = childpids;
        final long[] stimes = starttimes;
        return IntStream.range(0, size).mapToObj(i -> new ProcessHandleImpl(cpids[i], stimes[i]));
    }

    @Override
    public Stream<ProcessHandle> descendants() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
        int size = 100;
        long[] pids = null;
        long[] ppids = null;
        long[] starttimes = null;
        while (pids == null || size > pids.length) {
            pids = new long[size];
            ppids = new long[size];
            starttimes = new long[size];
            size = getProcessPids0(0, pids, ppids, starttimes);
        }

        int next = 0;       // index of next process to check
        int count = -1;     // count of subprocesses scanned
        long ppid = pid;    // start looking for this parent
        long ppStart = 0;
        // Find the start time of the parent
        for (int i = 0; i < size; i++) {
            if (pids[i] == ppid) {
                ppStart = starttimes[i];
                break;
            }
        }
        do {
            // Scan from next to size looking for ppid with child start time
            // the same or later than the parent.
            // If found, exchange it with index next
            for (int i = next; i < size; i++) {
                if (ppids[i] == ppid &&
                        ppStart <= starttimes[i]) {
                    swap(pids, i, next);
                    swap(ppids, i, next);
                    swap(starttimes, i, next);
                    next++;
                }
            }
            ppid = pids[++count];   // pick up the next pid to scan for
            ppStart = starttimes[count];    // and its start time
        } while (count < next);

        final long[] cpids = pids;
        final long[] stimes = starttimes;
        return IntStream.range(0, count).mapToObj(i -> new ProcessHandleImpl(cpids[i], stimes[i]));
    }

    // Swap two elements in an array
    private static void swap(long[] array, int x, int y) {
        long v = array[x];
        array[x] = array[y];
        array[y] = v;
    }

    @Override
    public ProcessHandle.Info info() {
        return ProcessHandleImpl.Info.info(pid, startTime);
    }

    @Override
    public int compareTo(ProcessHandle other) {
        return Long.compare(pid, ((ProcessHandleImpl) other).pid);
    }

    @Override
    public String toString() {
        return Long.toString(pid);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(pid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ProcessHandleImpl) {
            ProcessHandleImpl other = (ProcessHandleImpl) obj;
            return (pid == other.pid) &&
                    (startTime == other.startTime
                        || startTime == 0
                        || other.startTime == 0);
        }
        return false;
    }


    static class Info implements ProcessHandle.Info {
        static {
            initIDs();
        }


        private static native void initIDs();


        private native void info0(long pid);

        String command;
        String commandLine;
        String[] arguments;
        long startTime;
        long totalTime;
        String user;

        Info() {
            command = null;
            commandLine = null;
            arguments = null;
            startTime = -1L;
            totalTime = -1L;
            user = null;
        }


        public static ProcessHandle.Info info(long pid, long startTime) {
            Info info = new Info();
            info.info0(pid);
            if (startTime != info.startTime) {
                info.command = null;
                info.arguments = null;
                info.startTime = -1L;
                info.totalTime = -1L;
                info.user = null;
            }
            return info;
        }

        @Override
        public Optional<String> command() {
            return Optional.ofNullable(command);
        }

        @Override
        public Optional<String> commandLine() {
            if (command != null && arguments != null) {
                return Optional.of(command + " " + String.join(" ", arguments));
            } else {
                return Optional.ofNullable(commandLine);
            }
        }

        @Override
        public Optional<String[]> arguments() {
            return Optional.ofNullable(arguments);
        }

        @Override
        public Optional<Instant> startInstant() {
            return (startTime > 0)
                    ? Optional.of(Instant.ofEpochMilli(startTime))
                    : Optional.empty();
        }

        @Override
        public Optional<Duration> totalCpuDuration() {
            return (totalTime != -1)
                    ? Optional.of(Duration.ofNanos(totalTime))
                    : Optional.empty();
        }

        @Override
        public Optional<String> user() {
            return Optional.ofNullable(user);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(60);
            sb.append('[');
            if (user != null) {
                sb.append("user: ");
                sb.append(user());
            }
            if (command != null) {
                if (sb.length() != 0) sb.append(", ");
                sb.append("cmd: ");
                sb.append(command);
            }
            if (arguments != null && arguments.length > 0) {
                if (sb.length() != 0) sb.append(", ");
                sb.append("args: ");
                sb.append(Arrays.toString(arguments));
            }
            if (commandLine != null) {
                if (sb.length() != 0) sb.append(", ");
                sb.append("cmdLine: ");
                sb.append(commandLine);
            }
            if (startTime > 0) {
                if (sb.length() != 0) sb.append(", ");
                sb.append("startTime: ");
                sb.append(startInstant());
            }
            if (totalTime != -1) {
                if (sb.length() != 0) sb.append(", ");
                sb.append("totalTime: ");
                sb.append(totalCpuDuration().toString());
            }
            sb.append(']');
            return sb.toString();
        }
    }
}
