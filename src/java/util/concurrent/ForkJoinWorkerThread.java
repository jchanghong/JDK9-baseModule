/*
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
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;


public class ForkJoinWorkerThread extends Thread {
    /*
     * ForkJoinWorkerThreads are managed by ForkJoinPools and perform
     * ForkJoinTasks. For explanation, see the internal documentation
     * of class ForkJoinPool.
     *
     * This class just maintains links to its pool and WorkQueue.  The
     * pool field is set immediately upon construction, but the
     * workQueue field is not set until a call to registerWorker
     * completes. This leads to a visibility race, that is tolerated
     * by requiring that the workQueue field is only accessed by the
     * owning thread.
     *
     * Support for (non-public) subclass InnocuousForkJoinWorkerThread
     * requires that we break quite a lot of encapsulation (via helper
     * methods in ThreadLocalRandom) both here and in the subclass to
     * access and set Thread fields.
     */

    final ForkJoinPool pool;                // the pool this thread works in
    final ForkJoinPool.WorkQueue workQueue; // work-stealing mechanics


    protected ForkJoinWorkerThread(ForkJoinPool pool) {
        // Use a placeholder until a useful name can be set in registerWorker
        super("aForkJoinWorkerThread");
        this.pool = pool;
        this.workQueue = pool.registerWorker(this);
    }


    ForkJoinWorkerThread(ForkJoinPool pool, ClassLoader ccl) {
        super("aForkJoinWorkerThread");
        super.setContextClassLoader(ccl);
        this.pool = pool;
        this.workQueue = pool.registerWorker(this);
    }


    ForkJoinWorkerThread(ForkJoinPool pool,
                         ClassLoader ccl,
                         ThreadGroup threadGroup,
                         AccessControlContext acc) {
        super(threadGroup, null, "aForkJoinWorkerThread");
        super.setContextClassLoader(ccl);
        ThreadLocalRandom.setInheritedAccessControlContext(this, acc);
        ThreadLocalRandom.eraseThreadLocals(this); // clear before registering
        this.pool = pool;
        this.workQueue = pool.registerWorker(this);
    }


    public ForkJoinPool getPool() {
        return pool;
    }


    public int getPoolIndex() {
        return workQueue.getPoolIndex();
    }


    protected void onStart() {
    }


    protected void onTermination(Throwable exception) {
    }


    public void run() {
        if (workQueue.array == null) { // only run once
            Throwable exception = null;
            try {
                onStart();
                pool.runWorker(workQueue);
            } catch (Throwable ex) {
                exception = ex;
            } finally {
                try {
                    onTermination(exception);
                } catch (Throwable ex) {
                    if (exception == null)
                        exception = ex;
                } finally {
                    pool.deregisterWorker(this, exception);
                }
            }
        }
    }


    void afterTopLevelExec() {
    }


    static final class InnocuousForkJoinWorkerThread extends ForkJoinWorkerThread {

        private static final ThreadGroup innocuousThreadGroup =
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<>() {
                    public ThreadGroup run() {
                        ThreadGroup group = Thread.currentThread().getThreadGroup();
                        for (ThreadGroup p; (p = group.getParent()) != null; )
                            group = p;
                        return new ThreadGroup(group, "InnocuousForkJoinWorkerThreadGroup");
                    }});


        private static final AccessControlContext INNOCUOUS_ACC =
            new AccessControlContext(
                new ProtectionDomain[] {
                    new ProtectionDomain(null, null)
                });

        InnocuousForkJoinWorkerThread(ForkJoinPool pool) {
            super(pool,
                  ClassLoader.getSystemClassLoader(),
                  innocuousThreadGroup,
                  INNOCUOUS_ACC);
        }

        @Override // to erase ThreadLocals
        void afterTopLevelExec() {
            ThreadLocalRandom.eraseThreadLocals(this);
        }

        @Override // to silently fail
        public void setUncaughtExceptionHandler(UncaughtExceptionHandler x) { }

        @Override // paranoically
        public void setContextClassLoader(ClassLoader cl) {
            throw new SecurityException("setContextClassLoader");
        }
    }
}
