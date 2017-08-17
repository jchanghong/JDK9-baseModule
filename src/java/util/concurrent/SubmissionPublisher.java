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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;


public class SubmissionPublisher<T> implements Flow.Publisher<T>,
                                               AutoCloseable {
    /*
     * Most mechanics are handled by BufferedSubscription. This class
     * mainly tracks subscribers and ensures sequentiality, by using
     * built-in synchronization locks across public methods. (Using
     * built-in locks works well in the most typical case in which
     * only one thread submits items).
     */


    static final int BUFFER_CAPACITY_LIMIT = 1 << 30;


    static final int roundCapacity(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n <= 0) ? 1 : // at least 1
            (n >= BUFFER_CAPACITY_LIMIT) ? BUFFER_CAPACITY_LIMIT : n + 1;
    }

    // default Executor setup; nearly the same as CompletableFuture


    private static final Executor ASYNC_POOL =
        (ForkJoinPool.getCommonPoolParallelism() > 1) ?
        ForkJoinPool.commonPool() : new ThreadPerTaskExecutor();


    private static final class ThreadPerTaskExecutor implements Executor {
        ThreadPerTaskExecutor() {}      // prevent access constructor creation
        public void execute(Runnable r) { new Thread(r).start(); }
    }


    BufferedSubscription<T> clients;


    volatile boolean closed;

    volatile Throwable closedException;

    // Parameters for constructing BufferedSubscriptions
    final Executor executor;
    final BiConsumer<? super Flow.Subscriber<? super T>, ? super Throwable> onNextHandler;
    final int maxBufferCapacity;


    public SubmissionPublisher(Executor executor, int maxBufferCapacity,
                               BiConsumer<? super Flow.Subscriber<? super T>, ? super Throwable> handler) {
        if (executor == null)
            throw new NullPointerException();
        if (maxBufferCapacity <= 0)
            throw new IllegalArgumentException("capacity must be positive");
        this.executor = executor;
        this.onNextHandler = handler;
        this.maxBufferCapacity = roundCapacity(maxBufferCapacity);
    }


    public SubmissionPublisher(Executor executor, int maxBufferCapacity) {
        this(executor, maxBufferCapacity, null);
    }


    public SubmissionPublisher() {
        this(ASYNC_POOL, Flow.defaultBufferSize(), null);
    }


    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        if (subscriber == null) throw new NullPointerException();
        BufferedSubscription<T> subscription =
            new BufferedSubscription<T>(subscriber, executor,
                                        onNextHandler, maxBufferCapacity);
        synchronized (this) {
            for (BufferedSubscription<T> b = clients, pred = null;;) {
                if (b == null) {
                    Throwable ex;
                    subscription.onSubscribe();
                    if ((ex = closedException) != null)
                        subscription.onError(ex);
                    else if (closed)
                        subscription.onComplete();
                    else if (pred == null)
                        clients = subscription;
                    else
                        pred.next = subscription;
                    break;
                }
                BufferedSubscription<T> next = b.next;
                if (b.isDisabled()) { // remove
                    b.next = null;    // detach
                    if (pred == null)
                        clients = next;
                    else
                        pred.next = next;
                }
                else if (subscriber.equals(b.subscriber)) {
                    b.onError(new IllegalStateException("Duplicate subscribe"));
                    break;
                }
                else
                    pred = b;
                b = next;
            }
        }
    }


    public int submit(T item) {
        if (item == null) throw new NullPointerException();
        int lag = 0;
        boolean complete;
        synchronized (this) {
            complete = closed;
            BufferedSubscription<T> b = clients;
            if (!complete) {
                BufferedSubscription<T> pred = null, r = null, rtail = null;
                while (b != null) {
                    BufferedSubscription<T> next = b.next;
                    int stat = b.offer(item);
                    if (stat < 0) {           // disabled
                        b.next = null;
                        if (pred == null)
                            clients = next;
                        else
                            pred.next = next;
                    }
                    else {
                        if (stat > lag)
                            lag = stat;
                        else if (stat == 0) { // place on retry list
                            b.nextRetry = null;
                            if (rtail == null)
                                r = b;
                            else
                                rtail.nextRetry = b;
                            rtail = b;
                        }
                        pred = b;
                    }
                    b = next;
                }
                while (r != null) {
                    BufferedSubscription<T> nextRetry = r.nextRetry;
                    r.nextRetry = null;
                    int stat = r.submit(item);
                    if (stat > lag)
                        lag = stat;
                    else if (stat < 0 && clients == r)
                        clients = r.next; // postpone internal unsubscribes
                    r = nextRetry;
                }
            }
        }
        if (complete)
            throw new IllegalStateException("Closed");
        else
            return lag;
    }


    public int offer(T item,
                     BiPredicate<Flow.Subscriber<? super T>, ? super T> onDrop) {
        return doOffer(0L, item, onDrop);
    }


    public int offer(T item, long timeout, TimeUnit unit,
                     BiPredicate<Flow.Subscriber<? super T>, ? super T> onDrop) {
        return doOffer(unit.toNanos(timeout), item, onDrop);
    }


    final int doOffer(long nanos, T item,
                      BiPredicate<Flow.Subscriber<? super T>, ? super T> onDrop) {
        if (item == null) throw new NullPointerException();
        int lag = 0, drops = 0;
        boolean complete;
        synchronized (this) {
            complete = closed;
            BufferedSubscription<T> b = clients;
            if (!complete) {
                BufferedSubscription<T> pred = null, r = null, rtail = null;
                while (b != null) {
                    BufferedSubscription<T> next = b.next;
                    int stat = b.offer(item);
                    if (stat < 0) {
                        b.next = null;
                        if (pred == null)
                            clients = next;
                        else
                            pred.next = next;
                    }
                    else {
                        if (stat > lag)
                            lag = stat;
                        else if (stat == 0) {
                            b.nextRetry = null;
                            if (rtail == null)
                                r = b;
                            else
                                rtail.nextRetry = b;
                            rtail = b;
                        }
                        else if (stat > lag)
                            lag = stat;
                        pred = b;
                    }
                    b = next;
                }
                while (r != null) {
                    BufferedSubscription<T> nextRetry = r.nextRetry;
                    r.nextRetry = null;
                    int stat = (nanos > 0L)
                        ? r.timedOffer(item, nanos)
                        : r.offer(item);
                    if (stat == 0 && onDrop != null &&
                        onDrop.test(r.subscriber, item))
                        stat = r.offer(item);
                    if (stat == 0)
                        ++drops;
                    else if (stat > lag)
                        lag = stat;
                    else if (stat < 0 && clients == r)
                        clients = r.next;
                    r = nextRetry;
                }
            }
        }
        if (complete)
            throw new IllegalStateException("Closed");
        else
            return (drops > 0) ? -drops : lag;
    }


    public void close() {
        if (!closed) {
            BufferedSubscription<T> b;
            synchronized (this) {
                // no need to re-check closed here
                b = clients;
                clients = null;
                closed = true;
            }
            while (b != null) {
                BufferedSubscription<T> next = b.next;
                b.next = null;
                b.onComplete();
                b = next;
            }
        }
    }


    public void closeExceptionally(Throwable error) {
        if (error == null)
            throw new NullPointerException();
        if (!closed) {
            BufferedSubscription<T> b;
            synchronized (this) {
                b = clients;
                if (!closed) {  // don't clobber racing close
                    clients = null;
                    closedException = error;
                    closed = true;
                }
            }
            while (b != null) {
                BufferedSubscription<T> next = b.next;
                b.next = null;
                b.onError(error);
                b = next;
            }
        }
    }


    public boolean isClosed() {
        return closed;
    }


    public Throwable getClosedException() {
        return closedException;
    }


    public boolean hasSubscribers() {
        boolean nonEmpty = false;
        if (!closed) {
            synchronized (this) {
                for (BufferedSubscription<T> b = clients; b != null;) {
                    BufferedSubscription<T> next = b.next;
                    if (b.isDisabled()) {
                        b.next = null;
                        b = clients = next;
                    }
                    else {
                        nonEmpty = true;
                        break;
                    }
                }
            }
        }
        return nonEmpty;
    }


    public int getNumberOfSubscribers() {
        int count = 0;
        if (!closed) {
            synchronized (this) {
                BufferedSubscription<T> pred = null, next;
                for (BufferedSubscription<T> b = clients; b != null; b = next) {
                    next = b.next;
                    if (b.isDisabled()) {
                        b.next = null;
                        if (pred == null)
                            clients = next;
                        else
                            pred.next = next;
                    }
                    else {
                        pred = b;
                        ++count;
                    }
                }
            }
        }
        return count;
    }


    public Executor getExecutor() {
        return executor;
    }


    public int getMaxBufferCapacity() {
        return maxBufferCapacity;
    }


    public List<Flow.Subscriber<? super T>> getSubscribers() {
        ArrayList<Flow.Subscriber<? super T>> subs = new ArrayList<>();
        synchronized (this) {
            BufferedSubscription<T> pred = null, next;
            for (BufferedSubscription<T> b = clients; b != null; b = next) {
                next = b.next;
                if (b.isDisabled()) {
                    b.next = null;
                    if (pred == null)
                        clients = next;
                    else
                        pred.next = next;
                }
                else
                    subs.add(b.subscriber);
            }
        }
        return subs;
    }


    public boolean isSubscribed(Flow.Subscriber<? super T> subscriber) {
        if (subscriber == null) throw new NullPointerException();
        if (!closed) {
            synchronized (this) {
                BufferedSubscription<T> pred = null, next;
                for (BufferedSubscription<T> b = clients; b != null; b = next) {
                    next = b.next;
                    if (b.isDisabled()) {
                        b.next = null;
                        if (pred == null)
                            clients = next;
                        else
                            pred.next = next;
                    }
                    else if (subscriber.equals(b.subscriber))
                        return true;
                    else
                        pred = b;
                }
            }
        }
        return false;
    }


    public long estimateMinimumDemand() {
        long min = Long.MAX_VALUE;
        boolean nonEmpty = false;
        synchronized (this) {
            BufferedSubscription<T> pred = null, next;
            for (BufferedSubscription<T> b = clients; b != null; b = next) {
                int n; long d;
                next = b.next;
                if ((n = b.estimateLag()) < 0) {
                    b.next = null;
                    if (pred == null)
                        clients = next;
                    else
                        pred.next = next;
                }
                else {
                    if ((d = b.demand - n) < min)
                        min = d;
                    nonEmpty = true;
                    pred = b;
                }
            }
        }
        return nonEmpty ? min : 0;
    }


    public int estimateMaximumLag() {
        int max = 0;
        synchronized (this) {
            BufferedSubscription<T> pred = null, next;
            for (BufferedSubscription<T> b = clients; b != null; b = next) {
                int n;
                next = b.next;
                if ((n = b.estimateLag()) < 0) {
                    b.next = null;
                    if (pred == null)
                        clients = next;
                    else
                        pred.next = next;
                }
                else {
                    if (n > max)
                        max = n;
                    pred = b;
                }
            }
        }
        return max;
    }


    public CompletableFuture<Void> consume(Consumer<? super T> consumer) {
        if (consumer == null)
            throw new NullPointerException();
        CompletableFuture<Void> status = new CompletableFuture<>();
        subscribe(new ConsumerSubscriber<T>(status, consumer));
        return status;
    }


    private static final class ConsumerSubscriber<T>
        implements Flow.Subscriber<T> {
        final CompletableFuture<Void> status;
        final Consumer<? super T> consumer;
        Flow.Subscription subscription;
        ConsumerSubscriber(CompletableFuture<Void> status,
                           Consumer<? super T> consumer) {
            this.status = status; this.consumer = consumer;
        }
        public final void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            status.whenComplete((v, e) -> subscription.cancel());
            if (!status.isDone())
                subscription.request(Long.MAX_VALUE);
        }
        public final void onError(Throwable ex) {
            status.completeExceptionally(ex);
        }
        public final void onComplete() {
            status.complete(null);
        }
        public final void onNext(T item) {
            try {
                consumer.accept(item);
            } catch (Throwable ex) {
                subscription.cancel();
                status.completeExceptionally(ex);
            }
        }
    }


    @SuppressWarnings("serial")
    static final class ConsumerTask<T> extends ForkJoinTask<Void>
        implements Runnable, CompletableFuture.AsynchronousCompletionTask {
        final BufferedSubscription<T> consumer;
        ConsumerTask(BufferedSubscription<T> consumer) {
            this.consumer = consumer;
        }
        public final Void getRawResult() { return null; }
        public final void setRawResult(Void v) {}
        public final boolean exec() { consumer.consume(); return false; }
        public final void run() { consumer.consume(); }
    }


    @SuppressWarnings("serial")
    @jdk.internal.vm.annotation.Contended
    private static final class BufferedSubscription<T>
        implements Flow.Subscription, ForkJoinPool.ManagedBlocker {
        // Order-sensitive field declarations
        long timeout;                      // > 0 if timed wait
        volatile long demand;              // # unfilled requests
        int maxCapacity;                   // reduced on OOME
        int putStat;                       // offer result for ManagedBlocker
        volatile int ctl;                  // atomic run state flags
        volatile int head;                 // next position to take
        int tail;                          // next position to put
        Object[] array;                    // buffer: null if disabled
        Flow.Subscriber<? super T> subscriber; // null if disabled
        Executor executor;                 // null if disabled
        BiConsumer<? super Flow.Subscriber<? super T>, ? super Throwable> onNextHandler;
        volatile Throwable pendingError;   // holds until onError issued
        volatile Thread waiter;            // blocked producer thread
        T putItem;                         // for offer within ManagedBlocker
        BufferedSubscription<T> next;      // used only by publisher
        BufferedSubscription<T> nextRetry; // used only by publisher

        // ctl values
        static final int ACTIVE    = 0x01; // consumer task active
        static final int CONSUME   = 0x02; // keep-alive for consumer task
        static final int DISABLED  = 0x04; // final state
        static final int ERROR     = 0x08; // signal onError then disable
        static final int SUBSCRIBE = 0x10; // signal onSubscribe
        static final int COMPLETE  = 0x20; // signal onComplete when done

        static final long INTERRUPTED = -1L; // timeout vs interrupt sentinel


        static final int DEFAULT_INITIAL_CAP = 32;

        BufferedSubscription(Flow.Subscriber<? super T> subscriber,
                             Executor executor,
                             BiConsumer<? super Flow.Subscriber<? super T>,
                             ? super Throwable> onNextHandler,
                             int maxBufferCapacity) {
            this.subscriber = subscriber;
            this.executor = executor;
            this.onNextHandler = onNextHandler;
            this.maxCapacity = maxBufferCapacity;
            this.array = new Object[maxBufferCapacity < DEFAULT_INITIAL_CAP ?
                                    (maxBufferCapacity < 2 ? // at least 2 slots
                                     2 : maxBufferCapacity) :
                                    DEFAULT_INITIAL_CAP];
        }

        final boolean isDisabled() {
            return ctl == DISABLED;
        }


        final int estimateLag() {
            int n;
            return (ctl == DISABLED) ? -1 : ((n = tail - head) > 0) ? n : 0;
        }


        final int offer(T item) {
            int h = head, t = tail, cap, size, stat;
            Object[] a = array;
            if (a != null && (cap = a.length) > 0 && cap >= (size = t + 1 - h)) {
                a[(cap - 1) & t] = item;    // relaxed writes OK
                tail = t + 1;
                stat = size;
            }
            else
                stat = growAndAdd(a, item);
            return (stat > 0 &&
                    (ctl & (ACTIVE | CONSUME)) != (ACTIVE | CONSUME)) ?
                startOnOffer(stat) : stat;
        }


        private int growAndAdd(Object[] a, T item) {
            boolean alloc;
            int cap, stat;
            if ((ctl & (ERROR | DISABLED)) != 0) {
                cap = 0;
                stat = -1;
                alloc = false;
            }
            else if (a == null || (cap = a.length) <= 0) {
                cap = 0;
                stat = 1;
                alloc = true;
            }
            else {
                VarHandle.fullFence();           // recheck
                int h = head, t = tail, size = t + 1 - h;
                if (cap >= size) {
                    a[(cap - 1) & t] = item;
                    tail = t + 1;
                    stat = size;
                    alloc = false;
                }
                else if (cap >= maxCapacity) {
                    stat = 0;                    // cannot grow
                    alloc = false;
                }
                else {
                    stat = cap + 1;
                    alloc = true;
                }
            }
            if (alloc) {
                int newCap = (cap > 0) ? cap << 1 : 1;
                if (newCap <= cap)
                    stat = 0;
                else {
                    Object[] newArray = null;
                    try {
                        newArray = new Object[newCap];
                    } catch (Throwable ex) {     // try to cope with OOME
                    }
                    if (newArray == null) {
                        if (cap > 0)
                            maxCapacity = cap;   // avoid continuous failure
                        stat = 0;
                    }
                    else {
                        array = newArray;
                        int t = tail;
                        int newMask = newCap - 1;
                        if (a != null && cap > 0) {
                            int mask = cap - 1;
                            for (int j = head; j != t; ++j) {
                                int k = j & mask;
                                Object x = QA.getAcquire(a, k);
                                if (x != null && // races with consumer
                                    QA.compareAndSet(a, k, x, null))
                                    newArray[j & newMask] = x;
                            }
                        }
                        newArray[t & newMask] = item;
                        tail = t + 1;
                    }
                }
            }
            return stat;
        }


        final int submit(T item) {
            int stat;
            if ((stat = offer(item)) == 0) {
                putItem = item;
                timeout = 0L;
                putStat = 0;
                ForkJoinPool.helpAsyncBlocker(executor, this);
                if ((stat = putStat) == 0) {
                    try {
                        ForkJoinPool.managedBlock(this);
                    } catch (InterruptedException ie) {
                        timeout = INTERRUPTED;
                    }
                    stat = putStat;
                }
                if (timeout < 0L)
                    Thread.currentThread().interrupt();
            }
            return stat;
        }


        final int timedOffer(T item, long nanos) {
            int stat;
            if ((stat = offer(item)) == 0 && (timeout = nanos) > 0L) {
                putItem = item;
                putStat = 0;
                ForkJoinPool.helpAsyncBlocker(executor, this);
                if ((stat = putStat) == 0) {
                    try {
                        ForkJoinPool.managedBlock(this);
                    } catch (InterruptedException ie) {
                        timeout = INTERRUPTED;
                    }
                    stat = putStat;
                }
                if (timeout < 0L)
                    Thread.currentThread().interrupt();
            }
            return stat;
        }


        private int startOnOffer(int stat) {
            for (;;) {
                Executor e; int c;
                if ((c = ctl) == DISABLED || (e = executor) == null) {
                    stat = -1;
                    break;
                }
                else if ((c & ACTIVE) != 0) { // ensure keep-alive
                    if ((c & CONSUME) != 0 ||
                        CTL.compareAndSet(this, c, c | CONSUME))
                        break;
                }
                else if (demand == 0L || tail == head)
                    break;
                else if (CTL.compareAndSet(this, c, c | (ACTIVE | CONSUME))) {
                    try {
                        e.execute(new ConsumerTask<T>(this));
                        break;
                    } catch (RuntimeException | Error ex) { // back out
                        do {} while (((c = ctl) & DISABLED) == 0 &&
                                     (c & ACTIVE) != 0 &&
                                     !CTL.weakCompareAndSet
                                     (this, c, c & ~ACTIVE));
                        throw ex;
                    }
                }
            }
            return stat;
        }

        private void signalWaiter(Thread w) {
            waiter = null;
            LockSupport.unpark(w);    // release producer
        }


        private void detach() {
            Thread w = waiter;
            executor = null;
            subscriber = null;
            pendingError = null;
            signalWaiter(w);
        }


        final void onError(Throwable ex) {
            for (int c;;) {
                if (((c = ctl) & (ERROR | DISABLED)) != 0)
                    break;
                else if ((c & ACTIVE) != 0) {
                    pendingError = ex;
                    if (CTL.compareAndSet(this, c, c | ERROR))
                        break; // cause consumer task to exit
                }
                else if (CTL.compareAndSet(this, c, DISABLED)) {
                    Flow.Subscriber<? super T> s = subscriber;
                    if (s != null && ex != null) {
                        try {
                            s.onError(ex);
                        } catch (Throwable ignore) {
                        }
                    }
                    detach();
                    break;
                }
            }
        }


        private void startOrDisable() {
            Executor e;
            if ((e = executor) != null) { // skip if already disabled
                try {
                    e.execute(new ConsumerTask<T>(this));
                } catch (Throwable ex) {  // back out and force signal
                    for (int c;;) {
                        if ((c = ctl) == DISABLED || (c & ACTIVE) == 0)
                            break;
                        if (CTL.compareAndSet(this, c, c & ~ACTIVE)) {
                            onError(ex);
                            break;
                        }
                    }
                }
            }
        }

        final void onComplete() {
            for (int c;;) {
                if ((c = ctl) == DISABLED)
                    break;
                if (CTL.compareAndSet(this, c,
                                      c | (ACTIVE | CONSUME | COMPLETE))) {
                    if ((c & ACTIVE) == 0)
                        startOrDisable();
                    break;
                }
            }
        }

        final void onSubscribe() {
            for (int c;;) {
                if ((c = ctl) == DISABLED)
                    break;
                if (CTL.compareAndSet(this, c,
                                      c | (ACTIVE | CONSUME | SUBSCRIBE))) {
                    if ((c & ACTIVE) == 0)
                        startOrDisable();
                    break;
                }
            }
        }


        public void cancel() {
            for (int c;;) {
                if ((c = ctl) == DISABLED)
                    break;
                else if ((c & ACTIVE) != 0) {
                    if (CTL.compareAndSet(this, c,
                                          c | (CONSUME | ERROR)))
                        break;
                }
                else if (CTL.compareAndSet(this, c, DISABLED)) {
                    detach();
                    break;
                }
            }
        }


        public void request(long n) {
            if (n > 0L) {
                for (;;) {
                    long prev = demand, d;
                    if ((d = prev + n) < prev) // saturate
                        d = Long.MAX_VALUE;
                    if (DEMAND.compareAndSet(this, prev, d)) {
                        for (int c, h;;) {
                            if ((c = ctl) == DISABLED)
                                break;
                            else if ((c & ACTIVE) != 0) {
                                if ((c & CONSUME) != 0 ||
                                    CTL.compareAndSet(this, c, c | CONSUME))
                                    break;
                            }
                            else if ((h = head) != tail) {
                                if (CTL.compareAndSet(this, c,
                                                      c | (ACTIVE|CONSUME))) {
                                    startOrDisable();
                                    break;
                                }
                            }
                            else if (head == h && tail == h)
                                break;          // else stale
                            if (demand == 0L)
                                break;
                        }
                        break;
                    }
                }
            }
            else
                onError(new IllegalArgumentException(
                            "non-positive subscription request"));
        }

        public final boolean isReleasable() { // for ManagedBlocker
            T item = putItem;
            if (item != null) {
                if ((putStat = offer(item)) == 0)
                    return false;
                putItem = null;
            }
            return true;
        }

        public final boolean block() { // for ManagedBlocker
            T item = putItem;
            if (item != null) {
                putItem = null;
                long nanos = timeout;
                long deadline = (nanos > 0L) ? System.nanoTime() + nanos : 0L;
                while ((putStat = offer(item)) == 0) {
                    if (Thread.interrupted()) {
                        timeout = INTERRUPTED;
                        if (nanos > 0L)
                            break;
                    }
                    else if (nanos > 0L &&
                             (nanos = deadline - System.nanoTime()) <= 0L)
                        break;
                    else if (waiter == null)
                        waiter = Thread.currentThread();
                    else {
                        if (nanos > 0L)
                            LockSupport.parkNanos(this, nanos);
                        else
                            LockSupport.park(this);
                        waiter = null;
                    }
                }
            }
            waiter = null;
            return true;
        }


        final void consume() {
            Flow.Subscriber<? super T> s;
            int h = head;
            if ((s = subscriber) != null) {           // else disabled
                for (;;) {
                    long d = demand;
                    int c; Object[] a; int n, i; Object x; Thread w;
                    if (((c = ctl) & (ERROR | SUBSCRIBE | DISABLED)) != 0) {
                        if (!checkControl(s, c))
                            break;
                    }
                    else if ((a = array) == null || h == tail ||
                             (n = a.length) == 0 ||
                             (x = QA.getAcquire(a, i = (n - 1) & h)) == null) {
                        if (!checkEmpty(s, c))
                            break;
                    }
                    else if (d == 0L) {
                        if (!checkDemand(c))
                            break;
                    }
                    else if (((c & CONSUME) != 0 ||
                              CTL.compareAndSet(this, c, c | CONSUME)) &&
                             QA.compareAndSet(a, i, x, null)) {
                        HEAD.setRelease(this, ++h);
                        DEMAND.getAndAdd(this, -1L);
                        if ((w = waiter) != null)
                            signalWaiter(w);
                        try {
                            @SuppressWarnings("unchecked") T y = (T) x;
                            s.onNext(y);
                        } catch (Throwable ex) {
                            handleOnNext(s, ex);
                        }
                    }
                }
            }
        }


        private boolean checkControl(Flow.Subscriber<? super T> s, int c) {
            boolean stat = true;
            if ((c & SUBSCRIBE) != 0) {
                if (CTL.compareAndSet(this, c, c & ~SUBSCRIBE)) {
                    try {
                        if (s != null)
                            s.onSubscribe(this);
                    } catch (Throwable ex) {
                        onError(ex);
                    }
                }
            }
            else if ((c & ERROR) != 0) {
                Throwable ex = pendingError;
                ctl = DISABLED;           // no need for CAS
                if (ex != null) {         // null if errorless cancel
                    try {
                        if (s != null)
                            s.onError(ex);
                    } catch (Throwable ignore) {
                    }
                }
            }
            else {
                detach();
                stat = false;
            }
            return stat;
        }


        private boolean checkEmpty(Flow.Subscriber<? super T> s, int c) {
            boolean stat = true;
            if (head == tail) {
                if ((c & CONSUME) != 0)
                    CTL.compareAndSet(this, c, c & ~CONSUME);
                else if ((c & COMPLETE) != 0) {
                    if (CTL.compareAndSet(this, c, DISABLED)) {
                        try {
                            if (s != null)
                                s.onComplete();
                        } catch (Throwable ignore) {
                        }
                    }
                }
                else if (CTL.compareAndSet(this, c, c & ~ACTIVE))
                    stat = false;
            }
            return stat;
        }


        private boolean checkDemand(int c) {
            boolean stat = true;
            if (demand == 0L) {
                if ((c & CONSUME) != 0)
                    CTL.compareAndSet(this, c, c & ~CONSUME);
                else if (CTL.compareAndSet(this, c, c & ~ACTIVE))
                    stat = false;
            }
            return stat;
        }


        private void handleOnNext(Flow.Subscriber<? super T> s, Throwable ex) {
            BiConsumer<? super Flow.Subscriber<? super T>, ? super Throwable> h;
            if ((h = onNextHandler) != null) {
                try {
                    h.accept(s, ex);
                } catch (Throwable ignore) {
                }
            }
            onError(ex);
        }

        // VarHandle mechanics
        private static final VarHandle CTL;
        private static final VarHandle TAIL;
        private static final VarHandle HEAD;
        private static final VarHandle DEMAND;
        private static final VarHandle QA;

        static {
            try {
                MethodHandles.Lookup l = MethodHandles.lookup();
                CTL = l.findVarHandle(BufferedSubscription.class, "ctl",
                                      int.class);
                TAIL = l.findVarHandle(BufferedSubscription.class, "tail",
                                       int.class);
                HEAD = l.findVarHandle(BufferedSubscription.class, "head",
                                       int.class);
                DEMAND = l.findVarHandle(BufferedSubscription.class, "demand",
                                         long.class);
                QA = MethodHandles.arrayElementVarHandle(Object[].class);
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }

            // Reduce the risk of rare disastrous classloading in first call to
            // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
            Class<?> ensureLoaded = LockSupport.class;
        }
    }
}
