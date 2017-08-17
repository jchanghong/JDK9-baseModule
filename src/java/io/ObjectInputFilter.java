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

package java.io;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;



@FunctionalInterface
public interface ObjectInputFilter {


    Status checkInput(FilterInfo filterInfo);


    interface FilterInfo {

        Class<?> serialClass();


        long arrayLength();


        long depth();


        long references();


        long streamBytes();
    }


    enum Status {

        UNDECIDED,

        ALLOWED,

        REJECTED;
    }


    final class Config {
        /* No instances. */
        private Config() {}


        private final static Object serialFilterLock = new Object();


        private final static System.Logger configLog;


        static void filterLog(System.Logger.Level level, String msg, Object... args) {
            if (configLog != null) {
                configLog.log(level, msg, args);
            }
        }


        private final static String SERIAL_FILTER_PROPNAME = "jdk.serialFilter";


        private final static ObjectInputFilter configuredFilter;

        static {
            configuredFilter = AccessController
                    .doPrivileged((PrivilegedAction<ObjectInputFilter>) () -> {
                        String props = System.getProperty(SERIAL_FILTER_PROPNAME);
                        if (props == null) {
                            props = Security.getProperty(SERIAL_FILTER_PROPNAME);
                        }
                        if (props != null) {
                            System.Logger log =
                                    System.getLogger("java.io.serialization");
                            log.log(System.Logger.Level.INFO,
                                    "Creating serialization filter from {0}", props);
                            try {
                                return createFilter(props);
                            } catch (RuntimeException re) {
                                log.log(System.Logger.Level.ERROR,
                                        "Error configuring filter: {0}", re);
                            }
                        }
                        return null;
                    });
            configLog = (configuredFilter != null) ? System.getLogger("java.io.serialization") : null;
        }


        private static ObjectInputFilter serialFilter = configuredFilter;


        public static ObjectInputFilter getSerialFilter() {
            synchronized (serialFilterLock) {
                return serialFilter;
            }
        }


        public static void setSerialFilter(ObjectInputFilter filter) {
            Objects.requireNonNull(filter, "filter");
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(ObjectStreamConstants.SERIAL_FILTER_PERMISSION);
            }
            synchronized (serialFilterLock) {
                if (serialFilter != null) {
                    throw new IllegalStateException("Serial filter can only be set once");
                }
                serialFilter = filter;
            }
        }


        public static ObjectInputFilter createFilter(String pattern) {
            Objects.requireNonNull(pattern, "pattern");
            return Global.createFilter(pattern);
        }


        final static class Global implements ObjectInputFilter {

            private final String pattern;

            private final List<Function<Class<?>, Status>> filters;

            private long maxStreamBytes;

            private long maxDepth;

            private long maxReferences;

            private long maxArrayLength;


            static ObjectInputFilter createFilter(String pattern) {
                try {
                    return new Global(pattern);
                } catch (UnsupportedOperationException uoe) {
                    // no non-empty patterns
                    return null;
                }
            }


            private Global(String pattern) {
                boolean hasLimits = false;
                this.pattern = pattern;

                maxArrayLength = Long.MAX_VALUE; // Default values are unlimited
                maxDepth = Long.MAX_VALUE;
                maxReferences = Long.MAX_VALUE;
                maxStreamBytes = Long.MAX_VALUE;

                String[] patterns = pattern.split(";");
                filters = new ArrayList<>(patterns.length);
                for (int i = 0; i < patterns.length; i++) {
                    String p = patterns[i];
                    int nameLen = p.length();
                    if (nameLen == 0) {
                        continue;
                    }
                    if (parseLimit(p)) {
                        // If the pattern contained a limit setting, i.e. type=value
                        hasLimits = true;
                        continue;
                    }
                    boolean negate = p.charAt(0) == '!';
                    int poffset = negate ? 1 : 0;

                    // isolate module name, if any
                    int slash = p.indexOf('/', poffset);
                    if (slash == poffset) {
                        throw new IllegalArgumentException("module name is missing in: \"" + pattern + "\"");
                    }
                    final String moduleName = (slash >= 0) ? p.substring(poffset, slash) : null;
                    poffset = (slash >= 0) ? slash + 1 : poffset;

                    final Function<Class<?>, Status> patternFilter;
                    if (p.endsWith("*")) {
                        // Wildcard cases
                        if (p.endsWith(".*")) {
                            // Pattern is a package name with a wildcard
                            final String pkg = p.substring(poffset, nameLen - 1);
                            if (pkg.length() < 2) {
                                throw new IllegalArgumentException("package missing in: \"" + pattern + "\"");
                            }
                            if (negate) {
                                // A Function that fails if the class starts with the pattern, otherwise don't care
                                patternFilter = c -> matchesPackage(c, pkg) ? Status.REJECTED : Status.UNDECIDED;
                            } else {
                                // A Function that succeeds if the class starts with the pattern, otherwise don't care
                                patternFilter = c -> matchesPackage(c, pkg) ? Status.ALLOWED : Status.UNDECIDED;
                            }
                        } else if (p.endsWith(".**")) {
                            // Pattern is a package prefix with a double wildcard
                            final String pkgs = p.substring(poffset, nameLen - 2);
                            if (pkgs.length() < 2) {
                                throw new IllegalArgumentException("package missing in: \"" + pattern + "\"");
                            }
                            if (negate) {
                                // A Function that fails if the class starts with the pattern, otherwise don't care
                                patternFilter = c -> c.getName().startsWith(pkgs) ? Status.REJECTED : Status.UNDECIDED;
                            } else {
                                // A Function that succeeds if the class starts with the pattern, otherwise don't care
                                patternFilter = c -> c.getName().startsWith(pkgs) ? Status.ALLOWED : Status.UNDECIDED;
                            }
                        } else {
                            // Pattern is a classname (possibly empty) with a trailing wildcard
                            final String className = p.substring(poffset, nameLen - 1);
                            if (negate) {
                                // A Function that fails if the class starts with the pattern, otherwise don't care
                                patternFilter = c -> c.getName().startsWith(className) ? Status.REJECTED : Status.UNDECIDED;
                            } else {
                                // A Function that succeeds if the class starts with the pattern, otherwise don't care
                                patternFilter = c -> c.getName().startsWith(className) ? Status.ALLOWED : Status.UNDECIDED;
                            }
                        }
                    } else {
                        final String name = p.substring(poffset);
                        if (name.isEmpty()) {
                            throw new IllegalArgumentException("class or package missing in: \"" + pattern + "\"");
                        }
                        // Pattern is a class name
                        if (negate) {
                            // A Function that fails if the class equals the pattern, otherwise don't care
                            patternFilter = c -> c.getName().equals(name) ? Status.REJECTED : Status.UNDECIDED;
                        } else {
                            // A Function that succeeds if the class equals the pattern, otherwise don't care
                            patternFilter = c -> c.getName().equals(name) ? Status.ALLOWED : Status.UNDECIDED;
                        }
                    }
                    // If there is a moduleName, combine the module name check with the package/class check
                    if (moduleName == null) {
                        filters.add(patternFilter);
                    } else {
                        filters.add(c -> moduleName.equals(c.getModule().getName()) ? patternFilter.apply(c) : Status.UNDECIDED);
                    }
                }
                if (filters.isEmpty() && !hasLimits) {
                    throw new UnsupportedOperationException("no non-empty patterns");
                }
            }


            private boolean parseLimit(String pattern) {
                int eqNdx = pattern.indexOf('=');
                if (eqNdx < 0) {
                    // not a limit pattern
                    return false;
                }
                String valueString = pattern.substring(eqNdx + 1);
                if (pattern.startsWith("maxdepth=")) {
                    maxDepth = parseValue(valueString);
                } else if (pattern.startsWith("maxarray=")) {
                    maxArrayLength = parseValue(valueString);
                } else if (pattern.startsWith("maxrefs=")) {
                    maxReferences = parseValue(valueString);
                } else if (pattern.startsWith("maxbytes=")) {
                    maxStreamBytes = parseValue(valueString);
                } else {
                    throw new IllegalArgumentException("unknown limit: " + pattern.substring(0, eqNdx));
                }
                return true;
            }


            private static long parseValue(String string) throws IllegalArgumentException {
                // Parse a Long from after the '=' to the end
                long value = Long.parseLong(string);
                if (value < 0) {
                    throw new IllegalArgumentException("negative limit: " + string);
                }
                return value;
            }


            @Override
            public Status checkInput(FilterInfo filterInfo) {
                if (filterInfo.references() < 0
                        || filterInfo.depth() < 0
                        || filterInfo.streamBytes() < 0
                        || filterInfo.references() > maxReferences
                        || filterInfo.depth() > maxDepth
                        || filterInfo.streamBytes() > maxStreamBytes) {
                    return Status.REJECTED;
                }

                Class<?> clazz = filterInfo.serialClass();
                if (clazz != null) {
                    if (clazz.isArray()) {
                        if (filterInfo.arrayLength() >= 0 && filterInfo.arrayLength() > maxArrayLength) {
                            // array length is too big
                            return Status.REJECTED;
                        }
                        do {
                            // Arrays are decided based on the component type
                            clazz = clazz.getComponentType();
                        } while (clazz.isArray());
                    }

                    if (clazz.isPrimitive())  {
                        // Primitive types are undecided; let someone else decide
                        return Status.UNDECIDED;
                    } else {
                        // Find any filter that allowed or rejected the class
                        final Class<?> cl = clazz;
                        Optional<Status> status = filters.stream()
                                .map(f -> f.apply(cl))
                                .filter(p -> p != Status.UNDECIDED)
                                .findFirst();
                        return status.orElse(Status.UNDECIDED);
                    }
                }
                return Status.UNDECIDED;
            }


            private static boolean matchesPackage(Class<?> c, String pkg) {
                String n = c.getName();
                return n.startsWith(pkg) && n.lastIndexOf('.') == pkg.length() - 1;
            }


            @Override
            public String toString() {
                return pattern;
            }
        }
    }
}
