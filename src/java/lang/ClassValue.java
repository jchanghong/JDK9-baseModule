/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.WeakHashMap;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.ClassValue.ClassValueMap.probeHomeLocation;
import static java.lang.ClassValue.ClassValueMap.probeBackupLocations;


public abstract class ClassValue<T> {

    protected ClassValue() {
    }


    protected abstract T computeValue(Class<?> type);


    public T get(Class<?> type) {
        // non-racing this.hashCodeForCache : final int
        Entry<?>[] cache;
        Entry<T> e = probeHomeLocation(cache = getCacheCarefully(type), this);
        // racing e : current value <=> stale value from current cache or from stale cache
        // invariant:  e is null or an Entry with readable Entry.version and Entry.value
        if (match(e))
            // invariant:  No false positive matches.  False negatives are OK if rare.
            // The key fact that makes this work: if this.version == e.version,
            // then this thread has a right to observe (final) e.value.
            return e.value();
        // The fast path can fail for any of these reasons:
        // 1. no entry has been computed yet
        // 2. hash code collision (before or after reduction mod cache.length)
        // 3. an entry has been removed (either on this type or another)
        // 4. the GC has somehow managed to delete e.version and clear the reference
        return getFromBackup(cache, type);
    }


    public void remove(Class<?> type) {
        ClassValueMap map = getMap(type);
        map.removeEntry(this);
    }

    // Possible functionality for JSR 292 MR 1
    /*public*/ void put(Class<?> type, T value) {
        ClassValueMap map = getMap(type);
        map.changeEntry(this, value);
    }

    /// --------
    /// Implementation...
    /// --------


    private static Entry<?>[] getCacheCarefully(Class<?> type) {
        // racing type.classValueMap{.cacheArray} : null => new Entry[X] <=> new Entry[Y]
        ClassValueMap map = type.classValueMap;
        if (map == null)  return EMPTY_CACHE;
        Entry<?>[] cache = map.getCache();
        return cache;
        // invariant:  returned value is safe to dereference and check for an Entry
    }


    private static final Entry<?>[] EMPTY_CACHE = { null };


    private T getFromBackup(Entry<?>[] cache, Class<?> type) {
        Entry<T> e = probeBackupLocations(cache, this);
        if (e != null)
            return e.value();
        return getFromHashMap(type);
    }

    // Hack to suppress warnings on the (T) cast, which is a no-op.
    @SuppressWarnings("unchecked")
    Entry<T> castEntry(Entry<?> e) { return (Entry<T>) e; }


    private T getFromHashMap(Class<?> type) {
        // The fail-safe recovery is to fall back to the underlying classValueMap.
        ClassValueMap map = getMap(type);
        for (;;) {
            Entry<T> e = map.startEntry(this);
            if (!e.isPromise())
                return e.value();
            try {
                // Try to make a real entry for the promised version.
                e = makeEntry(e.version(), computeValue(type));
            } finally {
                // Whether computeValue throws or returns normally,
                // be sure to remove the empty entry.
                e = map.finishEntry(this, e);
            }
            if (e != null)
                return e.value();
            // else try again, in case a racing thread called remove (so e == null)
        }
    }


    boolean match(Entry<?> e) {
        // racing e.version : null (blank) => unique Version token => null (GC-ed version)
        // non-racing this.version : v1 => v2 => ... (updates are read faithfully from volatile)
        return (e != null && e.get() == this.version);
        // invariant:  No false positives on version match.  Null is OK for false negative.
        // invariant:  If version matches, then e.value is readable (final set in Entry.<init>)
    }


    final int hashCodeForCache = nextHashCode.getAndAdd(HASH_INCREMENT) & HASH_MASK;


    private static final AtomicInteger nextHashCode = new AtomicInteger();


    private static final int HASH_INCREMENT = 0x61c88647;


    static final int HASH_MASK = (-1 >>> 2);


    static class Identity {
    }

    final Identity identity = new Identity();


    private volatile Version<T> version = new Version<>(this);
    Version<T> version() { return version; }
    void bumpVersion() { version = new Version<>(this); }
    static class Version<T> {
        private final ClassValue<T> classValue;
        private final Entry<T> promise = new Entry<>(this);
        Version(ClassValue<T> classValue) { this.classValue = classValue; }
        ClassValue<T> classValue() { return classValue; }
        Entry<T> promise() { return promise; }
        boolean isLive() { return classValue.version() == this; }
    }


    static class Entry<T> extends WeakReference<Version<T>> {
        final Object value;  // usually of type T, but sometimes (Entry)this
        Entry(Version<T> version, T value) {
            super(version);
            this.value = value;  // for a regular entry, value is of type T
        }
        private void assertNotPromise() { assert(!isPromise()); }

        Entry(Version<T> version) {
            super(version);
            this.value = this;  // for a promise, value is not of type T, but Entry!
        }

        @SuppressWarnings("unchecked")  // if !isPromise, type is T
        T value() { assertNotPromise(); return (T) value; }
        boolean isPromise() { return value == this; }
        Version<T> version() { return get(); }
        ClassValue<T> classValueOrNull() {
            Version<T> v = version();
            return (v == null) ? null : v.classValue();
        }
        boolean isLive() {
            Version<T> v = version();
            if (v == null)  return false;
            if (v.isLive())  return true;
            clear();
            return false;
        }
        Entry<T> refreshVersion(Version<T> v2) {
            assertNotPromise();
            @SuppressWarnings("unchecked")  // if !isPromise, type is T
            Entry<T> e2 = new Entry<>(v2, (T) value);
            clear();
            // value = null -- caller must drop
            return e2;
        }
        static final Entry<?> DEAD_ENTRY = new Entry<>(null, null);
    }


    private static ClassValueMap getMap(Class<?> type) {
        // racing type.classValueMap : null (blank) => unique ClassValueMap
        // if a null is observed, a map is created (lazily, synchronously, uniquely)
        // all further access to that map is synchronized
        ClassValueMap map = type.classValueMap;
        if (map != null)  return map;
        return initializeMap(type);
    }

    private static final Object CRITICAL_SECTION = new Object();
    private static ClassValueMap initializeMap(Class<?> type) {
        ClassValueMap map;
        synchronized (CRITICAL_SECTION) {  // private object to avoid deadlocks
            // happens about once per type
            if ((map = type.classValueMap) == null)
                type.classValueMap = map = new ClassValueMap();
        }
        return map;
    }

    static <T> Entry<T> makeEntry(Version<T> explicitVersion, T value) {
        // Note that explicitVersion might be different from this.version.
        return new Entry<>(explicitVersion, value);

        // As soon as the Entry is put into the cache, the value will be
        // reachable via a data race (as defined by the Java Memory Model).
        // This race is benign, assuming the value object itself can be
        // read safely by multiple threads.  This is up to the user.
        //
        // The entry and version fields themselves can be safely read via
        // a race because they are either final or have controlled states.
        // If the pointer from the entry to the version is still null,
        // or if the version goes immediately dead and is nulled out,
        // the reader will take the slow path and retry under a lock.
    }

    // The following class could also be top level and non-public:


    static class ClassValueMap extends WeakHashMap<ClassValue.Identity, Entry<?>> {
        private Entry<?>[] cacheArray;
        private int cacheLoad, cacheLoadLimit;


        private static final int INITIAL_ENTRIES = 32;


        ClassValueMap() {
            sizeCache(INITIAL_ENTRIES);
        }

        Entry<?>[] getCache() { return cacheArray; }


        synchronized
        <T> Entry<T> startEntry(ClassValue<T> classValue) {
            @SuppressWarnings("unchecked")  // one map has entries for all value types <T>
            Entry<T> e = (Entry<T>) get(classValue.identity);
            Version<T> v = classValue.version();
            if (e == null) {
                e = v.promise();
                // The presence of a promise means that a value is pending for v.
                // Eventually, finishEntry will overwrite the promise.
                put(classValue.identity, e);
                // Note that the promise is never entered into the cache!
                return e;
            } else if (e.isPromise()) {
                // Somebody else has asked the same question.
                // Let the races begin!
                if (e.version() != v) {
                    e = v.promise();
                    put(classValue.identity, e);
                }
                return e;
            } else {
                // there is already a completed entry here; report it
                if (e.version() != v) {
                    // There is a stale but valid entry here; make it fresh again.
                    // Once an entry is in the hash table, we don't care what its version is.
                    e = e.refreshVersion(v);
                    put(classValue.identity, e);
                }
                // Add to the cache, to enable the fast path, next time.
                checkCacheLoad();
                addToCache(classValue, e);
                return e;
            }
        }


        synchronized
        <T> Entry<T> finishEntry(ClassValue<T> classValue, Entry<T> e) {
            @SuppressWarnings("unchecked")  // one map has entries for all value types <T>
            Entry<T> e0 = (Entry<T>) get(classValue.identity);
            if (e == e0) {
                // We can get here during exception processing, unwinding from computeValue.
                assert(e.isPromise());
                remove(classValue.identity);
                return null;
            } else if (e0 != null && e0.isPromise() && e0.version() == e.version()) {
                // If e0 matches the intended entry, there has not been a remove call
                // between the previous startEntry and now.  So now overwrite e0.
                Version<T> v = classValue.version();
                if (e.version() != v)
                    e = e.refreshVersion(v);
                put(classValue.identity, e);
                // Add to the cache, to enable the fast path, next time.
                checkCacheLoad();
                addToCache(classValue, e);
                return e;
            } else {
                // Some sort of mismatch; caller must try again.
                return null;
            }
        }


        synchronized
        void removeEntry(ClassValue<?> classValue) {
            Entry<?> e = remove(classValue.identity);
            if (e == null) {
                // Uninitialized, and no pending calls to computeValue.  No change.
            } else if (e.isPromise()) {
                // State is uninitialized, with a pending call to finishEntry.
                // Since remove is a no-op in such a state, keep the promise
                // by putting it back into the map.
                put(classValue.identity, e);
            } else {
                // In an initialized state.  Bump forward, and de-initialize.
                classValue.bumpVersion();
                // Make all cache elements for this guy go stale.
                removeStaleEntries(classValue);
            }
        }


        synchronized
        <T> void changeEntry(ClassValue<T> classValue, T value) {
            @SuppressWarnings("unchecked")  // one map has entries for all value types <T>
            Entry<T> e0 = (Entry<T>) get(classValue.identity);
            Version<T> version = classValue.version();
            if (e0 != null) {
                if (e0.version() == version && e0.value() == value)
                    // no value change => no version change needed
                    return;
                classValue.bumpVersion();
                removeStaleEntries(classValue);
            }
            Entry<T> e = makeEntry(version, value);
            put(classValue.identity, e);
            // Add to the cache, to enable the fast path, next time.
            checkCacheLoad();
            addToCache(classValue, e);
        }

        /// --------
        /// Cache management.
        /// --------

        // Statics do not need synchronization.


        static Entry<?> loadFromCache(Entry<?>[] cache, int i) {
            // non-racing cache.length : constant
            // racing cache[i & (mask)] : null <=> Entry
            return cache[i & (cache.length-1)];
            // invariant:  returned value is null or well-constructed (ready to match)
        }


        static <T> Entry<T> probeHomeLocation(Entry<?>[] cache, ClassValue<T> classValue) {
            return classValue.castEntry(loadFromCache(cache, classValue.hashCodeForCache));
        }


        static <T> Entry<T> probeBackupLocations(Entry<?>[] cache, ClassValue<T> classValue) {
            if (PROBE_LIMIT <= 0)  return null;
            // Probe the cache carefully, in a range of slots.
            int mask = (cache.length-1);
            int home = (classValue.hashCodeForCache & mask);
            Entry<?> e2 = cache[home];  // victim, if we find the real guy
            if (e2 == null) {
                return null;   // if nobody is at home, no need to search nearby
            }
            // assume !classValue.match(e2), but do not assert, because of races
            int pos2 = -1;
            for (int i = home + 1; i < home + PROBE_LIMIT; i++) {
                Entry<?> e = cache[i & mask];
                if (e == null) {
                    break;   // only search within non-null runs
                }
                if (classValue.match(e)) {
                    // relocate colliding entry e2 (from cache[home]) to first empty slot
                    cache[home] = e;
                    if (pos2 >= 0) {
                        cache[i & mask] = Entry.DEAD_ENTRY;
                    } else {
                        pos2 = i;
                    }
                    cache[pos2 & mask] = ((entryDislocation(cache, pos2, e2) < PROBE_LIMIT)
                                          ? e2                  // put e2 here if it fits
                                          : Entry.DEAD_ENTRY);
                    return classValue.castEntry(e);
                }
                // Remember first empty slot, if any:
                if (!e.isLive() && pos2 < 0)  pos2 = i;
            }
            return null;
        }


        private static int entryDislocation(Entry<?>[] cache, int pos, Entry<?> e) {
            ClassValue<?> cv = e.classValueOrNull();
            if (cv == null)  return 0;  // entry is not live!
            int mask = (cache.length-1);
            return (pos - cv.hashCodeForCache) & mask;
        }

        /// --------
        /// Below this line all functions are private, and assume synchronized access.
        /// --------

        private void sizeCache(int length) {
            assert((length & (length-1)) == 0);  // must be power of 2
            cacheLoad = 0;
            cacheLoadLimit = (int) ((double) length * CACHE_LOAD_LIMIT / 100);
            cacheArray = new Entry<?>[length];
        }


        private void checkCacheLoad() {
            if (cacheLoad >= cacheLoadLimit) {
                reduceCacheLoad();
            }
        }
        private void reduceCacheLoad() {
            removeStaleEntries();
            if (cacheLoad < cacheLoadLimit)
                return;  // win
            Entry<?>[] oldCache = getCache();
            if (oldCache.length > HASH_MASK)
                return;  // lose
            sizeCache(oldCache.length * 2);
            for (Entry<?> e : oldCache) {
                if (e != null && e.isLive()) {
                    addToCache(e);
                }
            }
        }


        private void removeStaleEntries(Entry<?>[] cache, int begin, int count) {
            if (PROBE_LIMIT <= 0)  return;
            int mask = (cache.length-1);
            int removed = 0;
            for (int i = begin; i < begin + count; i++) {
                Entry<?> e = cache[i & mask];
                if (e == null || e.isLive())
                    continue;  // skip null and live entries
                Entry<?> replacement = null;
                if (PROBE_LIMIT > 1) {
                    // avoid breaking up a non-null run
                    replacement = findReplacement(cache, i);
                }
                cache[i & mask] = replacement;
                if (replacement == null)  removed += 1;
            }
            cacheLoad = Math.max(0, cacheLoad - removed);
        }


        private Entry<?> findReplacement(Entry<?>[] cache, int home1) {
            Entry<?> replacement = null;
            int haveReplacement = -1, replacementPos = 0;
            int mask = (cache.length-1);
            for (int i2 = home1 + 1; i2 < home1 + PROBE_LIMIT; i2++) {
                Entry<?> e2 = cache[i2 & mask];
                if (e2 == null)  break;  // End of non-null run.
                if (!e2.isLive())  continue;  // Doomed anyway.
                int dis2 = entryDislocation(cache, i2, e2);
                if (dis2 == 0)  continue;  // e2 already optimally placed
                int home2 = i2 - dis2;
                if (home2 <= home1) {
                    // e2 can replace entry at cache[home1]
                    if (home2 == home1) {
                        // Put e2 exactly where he belongs.
                        haveReplacement = 1;
                        replacementPos = i2;
                        replacement = e2;
                    } else if (haveReplacement <= 0) {
                        haveReplacement = 0;
                        replacementPos = i2;
                        replacement = e2;
                    }
                    // And keep going, so we can favor larger dislocations.
                }
            }
            if (haveReplacement >= 0) {
                if (cache[(replacementPos+1) & mask] != null) {
                    // Be conservative, to avoid breaking up a non-null run.
                    cache[replacementPos & mask] = (Entry<?>) Entry.DEAD_ENTRY;
                } else {
                    cache[replacementPos & mask] = null;
                    cacheLoad -= 1;
                }
            }
            return replacement;
        }


        private void removeStaleEntries(ClassValue<?> classValue) {
            removeStaleEntries(getCache(), classValue.hashCodeForCache, PROBE_LIMIT);
        }


        private void removeStaleEntries() {
            Entry<?>[] cache = getCache();
            removeStaleEntries(cache, 0, cache.length + PROBE_LIMIT - 1);
        }


        private <T> void addToCache(Entry<T> e) {
            ClassValue<T> classValue = e.classValueOrNull();
            if (classValue != null)
                addToCache(classValue, e);
        }


        private <T> void addToCache(ClassValue<T> classValue, Entry<T> e) {
            if (PROBE_LIMIT <= 0)  return;  // do not fill cache
            // Add e to the cache.
            Entry<?>[] cache = getCache();
            int mask = (cache.length-1);
            int home = classValue.hashCodeForCache & mask;
            Entry<?> e2 = placeInCache(cache, home, e, false);
            if (e2 == null)  return;  // done
            if (PROBE_LIMIT > 1) {
                // try to move e2 somewhere else in his probe range
                int dis2 = entryDislocation(cache, home, e2);
                int home2 = home - dis2;
                for (int i2 = home2; i2 < home2 + PROBE_LIMIT; i2++) {
                    if (placeInCache(cache, i2 & mask, e2, true) == null) {
                        return;
                    }
                }
            }
            // Note:  At this point, e2 is just dropped from the cache.
        }


        private Entry<?> placeInCache(Entry<?>[] cache, int pos, Entry<?> e, boolean gently) {
            Entry<?> e2 = overwrittenEntry(cache[pos]);
            if (gently && e2 != null) {
                // do not overwrite a live entry
                return e;
            } else {
                cache[pos] = e;
                return e2;
            }
        }


        private <T> Entry<T> overwrittenEntry(Entry<T> e2) {
            if (e2 == null)  cacheLoad += 1;
            else if (e2.isLive())  return e2;
            return null;
        }


        private static final int CACHE_LOAD_LIMIT = 67;  // 0..100

        private static final int PROBE_LIMIT      =  6;       // 1..
        // N.B.  Set PROBE_LIMIT=0 to disable all fast paths.
    }
}
