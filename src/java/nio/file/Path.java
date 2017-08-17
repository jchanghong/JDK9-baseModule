/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.NoSuchElementException;



public interface Path
    extends Comparable<Path>, Iterable<Path>, Watchable
{

    FileSystem getFileSystem();


    boolean isAbsolute();


    Path getRoot();


    Path getFileName();


    Path getParent();


    int getNameCount();


    Path getName(int index);


    Path subpath(int beginIndex, int endIndex);


    boolean startsWith(Path other);


    default boolean startsWith(String other) {
        return startsWith(getFileSystem().getPath(other));
    }


    boolean endsWith(Path other);


    default boolean endsWith(String other) {
        return endsWith(getFileSystem().getPath(other));
    }


    Path normalize();

    // -- resolution and relativization --


    Path resolve(Path other);


    default Path resolve(String other) {
        return resolve(getFileSystem().getPath(other));
    }


    default Path resolveSibling(Path other) {
        if (other == null)
            throw new NullPointerException();
        Path parent = getParent();
        return (parent == null) ? other : parent.resolve(other);
    }


    default Path resolveSibling(String other) {
        return resolveSibling(getFileSystem().getPath(other));
    }


    Path relativize(Path other);


    URI toUri();


    Path toAbsolutePath();


    Path toRealPath(LinkOption... options) throws IOException;


    default File toFile() {
        if (getFileSystem() == FileSystems.getDefault()) {
            return new File(toString());
        } else {
            throw new UnsupportedOperationException("Path not associated with "
                    + "default file system.");
        }
    }

    // -- watchable --


    @Override
    WatchKey register(WatchService watcher,
                      WatchEvent.Kind<?>[] events,
                      WatchEvent.Modifier... modifiers)
        throws IOException;


    @Override
    default WatchKey register(WatchService watcher,
                      WatchEvent.Kind<?>... events) throws IOException {
        return register(watcher, events, new WatchEvent.Modifier[0]);
    }

    // -- Iterable --


    @Override
    default Iterator<Path> iterator() {
        return new Iterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return (i < getNameCount());
            }

            @Override
            public Path next() {
                if (i < getNameCount()) {
                    Path result = getName(i);
                    i++;
                    return result;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    // -- compareTo/equals/hashCode --


    @Override
    int compareTo(Path other);


    boolean equals(Object other);


    int hashCode();


    String toString();
}
