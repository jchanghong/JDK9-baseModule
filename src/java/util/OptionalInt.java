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

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.IntStream;


public final class OptionalInt {

    private static final OptionalInt EMPTY = new OptionalInt();


    private final boolean isPresent;
    private final int value;


    private OptionalInt() {
        this.isPresent = false;
        this.value = 0;
    }


    public static OptionalInt empty() {
        return EMPTY;
    }


    private OptionalInt(int value) {
        this.isPresent = true;
        this.value = value;
    }


    public static OptionalInt of(int value) {
        return new OptionalInt(value);
    }


    public int getAsInt() {
        if (!isPresent) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }


    public boolean isPresent() {
        return isPresent;
    }


    public void ifPresent(IntConsumer action) {
        if (isPresent) {
            action.accept(value);
        }
    }


    public void ifPresentOrElse(IntConsumer action, Runnable emptyAction) {
        if (isPresent) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }


    public IntStream stream() {
        if (isPresent) {
            return IntStream.of(value);
        } else {
            return IntStream.empty();
        }
    }


    public int orElse(int other) {
        return isPresent ? value : other;
    }


    public int orElseGet(IntSupplier supplier) {
        return isPresent ? value : supplier.getAsInt();
    }


    public<X extends Throwable> int orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
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

        if (!(obj instanceof OptionalInt)) {
            return false;
        }

        OptionalInt other = (OptionalInt) obj;
        return (isPresent && other.isPresent)
                ? value == other.value
                : isPresent == other.isPresent;
    }


    @Override
    public int hashCode() {
        return isPresent ? Integer.hashCode(value) : 0;
    }


    @Override
    public String toString() {
        return isPresent
                ? String.format("OptionalInt[%s]", value)
                : "OptionalInt.empty";
    }
}
