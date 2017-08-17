/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package java.util.spi;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;


public interface ToolProvider {

    String name();


    int run(PrintWriter out, PrintWriter err, String... args);


    default int run(PrintStream out, PrintStream err, String... args) {
        Objects.requireNonNull(out);
        Objects.requireNonNull(err);
        for (String arg : args) {
            Objects.requireNonNull(args);
        }

        PrintWriter outWriter = new PrintWriter(out);
        PrintWriter errWriter = new PrintWriter(err);
        try {
            try {
                return run(outWriter, errWriter, args);
            } finally {
                outWriter.flush();
            }
        } finally {
            errWriter.flush();
        }
    }


    static Optional<ToolProvider> findFirst(String name) {
        Objects.requireNonNull(name);
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        return AccessController.doPrivileged(
            (PrivilegedAction<Optional<ToolProvider>>) () -> {
                ServiceLoader<ToolProvider> sl =
                    ServiceLoader.load(ToolProvider.class, systemClassLoader);
                return StreamSupport.stream(sl.spliterator(), false)
                    .filter(p -> p.name().equals(name))
                    .findFirst();
            });
    }
}

