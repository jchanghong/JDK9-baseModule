/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
package java.util;

import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.LongStream;


public final class OptionalLong {

    private static final OptionalLong EMPTY = new OptionalLong();


    private final boolean isPresent;
    private final long value;


    private OptionalLong() {
        this.isPresent = false;
        this.value = 0;
    }


    public static OptionalLong empty() {
        return EMPTY;
    }


    private OptionalLong(long value) {
        this.isPresent = true;
        this.value = value;
    }


    public static OptionalLong of(long value) {
        return new OptionalLong(value);
    }


    public long getAsLong() {
        if (!isPresent) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }


    public boolean isPresent() {
        return isPresent;
    }


    public void ifPresent(LongConsumer action) {
        if (isPresent) {
            action.accept(value);
        }
    }


    public void ifPresentOrElse(LongConsumer action, Runnable emptyAction) {
        if (isPresent) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }


    public LongStream stream() {
        if (isPresent) {
            return LongStream.of(value);
        } else {
            return LongStream.empty();
        }
    }


    public long orElse(long other) {
        return isPresent ? value : other;
    }


    public long orElseGet(LongSupplier supplier) {
        return isPresent ? value : supplier.getAsLong();
    }


    public<X extends Throwable> long orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (isPresent) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof OptionalLong)) {
            return false;
        }

        OptionalLong other = (OptionalLong) obj;
        return (isPresent && other.isPresent)
                ? value == other.value
                : isPresent == other.isPresent;
    }


    @Override
    public int hashCode() {
        return isPresent ? Long.hashCode(value) : 0;
    }


    @Override
    public String toString() {
        return isPresent
                ? String.format("OptionalLong[%s]", value)
                : "OptionalLong.empty";
    }
}
