/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
package java.util.stream;

import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;


public interface IntStream extends BaseStream<Integer, IntStream> {


    IntStream filter(IntPredicate predicate);


    IntStream map(IntUnaryOperator mapper);


    <U> Stream<U> mapToObj(IntFunction<? extends U> mapper);


    LongStream mapToLong(IntToLongFunction mapper);


    DoubleStream mapToDouble(IntToDoubleFunction mapper);


    IntStream flatMap(IntFunction<? extends IntStream> mapper);


    IntStream distinct();


    IntStream sorted();


    IntStream peek(IntConsumer action);


    IntStream limit(long maxSize);


    IntStream skip(long n);


    default IntStream takeWhile(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        // Reuses the unordered spliterator, which, when encounter is present,
        // is safe to use as long as it configured not to split
        return StreamSupport.intStream(
                new WhileOps.UnorderedWhileSpliterator.OfInt.Taking(spliterator(), true, predicate),
                isParallel()).onClose(this::close);
    }


    default IntStream dropWhile(IntPredicate predicate) {
        Objects.requireNonNull(predicate);
        // Reuses the unordered spliterator, which, when encounter is present,
        // is safe to use as long as it configured not to split
        return StreamSupport.intStream(
                new WhileOps.UnorderedWhileSpliterator.OfInt.Dropping(spliterator(), true, predicate),
                isParallel()).onClose(this::close);
    }


    void forEach(IntConsumer action);


    void forEachOrdered(IntConsumer action);


    int[] toArray();


    int reduce(int identity, IntBinaryOperator op);


    OptionalInt reduce(IntBinaryOperator op);


    <R> R collect(Supplier<R> supplier,
                  ObjIntConsumer<R> accumulator,
                  BiConsumer<R, R> combiner);


    int sum();


    OptionalInt min();


    OptionalInt max();


    long count();


    OptionalDouble average();


    IntSummaryStatistics summaryStatistics();


    boolean anyMatch(IntPredicate predicate);


    boolean allMatch(IntPredicate predicate);


    boolean noneMatch(IntPredicate predicate);


    OptionalInt findFirst();


    OptionalInt findAny();


    LongStream asLongStream();


    DoubleStream asDoubleStream();


    Stream<Integer> boxed();

    @Override
    IntStream sequential();

    @Override
    IntStream parallel();

    @Override
    PrimitiveIterator.OfInt iterator();

    @Override
    Spliterator.OfInt spliterator();

    // Static factories


    public static Builder builder() {
        return new Streams.IntStreamBuilderImpl();
    }


    public static IntStream empty() {
        return StreamSupport.intStream(Spliterators.emptyIntSpliterator(), false);
    }


    public static IntStream of(int t) {
        return StreamSupport.intStream(new Streams.IntStreamBuilderImpl(t), false);
    }


    public static IntStream of(int... values) {
        return Arrays.stream(values);
    }


    public static IntStream iterate(final int seed, final IntUnaryOperator f) {
        Objects.requireNonNull(f);
        Spliterator.OfInt spliterator = new Spliterators.AbstractIntSpliterator(Long.MAX_VALUE,
               Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL) {
            int prev;
            boolean started;

            @Override
            public boolean tryAdvance(IntConsumer action) {
                Objects.requireNonNull(action);
                int t;
                if (started)
                    t = f.applyAsInt(prev);
                else {
                    t = seed;
                    started = true;
                }
                action.accept(prev = t);
                return true;
            }
        };
        return StreamSupport.intStream(spliterator, false);
    }


    public static IntStream iterate(int seed, IntPredicate hasNext, IntUnaryOperator next) {
        Objects.requireNonNull(next);
        Objects.requireNonNull(hasNext);
        Spliterator.OfInt spliterator = new Spliterators.AbstractIntSpliterator(Long.MAX_VALUE,
               Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL) {
            int prev;
            boolean started, finished;

            @Override
            public boolean tryAdvance(IntConsumer action) {
                Objects.requireNonNull(action);
                if (finished)
                    return false;
                int t;
                if (started)
                    t = next.applyAsInt(prev);
                else {
                    t = seed;
                    started = true;
                }
                if (!hasNext.test(t)) {
                    finished = true;
                    return false;
                }
                action.accept(prev = t);
                return true;
            }

            @Override
            public void forEachRemaining(IntConsumer action) {
                Objects.requireNonNull(action);
                if (finished)
                    return;
                finished = true;
                int t = started ? next.applyAsInt(prev) : seed;
                while (hasNext.test(t)) {
                    action.accept(t);
                    t = next.applyAsInt(t);
                }
            }
        };
        return StreamSupport.intStream(spliterator, false);
    }


    public static IntStream generate(IntSupplier s) {
        Objects.requireNonNull(s);
        return StreamSupport.intStream(
                new StreamSpliterators.InfiniteSupplyingSpliterator.OfInt(Long.MAX_VALUE, s), false);
    }


    public static IntStream range(int startInclusive, int endExclusive) {
        if (startInclusive >= endExclusive) {
            return empty();
        } else {
            return StreamSupport.intStream(
                    new Streams.RangeIntSpliterator(startInclusive, endExclusive, false), false);
        }
    }


    public static IntStream rangeClosed(int startInclusive, int endInclusive) {
        if (startInclusive > endInclusive) {
            return empty();
        } else {
            return StreamSupport.intStream(
                    new Streams.RangeIntSpliterator(startInclusive, endInclusive, true), false);
        }
    }


    public static IntStream concat(IntStream a, IntStream b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);

        Spliterator.OfInt split = new Streams.ConcatSpliterator.OfInt(
                a.spliterator(), b.spliterator());
        IntStream stream = StreamSupport.intStream(split, a.isParallel() || b.isParallel());
        return stream.onClose(Streams.composedClose(a, b));
    }


    public interface Builder extends IntConsumer {


        @Override
        void accept(int t);


        default Builder add(int t) {
            accept(t);
            return this;
        }


        IntStream build();
    }
}
