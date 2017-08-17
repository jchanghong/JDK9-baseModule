/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.io;

import java.util.Objects;


public class UncheckedIOException extends RuntimeException {
    private static final long serialVersionUID = -8134305061645241065L;


    public UncheckedIOException(String message, IOException cause) {
        super(message, Objects.requireNonNull(cause));
    }


    public UncheckedIOException(IOException cause) {
        super(Objects.requireNonNull(cause));
    }


    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }


    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException
    {
        s.defaultReadObject();
        Throwable cause = super.getCause();
        if (!(cause instanceof IOException))
            throw new InvalidObjectException("Cause must be an IOException");
    }
}
