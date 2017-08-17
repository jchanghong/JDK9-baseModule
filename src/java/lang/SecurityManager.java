/*
 * Copyright (c) 1995, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.module.ModuleReference;
import java.lang.reflect.Member;
import java.io.FileDescriptor;
import java.io.File;
import java.io.FilePermission;
import java.net.InetAddress;
import java.net.SocketPermission;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.Security;
import java.security.SecurityPermission;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jdk.internal.module.ModuleBootstrap;
import jdk.internal.module.ModuleLoaderMap;
import jdk.internal.reflect.CallerSensitive;
import sun.security.util.SecurityConstants;


public
class SecurityManager {


    @Deprecated(since="1.2", forRemoval=true)
    protected boolean inCheck;

    /*
     * Have we been initialized. Effective against finalizer attacks.
     */
    private boolean initialized = false;



    private boolean hasAllPermission() {
        try {
            checkPermission(SecurityConstants.ALL_PERMISSION);
            return true;
        } catch (SecurityException se) {
            return false;
        }
    }


    @Deprecated(since="1.2", forRemoval=true)
    public boolean getInCheck() {
        return inCheck;
    }


    public SecurityManager() {
        synchronized(SecurityManager.class) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                // ask the currently installed security manager if we
                // can create a new one.
                sm.checkPermission(new RuntimePermission
                                   ("createSecurityManager"));
            }
            initialized = true;
        }
    }


    protected native Class<?>[] getClassContext();


    @Deprecated(since="1.2", forRemoval=true)
    protected ClassLoader currentClassLoader() {
        ClassLoader cl = currentClassLoader0();
        if ((cl != null) && hasAllPermission())
            cl = null;
        return cl;
    }

    private native ClassLoader currentClassLoader0();


    @Deprecated(since="1.2", forRemoval=true)
    protected Class<?> currentLoadedClass() {
        Class<?> c = currentLoadedClass0();
        if ((c != null) && hasAllPermission())
            c = null;
        return c;
    }


    @Deprecated(since="1.2", forRemoval=true)
    protected native int classDepth(String name);


    @Deprecated(since="1.2", forRemoval=true)
    protected int classLoaderDepth() {
        int depth = classLoaderDepth0();
        if (depth != -1) {
            if (hasAllPermission())
                depth = -1;
            else
                depth--; // make sure we don't include ourself
        }
        return depth;
    }

    private native int classLoaderDepth0();


    @Deprecated(since="1.2", forRemoval=true)
    protected boolean inClass(String name) {
        return classDepth(name) >= 0;
    }


    @Deprecated(since="1.2", forRemoval=true)
    protected boolean inClassLoader() {
        return currentClassLoader() != null;
    }


    public Object getSecurityContext() {
        return AccessController.getContext();
    }


    public void checkPermission(Permission perm) {
        java.security.AccessController.checkPermission(perm);
    }


    public void checkPermission(Permission perm, Object context) {
        if (context instanceof AccessControlContext) {
            ((AccessControlContext)context).checkPermission(perm);
        } else {
            throw new SecurityException();
        }
    }


    public void checkCreateClassLoader() {
        checkPermission(SecurityConstants.CREATE_CLASSLOADER_PERMISSION);
    }



    private static ThreadGroup rootGroup = getRootGroup();

    private static ThreadGroup getRootGroup() {
        ThreadGroup root =  Thread.currentThread().getThreadGroup();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        return root;
    }


    public void checkAccess(Thread t) {
        if (t == null) {
            throw new NullPointerException("thread can't be null");
        }
        if (t.getThreadGroup() == rootGroup) {
            checkPermission(SecurityConstants.MODIFY_THREAD_PERMISSION);
        } else {
            // just return
        }
    }

    public void checkAccess(ThreadGroup g) {
        if (g == null) {
            throw new NullPointerException("thread group can't be null");
        }
        if (g == rootGroup) {
            checkPermission(SecurityConstants.MODIFY_THREADGROUP_PERMISSION);
        } else {
            // just return
        }
    }


    public void checkExit(int status) {
        checkPermission(new RuntimePermission("exitVM."+status));
    }


    public void checkExec(String cmd) {
        File f = new File(cmd);
        if (f.isAbsolute()) {
            checkPermission(new FilePermission(cmd,
                SecurityConstants.FILE_EXECUTE_ACTION));
        } else {
            checkPermission(new FilePermission("<<ALL FILES>>",
                SecurityConstants.FILE_EXECUTE_ACTION));
        }
    }


    public void checkLink(String lib) {
        if (lib == null) {
            throw new NullPointerException("library can't be null");
        }
        checkPermission(new RuntimePermission("loadLibrary."+lib));
    }


    public void checkRead(FileDescriptor fd) {
        if (fd == null) {
            throw new NullPointerException("file descriptor can't be null");
        }
        checkPermission(new RuntimePermission("readFileDescriptor"));
    }


    public void checkRead(String file) {
        checkPermission(new FilePermission(file,
            SecurityConstants.FILE_READ_ACTION));
    }


    public void checkRead(String file, Object context) {
        checkPermission(
            new FilePermission(file, SecurityConstants.FILE_READ_ACTION),
            context);
    }


    public void checkWrite(FileDescriptor fd) {
        if (fd == null) {
            throw new NullPointerException("file descriptor can't be null");
        }
        checkPermission(new RuntimePermission("writeFileDescriptor"));

    }


    public void checkWrite(String file) {
        checkPermission(new FilePermission(file,
            SecurityConstants.FILE_WRITE_ACTION));
    }


    public void checkDelete(String file) {
        checkPermission(new FilePermission(file,
            SecurityConstants.FILE_DELETE_ACTION));
    }


    public void checkConnect(String host, int port) {
        if (host == null) {
            throw new NullPointerException("host can't be null");
        }
        if (!host.startsWith("[") && host.indexOf(':') != -1) {
            host = "[" + host + "]";
        }
        if (port == -1) {
            checkPermission(new SocketPermission(host,
                SecurityConstants.SOCKET_RESOLVE_ACTION));
        } else {
            checkPermission(new SocketPermission(host+":"+port,
                SecurityConstants.SOCKET_CONNECT_ACTION));
        }
    }


    public void checkConnect(String host, int port, Object context) {
        if (host == null) {
            throw new NullPointerException("host can't be null");
        }
        if (!host.startsWith("[") && host.indexOf(':') != -1) {
            host = "[" + host + "]";
        }
        if (port == -1)
            checkPermission(new SocketPermission(host,
                SecurityConstants.SOCKET_RESOLVE_ACTION),
                context);
        else
            checkPermission(new SocketPermission(host+":"+port,
                SecurityConstants.SOCKET_CONNECT_ACTION),
                context);
    }


    public void checkListen(int port) {
        checkPermission(new SocketPermission("localhost:"+port,
            SecurityConstants.SOCKET_LISTEN_ACTION));
    }


    public void checkAccept(String host, int port) {
        if (host == null) {
            throw new NullPointerException("host can't be null");
        }
        if (!host.startsWith("[") && host.indexOf(':') != -1) {
            host = "[" + host + "]";
        }
        checkPermission(new SocketPermission(host+":"+port,
            SecurityConstants.SOCKET_ACCEPT_ACTION));
    }


    public void checkMulticast(InetAddress maddr) {
        String host = maddr.getHostAddress();
        if (!host.startsWith("[") && host.indexOf(':') != -1) {
            host = "[" + host + "]";
        }
        checkPermission(new SocketPermission(host,
            SecurityConstants.SOCKET_CONNECT_ACCEPT_ACTION));
    }


    @Deprecated(since="1.4")
    public void checkMulticast(InetAddress maddr, byte ttl) {
        String host = maddr.getHostAddress();
        if (!host.startsWith("[") && host.indexOf(':') != -1) {
            host = "[" + host + "]";
        }
        checkPermission(new SocketPermission(host,
            SecurityConstants.SOCKET_CONNECT_ACCEPT_ACTION));
    }


    public void checkPropertiesAccess() {
        checkPermission(new PropertyPermission("*",
            SecurityConstants.PROPERTY_RW_ACTION));
    }


    public void checkPropertyAccess(String key) {
        checkPermission(new PropertyPermission(key,
            SecurityConstants.PROPERTY_READ_ACTION));
    }


    @Deprecated(since="1.8", forRemoval=true)
    public boolean checkTopLevelWindow(Object window) {
        if (window == null) {
            throw new NullPointerException("window can't be null");
        }
        return hasAllPermission();
    }


    public void checkPrintJobAccess() {
        checkPermission(new RuntimePermission("queuePrintJob"));
    }


    @Deprecated(since="1.8", forRemoval=true)
    public void checkSystemClipboardAccess() {
        checkPermission(SecurityConstants.ALL_PERMISSION);
    }


    @Deprecated(since="1.8", forRemoval=true)
    public void checkAwtEventQueueAccess() {
        checkPermission(SecurityConstants.ALL_PERMISSION);
    }

    /*
     * We have an initial invalid bit (initially false) for the class
     * variables which tell if the cache is valid.  If the underlying
     * java.security.Security property changes via setProperty(), the
     * Security class uses reflection to change the variable and thus
     * invalidate the cache.
     *
     * Locking is handled by synchronization to the
     * packageAccessLock/packageDefinitionLock objects.  They are only
     * used in this class.
     *
     * Note that cache invalidation as a result of the property change
     * happens without using these locks, so there may be a delay between
     * when a thread updates the property and when other threads updates
     * the cache.
     */
    private static boolean packageAccessValid = false;
    private static String[] packageAccess;
    private static final Object packageAccessLock = new Object();

    private static boolean packageDefinitionValid = false;
    private static String[] packageDefinition;
    private static final Object packageDefinitionLock = new Object();

    private static String[] getPackages(String p) {
        String packages[] = null;
        if (p != null && !p.equals("")) {
            java.util.StringTokenizer tok =
                new java.util.StringTokenizer(p, ",");
            int n = tok.countTokens();
            if (n > 0) {
                packages = new String[n];
                int i = 0;
                while (tok.hasMoreElements()) {
                    String s = tok.nextToken().trim();
                    packages[i++] = s;
                }
            }
        }

        if (packages == null) {
            packages = new String[0];
        }
        return packages;
    }

    // The non-exported packages in modules defined to the boot or platform
    // class loaders. A non-exported package is a package that is not exported
    // or is only exported to specific modules.
    private static final Map<String, Boolean> nonExportedPkgs = new ConcurrentHashMap<>();
    static {
        addNonExportedPackages(ModuleLayer.boot());
    }


    static void addNonExportedPackages(ModuleLayer layer) {
        Set<String> bootModules = ModuleLoaderMap.bootModules();
        Set<String> platformModules = ModuleLoaderMap.platformModules();
        layer.modules().stream()
                .map(Module::getDescriptor)
                .filter(md -> bootModules.contains(md.name())
                        || platformModules.contains(md.name()))
                .map(SecurityManager::nonExportedPkgs)
                .flatMap(Set::stream)
                .forEach(pn -> nonExportedPkgs.put(pn, Boolean.TRUE));
    }



    static void invalidatePackageAccessCache() {
        synchronized (packageAccessLock) {
            packageAccessValid = false;
        }
        synchronized (packageDefinitionLock) {
            packageDefinitionValid = false;
        }
    }


    private static Set<String> nonExportedPkgs(ModuleDescriptor md) {
        // start with all packages in the module
        Set<String> pkgs = new HashSet<>(md.packages());

        // remove the non-qualified exported packages
        md.exports().stream()
                    .filter(p -> !p.isQualified())
                    .map(Exports::source)
                    .forEach(pkgs::remove);

        // remove the non-qualified open packages
        md.opens().stream()
                  .filter(p -> !p.isQualified())
                  .map(Opens::source)
                  .forEach(pkgs::remove);

        return pkgs;
    }


    public void checkPackageAccess(String pkg) {
        Objects.requireNonNull(pkg, "package name can't be null");

        // check if pkg is not exported to all modules
        if (nonExportedPkgs.containsKey(pkg)) {
            checkPermission(
                new RuntimePermission("accessClassInPackage." + pkg));
            return;
        }

        String[] restrictedPkgs;
        synchronized (packageAccessLock) {
            /*
             * Do we need to update our property array?
             */
            if (!packageAccessValid) {
                String tmpPropertyStr =
                    AccessController.doPrivileged(
                        new PrivilegedAction<>() {
                            public String run() {
                                return Security.getProperty("package.access");
                            }
                        }
                    );
                packageAccess = getPackages(tmpPropertyStr);
                packageAccessValid = true;
            }

            // Using a snapshot of packageAccess -- don't care if static field
            // changes afterwards; array contents won't change.
            restrictedPkgs = packageAccess;
        }

        /*
         * Traverse the list of packages, check for any matches.
         */
        final int plen = pkg.length();
        for (String restrictedPkg : restrictedPkgs) {
            final int rlast = restrictedPkg.length() - 1;

            // Optimizations:
            //
            // If rlast >= plen then restrictedPkg is longer than pkg by at
            // least one char. This means pkg cannot start with restrictedPkg,
            // since restrictedPkg will be longer than pkg.
            //
            // Similarly if rlast != plen, then pkg + "." cannot be the same
            // as restrictedPkg, since pkg + "." will have a different length
            // than restrictedPkg.
            //
            if (rlast < plen && pkg.startsWith(restrictedPkg) ||
                // The following test is equivalent to
                // restrictedPkg.equals(pkg + ".") but is noticeably more
                // efficient:
                rlast == plen && restrictedPkg.startsWith(pkg) &&
                restrictedPkg.charAt(rlast) == '.')
            {
                checkPermission(
                    new RuntimePermission("accessClassInPackage." + pkg));
                break;  // No need to continue; only need to check this once
            }
        }
    }


    public void checkPackageDefinition(String pkg) {
        Objects.requireNonNull(pkg, "package name can't be null");

        // check if pkg is not exported to all modules
        if (nonExportedPkgs.containsKey(pkg)) {
            checkPermission(
                new RuntimePermission("defineClassInPackage." + pkg));
            return;
        }

        String[] pkgs;
        synchronized (packageDefinitionLock) {
            /*
             * Do we need to update our property array?
             */
            if (!packageDefinitionValid) {
                String tmpPropertyStr =
                    AccessController.doPrivileged(
                        new PrivilegedAction<>() {
                            public String run() {
                                return java.security.Security.getProperty(
                                    "package.definition");
                            }
                        }
                    );
                packageDefinition = getPackages(tmpPropertyStr);
                packageDefinitionValid = true;
            }
            // Using a snapshot of packageDefinition -- don't care if static
            // field changes afterwards; array contents won't change.
            pkgs = packageDefinition;
        }

        /*
         * Traverse the list of packages, check for any matches.
         */
        for (String restrictedPkg : pkgs) {
            if (pkg.startsWith(restrictedPkg) || restrictedPkg.equals(pkg + ".")) {
                checkPermission(
                    new RuntimePermission("defineClassInPackage." + pkg));
                break; // No need to continue; only need to check this once
            }
        }
    }


    public void checkSetFactory() {
        checkPermission(new RuntimePermission("setFactory"));
    }


    @Deprecated(since="1.8", forRemoval=true)
    @CallerSensitive
    public void checkMemberAccess(Class<?> clazz, int which) {
        if (clazz == null) {
            throw new NullPointerException("class can't be null");
        }
        if (which != Member.PUBLIC) {
            Class<?> stack[] = getClassContext();
            /*
             * stack depth of 4 should be the caller of one of the
             * methods in java.lang.Class that invoke checkMember
             * access. The stack should look like:
             *
             * someCaller                        [3]
             * java.lang.Class.someReflectionAPI [2]
             * java.lang.Class.checkMemberAccess [1]
             * SecurityManager.checkMemberAccess [0]
             *
             */
            if ((stack.length<4) ||
                (stack[3].getClassLoader() != clazz.getClassLoader())) {
                checkPermission(SecurityConstants.CHECK_MEMBER_ACCESS_PERMISSION);
            }
        }
    }


    public void checkSecurityAccess(String target) {
        checkPermission(new SecurityPermission(target));
    }

    private native Class<?> currentLoadedClass0();


    public ThreadGroup getThreadGroup() {
        return Thread.currentThread().getThreadGroup();
    }

}
