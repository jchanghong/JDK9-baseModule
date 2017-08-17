/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Native;



abstract class FileSystem {

    /* -- Normalization and construction -- */


    public abstract char getSeparator();


    public abstract char getPathSeparator();


    public abstract String normalize(String path);


    public abstract int prefixLength(String path);


    public abstract String resolve(String parent, String child);


    public abstract String getDefaultParent();


    public abstract String fromURIPath(String path);


    /* -- Path operations -- */


    public abstract boolean isAbsolute(File f);


    public abstract String resolve(File f);

    public abstract String canonicalize(String path) throws IOException;


    /* -- Attribute accessors -- */

    /* Constants for simple boolean attributes */
    @Native public static final int BA_EXISTS    = 0x01;
    @Native public static final int BA_REGULAR   = 0x02;
    @Native public static final int BA_DIRECTORY = 0x04;
    @Native public static final int BA_HIDDEN    = 0x08;


    public abstract int getBooleanAttributes(File f);

    @Native public static final int ACCESS_READ    = 0x04;
    @Native public static final int ACCESS_WRITE   = 0x02;
    @Native public static final int ACCESS_EXECUTE = 0x01;


    public abstract boolean checkAccess(File f, int access);

    public abstract boolean setPermission(File f, int access, boolean enable, boolean owneronly);


    public abstract long getLastModifiedTime(File f);


    public abstract long getLength(File f);


    /* -- File operations -- */


    public abstract boolean createFileExclusively(String pathname)
        throws IOException;


    public abstract boolean delete(File f);


    public abstract String[] list(File f);


    public abstract boolean createDirectory(File f);


    public abstract boolean rename(File f1, File f2);


    public abstract boolean setLastModifiedTime(File f, long time);


    public abstract boolean setReadOnly(File f);


    /* -- Filesystem interface -- */


    public abstract File[] listRoots();

    /* -- Disk usage -- */
    @Native public static final int SPACE_TOTAL  = 0;
    @Native public static final int SPACE_FREE   = 1;
    @Native public static final int SPACE_USABLE = 2;

    public abstract long getSpace(File f, int t);

    /* -- Basic infrastructure -- */


    public abstract int getNameMax(String path);


    public abstract int compare(File f1, File f2);


    public abstract int hashCode(File f);

    // Flags for enabling/disabling performance optimizations for file
    // name canonicalization
    static boolean useCanonCaches      = true;
    static boolean useCanonPrefixCache = true;

    private static boolean getBooleanProperty(String prop, boolean defaultVal) {
        return Boolean.parseBoolean(System.getProperty(prop,
                String.valueOf(defaultVal)));
    }

    static {
        useCanonCaches      = getBooleanProperty("sun.io.useCanonCaches",
                                                 useCanonCaches);
        useCanonPrefixCache = getBooleanProperty("sun.io.useCanonPrefixCache",
                                                 useCanonPrefixCache);
    }
}
