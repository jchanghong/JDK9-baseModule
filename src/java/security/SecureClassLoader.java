/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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

package java.security;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import sun.security.util.Debug;


public class SecureClassLoader extends ClassLoader {
    /*
     * If initialization succeed this is set to true and security checks will
     * succeed. Otherwise the object is not initialized and the object is
     * useless.
     */
    private final boolean initialized;

    /*
     * Map that maps the CodeSource to a ProtectionDomain. The key is a
     * CodeSourceKey class that uses a String instead of a URL to avoid
     * potential expensive name service lookups. This does mean that URLs that
     * are equivalent after nameservice lookup will be placed in separate
     * ProtectionDomains; however during policy enforcement these URLs will be
     * canonicalized and resolved resulting in a consistent set of granted
     * permissions.
     */
    private final Map<CodeSourceKey, ProtectionDomain> pdcache
            = new ConcurrentHashMap<>(11);

    static {
        ClassLoader.registerAsParallelCapable();
    }


    protected SecureClassLoader(ClassLoader parent) {
        super(parent);
        // this is to make the stack depth consistent with 1.1
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkCreateClassLoader();
        }
        initialized = true;
    }


    protected SecureClassLoader() {
        super();
        // this is to make the stack depth consistent with 1.1
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkCreateClassLoader();
        }
        initialized = true;
    }


    protected SecureClassLoader(String name, ClassLoader parent) {
        super(name, parent);
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkCreateClassLoader();
        }
        initialized = true;
    }


    protected final Class<?> defineClass(String name,
                                         byte[] b, int off, int len,
                                         CodeSource cs)
    {
        return defineClass(name, b, off, len, getProtectionDomain(cs));
    }


    protected final Class<?> defineClass(String name, java.nio.ByteBuffer b,
                                         CodeSource cs)
    {
        return defineClass(name, b, getProtectionDomain(cs));
    }


    protected PermissionCollection getPermissions(CodeSource codesource)
    {
        check();
        return new Permissions(); // ProtectionDomain defers the binding
    }

    /*
     * holder class for the static field "debug" to delay its initialization
     */
    private static class DebugHolder {
        private static final Debug debug = Debug.getInstance("scl");
    }

    /*
     * Returned cached ProtectionDomain for the specified CodeSource.
     */
    private ProtectionDomain getProtectionDomain(CodeSource cs) {
        if (cs == null) {
            return null;
        }

        // Use a CodeSourceKey object key. It should behave in the
        // same manner as the CodeSource when compared for equality except
        // that no nameservice lookup is done on the hostname (String comparison
        // only), and the fragment is not considered.
        CodeSourceKey key = new CodeSourceKey(cs);
        return pdcache.computeIfAbsent(key, new Function<>() {
            @Override
            public ProtectionDomain apply(CodeSourceKey key /* not used */) {
                PermissionCollection perms
                        = SecureClassLoader.this.getPermissions(cs);
                ProtectionDomain pd = new ProtectionDomain(
                        cs, perms, SecureClassLoader.this, null);
                if (DebugHolder.debug != null) {
                    DebugHolder.debug.println(" getPermissions " + pd);
                    DebugHolder.debug.println("");
                }
                return pd;
            }
        });
    }

    /*
     * Check to make sure the class loader has been initialized.
     */
    private void check() {
        if (!initialized) {
            throw new SecurityException("ClassLoader object not initialized");
        }
    }

    private static class CodeSourceKey {
        private final CodeSource cs;

        CodeSourceKey(CodeSource cs) {
            this.cs = cs;
        }

        @Override
        public int hashCode() {
            String locationNoFrag = cs.getLocationNoFragString();
            return locationNoFrag != null ? locationNoFrag.hashCode() : 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (!(obj instanceof CodeSourceKey)) {
                return false;
            }

            CodeSourceKey csk = (CodeSourceKey) obj;

            if (!Objects.equals(cs.getLocationNoFragString(),
                                csk.cs.getLocationNoFragString())) {
                return false;
            }

            return cs.matchCerts(csk.cs, true);
        }
    }
}
