/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.reflect.CallerSensitive;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;


public final class StackWalker {

    public static interface StackFrame {

        public String getClassName();


        public String getMethodName();


        public Class<?> getDeclaringClass();


        public int getByteCodeIndex();


        public String getFileName();


        public int getLineNumber();


        public boolean isNativeMethod();


        public StackTraceElement toStackTraceElement();
    }


    public enum Option {

        RETAIN_CLASS_REFERENCE,

        SHOW_REFLECT_FRAMES,

        SHOW_HIDDEN_FRAMES;
    }

    enum ExtendedOption {

        LOCALS_AND_OPERANDS
    };

    static final EnumSet<Option> DEFAULT_EMPTY_OPTION = EnumSet.noneOf(Option.class);

    private final static StackWalker DEFAULT_WALKER =
        new StackWalker(DEFAULT_EMPTY_OPTION);

    private final Set<Option> options;
    private final ExtendedOption extendedOption;
    private final int estimateDepth;


    public static StackWalker getInstance() {
        // no permission check needed
        return DEFAULT_WALKER;
    }


    public static StackWalker getInstance(Option option) {
        return getInstance(EnumSet.of(Objects.requireNonNull(option)));
    }


    public static StackWalker getInstance(Set<Option> options) {
        if (options.isEmpty()) {
            return DEFAULT_WALKER;
        }

        EnumSet<Option> optionSet = toEnumSet(options);
        checkPermission(optionSet);
        return new StackWalker(optionSet);
    }


    public static StackWalker getInstance(Set<Option> options, int estimateDepth) {
        if (estimateDepth <= 0) {
            throw new IllegalArgumentException("estimateDepth must be > 0");
        }
        EnumSet<Option> optionSet = toEnumSet(options);
        checkPermission(optionSet);
        return new StackWalker(optionSet, estimateDepth);
    }

    // ----- private constructors ------
    private StackWalker(EnumSet<Option> options) {
        this(options, 0, null);
    }
    private StackWalker(EnumSet<Option> options, int estimateDepth) {
        this(options, estimateDepth, null);
    }
    private StackWalker(EnumSet<Option> options, int estimateDepth, ExtendedOption extendedOption) {
        this.options = options;
        this.estimateDepth = estimateDepth;
        this.extendedOption = extendedOption;
    }

    private static void checkPermission(Set<Option> options) {
        Objects.requireNonNull(options);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (options.contains(Option.RETAIN_CLASS_REFERENCE)) {
                sm.checkPermission(new RuntimePermission("getStackWalkerWithClassReference"));
            }
        }
    }

    /*
     * Returns a defensive copy
     */
    private static EnumSet<Option> toEnumSet(Set<Option> options) {
        Objects.requireNonNull(options);
        if (options.isEmpty()) {
            return DEFAULT_EMPTY_OPTION;
        } else {
            return EnumSet.copyOf(options);
        }
    }


    @CallerSensitive
    public <T> T walk(Function<? super Stream<StackFrame>, ? extends T> function) {
        // Returning a Stream<StackFrame> would be unsafe, as the stream could
        // be used to access the stack frames in an uncontrolled manner.  For
        // example, a caller might pass a Spliterator of stack frames after one
        // or more frames had been traversed. There is no robust way to detect
        // whether the execution point when
        // Spliterator.tryAdvance(java.util.function.Consumer<? super T>) is
        // invoked is the exact same execution point where the stack frame
        // traversal is expected to resume.

        Objects.requireNonNull(function);
        return StackStreamFactory.makeStackTraverser(this, function)
                                 .walk();
    }


    @CallerSensitive
    public void forEach(Consumer<? super StackFrame> action) {
        Objects.requireNonNull(action);
        StackStreamFactory.makeStackTraverser(this, s -> {
            s.forEach(action);
            return null;
        }).walk();
    }


    @CallerSensitive
    public Class<?> getCallerClass() {
        if (!options.contains(Option.RETAIN_CLASS_REFERENCE)) {
            throw new UnsupportedOperationException("This stack walker " +
                    "does not have RETAIN_CLASS_REFERENCE access");
        }

        return StackStreamFactory.makeCallerFinder(this).findCaller();
    }

    // ---- package access ----

    static StackWalker newInstance(Set<Option> options, ExtendedOption extendedOption) {
        EnumSet<Option> optionSet = toEnumSet(options);
        checkPermission(optionSet);
        return new StackWalker(optionSet, 0, extendedOption);
    }

    int estimateDepth() {
        return estimateDepth;
    }

    boolean hasOption(Option option) {
        return options.contains(option);
    }

    boolean hasLocalsOperandsOption() {
        return extendedOption == ExtendedOption.LOCALS_AND_OPERANDS;
    }

    void ensureAccessEnabled(Option access) {
        if (!hasOption(access)) {
            throw new UnsupportedOperationException("No access to " + access +
                    ": " + options.toString());
        }
    }
}
