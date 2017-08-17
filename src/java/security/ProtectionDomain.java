/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import jdk.internal.misc.JavaSecurityAccess;
import jdk.internal.misc.JavaSecurityProtectionDomainAccess;
import static jdk.internal.misc.JavaSecurityProtectionDomainAccess.ProtectionDomainCache;
import jdk.internal.misc.SharedSecrets;
import sun.security.action.GetPropertyAction;
import sun.security.provider.PolicyFile;
import sun.security.util.Debug;
import sun.security.util.FilePermCompat;
import sun.security.util.SecurityConstants;



public class ProtectionDomain {


    private static final boolean filePermCompatInPD =
            "true".equals(GetPropertyAction.privilegedGetProperty(
                "jdk.security.filePermCompat"));

    private static class JavaSecurityAccessImpl implements JavaSecurityAccess {

        private JavaSecurityAccessImpl() {
        }

        @Override
        public <T> T doIntersectionPrivilege(
                PrivilegedAction<T> action,
                final AccessControlContext stack,
                final AccessControlContext context) {
            if (action == null) {
                throw new NullPointerException();
            }

            return AccessController.doPrivileged(
                action,
                getCombinedACC(context, stack)
            );
        }

        @Override
        public <T> T doIntersectionPrivilege(
                PrivilegedAction<T> action,
                AccessControlContext context) {
            return doIntersectionPrivilege(action,
                AccessController.getContext(), context);
        }

        @Override
        public ProtectionDomain[] getProtectDomains(AccessControlContext context) {
            return context.getContext();
        }

        private static AccessControlContext getCombinedACC(
            AccessControlContext context, AccessControlContext stack) {
            AccessControlContext acc =
                new AccessControlContext(context, stack.getCombiner(), true);

            return new AccessControlContext(stack.getContext(), acc).optimize();
        }
    }

    static {
        // setup SharedSecrets to allow access to doIntersectionPrivilege
        // methods and ProtectionDomain cache
        SharedSecrets.setJavaSecurityAccess(new JavaSecurityAccessImpl());
        SharedSecrets.setJavaSecurityProtectionDomainAccess(
            new JavaSecurityProtectionDomainAccess() {
                @Override
                public ProtectionDomainCache getProtectionDomainCache() {
                    return new PDCache();
                }
            });
    }


    static final class Key {}

    /* CodeSource */
    private CodeSource codesource ;

    /* ClassLoader the protection domain was consed from */
    private ClassLoader classloader;

    /* Principals running-as within this protection domain */
    private Principal[] principals;

    /* the rights this protection domain is granted */
    private PermissionCollection permissions;

    /* if the permissions object has AllPermission */
    private boolean hasAllPerm = false;

    /* the PermissionCollection is static (pre 1.4 constructor)
       or dynamic (via a policy refresh) */
    private final boolean staticPermissions;

    /*
     * An object used as a key when the ProtectionDomain is stored in a Map.
     */
    final Key key = new Key();


    public ProtectionDomain(CodeSource codesource,
                            PermissionCollection permissions) {
        this.codesource = codesource;
        if (permissions != null) {
            this.permissions = permissions;
            this.permissions.setReadOnly();
            if (permissions instanceof Permissions &&
                ((Permissions)permissions).allPermission != null) {
                hasAllPerm = true;
            }
        }
        this.classloader = null;
        this.principals = new Principal[0];
        staticPermissions = true;
    }


    public ProtectionDomain(CodeSource codesource,
                            PermissionCollection permissions,
                            ClassLoader classloader,
                            Principal[] principals) {
        this.codesource = codesource;
        if (permissions != null) {
            this.permissions = permissions;
            this.permissions.setReadOnly();
            if (permissions instanceof Permissions &&
                ((Permissions)permissions).allPermission != null) {
                hasAllPerm = true;
            }
        }
        this.classloader = classloader;
        this.principals = (principals != null ? principals.clone():
                           new Principal[0]);
        staticPermissions = false;
    }


    public final CodeSource getCodeSource() {
        return this.codesource;
    }



    public final ClassLoader getClassLoader() {
        return this.classloader;
    }



    public final Principal[] getPrincipals() {
        return this.principals.clone();
    }


    public final PermissionCollection getPermissions() {
        return permissions;
    }


    public final boolean staticPermissionsOnly() {
        return this.staticPermissions;
    }


    public boolean implies(Permission perm) {

        if (hasAllPerm) {
            // internal permission collection already has AllPermission -
            // no need to go to policy
            return true;
        }

        if (!staticPermissions &&
            Policy.getPolicyNoCheck().implies(this, perm)) {
            return true;
        }
        if (permissions != null) {
            return permissions.implies(perm);
        }

        return false;
    }


    boolean impliesWithAltFilePerm(Permission perm) {

        // If FilePermCompat.compat is set (default value), FilePermission
        // checking compatibility should be considered.

        // If filePermCompatInPD is set, this method checks for alternative
        // FilePermission to keep compatibility for any Policy implementation.
        // When set to false (default value), implies() is called since
        // the PolicyFile implementation already supports compatibility.

        // If this is a subclass of ProtectionDomain, call implies()
        // because most likely user has overridden it.

        if (!filePermCompatInPD || !FilePermCompat.compat ||
                getClass() != ProtectionDomain.class) {
            return implies(perm);
        }

        if (hasAllPerm) {
            // internal permission collection already has AllPermission -
            // no need to go to policy
            return true;
        }

        Permission p2 = null;
        boolean p2Calculated = false;

        if (!staticPermissions) {
            Policy policy = Policy.getPolicyNoCheck();
            if (policy instanceof PolicyFile) {
                // The PolicyFile implementation supports compatibility
                // inside and it also covers the static permissions.
                return policy.implies(this, perm);
            } else {
                if (policy.implies(this, perm)) {
                    return true;
                }
                p2 = FilePermCompat.newPermUsingAltPath(perm);
                p2Calculated = true;
                if (p2 != null && policy.implies(this, p2)) {
                    return true;
                }
            }
        }
        if (permissions != null) {
            if (permissions.implies(perm)) {
                return true;
            } else {
                if (!p2Calculated) {
                    p2 = FilePermCompat.newPermUsingAltPath(perm);
                }
                if (p2 != null) {
                    return permissions.implies(p2);
                }
            }
        }
        return false;
    }

    // called by the VM -- do not remove
    boolean impliesCreateAccessControlContext() {
        return implies(SecurityConstants.CREATE_ACC_PERMISSION);
    }


    @Override public String toString() {
        String pals = "<no principals>";
        if (principals != null && principals.length > 0) {
            StringBuilder palBuf = new StringBuilder("(principals ");

            for (int i = 0; i < principals.length; i++) {
                palBuf.append(principals[i].getClass().getName() +
                            " \"" + principals[i].getName() +
                            "\"");
                if (i < principals.length-1)
                    palBuf.append(",\n");
                else
                    palBuf.append(")\n");
            }
            pals = palBuf.toString();
        }

        // Check if policy is set; we don't want to load
        // the policy prematurely here
        PermissionCollection pc = Policy.isSet() && seeAllp() ?
                                      mergePermissions():
                                      getPermissions();

        return "ProtectionDomain "+
            " "+codesource+"\n"+
            " "+classloader+"\n"+
            " "+pals+"\n"+
            " "+pc+"\n";
    }

    /*
     * holder class for the static field "debug" to delay its initialization
     */
    private static class DebugHolder {
        private static final Debug debug = Debug.getInstance("domain");
    }


    private static boolean seeAllp() {
        SecurityManager sm = System.getSecurityManager();

        if (sm == null) {
            return true;
        } else {
            if (DebugHolder.debug != null) {
                if (sm.getClass().getClassLoader() == null &&
                    Policy.getPolicyNoCheck().getClass().getClassLoader()
                                                                == null) {
                    return true;
                }
            } else {
                try {
                    sm.checkPermission(SecurityConstants.GET_POLICY_PERMISSION);
                    return true;
                } catch (SecurityException se) {
                    // fall thru and return false
                }
            }
        }

        return false;
    }

    private PermissionCollection mergePermissions() {
        if (staticPermissions)
            return permissions;

        PermissionCollection perms =
            java.security.AccessController.doPrivileged
            (new java.security.PrivilegedAction<>() {
                    public PermissionCollection run() {
                        Policy p = Policy.getPolicyNoCheck();
                        return p.getPermissions(ProtectionDomain.this);
                    }
                });

        Permissions mergedPerms = new Permissions();
        int swag = 32;
        int vcap = 8;
        Enumeration<Permission> e;
        List<Permission> pdVector = new ArrayList<>(vcap);
        List<Permission> plVector = new ArrayList<>(swag);

        //
        // Build a vector of domain permissions for subsequent merge
        if (permissions != null) {
            synchronized (permissions) {
                e = permissions.elements();
                while (e.hasMoreElements()) {
                    pdVector.add(e.nextElement());
                }
            }
        }

        //
        // Build a vector of Policy permissions for subsequent merge
        if (perms != null) {
            synchronized (perms) {
                e = perms.elements();
                while (e.hasMoreElements()) {
                    plVector.add(e.nextElement());
                    vcap++;
                }
            }
        }

        if (perms != null && permissions != null) {
            //
            // Weed out the duplicates from the policy. Unless a refresh
            // has occurred since the pd was consed this should result in
            // an empty vector.
            synchronized (permissions) {
                e = permissions.elements();   // domain vs policy
                while (e.hasMoreElements()) {
                    Permission pdp = e.nextElement();
                    Class<?> pdpClass = pdp.getClass();
                    String pdpActions = pdp.getActions();
                    String pdpName = pdp.getName();
                    for (int i = 0; i < plVector.size(); i++) {
                        Permission pp = plVector.get(i);
                        if (pdpClass.isInstance(pp)) {
                            // The equals() method on some permissions
                            // have some side effects so this manual
                            // comparison is sufficient.
                            if (pdpName.equals(pp.getName()) &&
                                Objects.equals(pdpActions, pp.getActions())) {
                                plVector.remove(i);
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (perms !=null) {
            // the order of adding to merged perms and permissions
            // needs to preserve the bugfix 4301064

            for (int i = plVector.size()-1; i >= 0; i--) {
                mergedPerms.add(plVector.get(i));
            }
        }
        if (permissions != null) {
            for (int i = pdVector.size()-1; i >= 0; i--) {
                mergedPerms.add(pdVector.get(i));
            }
        }

        return mergedPerms;
    }


    private static class PDCache implements ProtectionDomainCache {
        private final ConcurrentHashMap<WeakProtectionDomainKey,
                                        SoftReference<PermissionCollection>>
                                        pdMap = new ConcurrentHashMap<>();
        private final ReferenceQueue<Key> queue = new ReferenceQueue<>();

        @Override
        public void put(ProtectionDomain pd, PermissionCollection pc) {
            processQueue(queue, pdMap);
            WeakProtectionDomainKey weakPd =
                new WeakProtectionDomainKey(pd, queue);
            pdMap.put(weakPd, new SoftReference<>(pc));
        }

        @Override
        public PermissionCollection get(ProtectionDomain pd) {
            processQueue(queue, pdMap);
            WeakProtectionDomainKey weakPd = new WeakProtectionDomainKey(pd);
            SoftReference<PermissionCollection> sr = pdMap.get(weakPd);
            return (sr == null) ? null : sr.get();
        }


        private static void processQueue(ReferenceQueue<Key> queue,
                                         ConcurrentHashMap<? extends
                                         WeakReference<Key>, ?> pdMap) {
            Reference<? extends Key> ref;
            while ((ref = queue.poll()) != null) {
                pdMap.remove(ref);
            }
        }
    }


    private static class WeakProtectionDomainKey extends WeakReference<Key> {

        private final int hash;


        private static final Key NULL_KEY = new Key();


        WeakProtectionDomainKey(ProtectionDomain pd, ReferenceQueue<Key> rq) {
            this((pd == null ? NULL_KEY : pd.key), rq);
        }

        WeakProtectionDomainKey(ProtectionDomain pd) {
            this(pd == null ? NULL_KEY : pd.key);
        }

        private WeakProtectionDomainKey(Key key, ReferenceQueue<Key> rq) {
            super(key, rq);
            hash = key.hashCode();
        }

        private WeakProtectionDomainKey(Key key) {
            super(key);
            hash = key.hashCode();
        }


        @Override
        public int hashCode() {
            return hash;
        }


        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof WeakProtectionDomainKey) {
                Object referent = get();
                return (referent != null) &&
                       (referent == ((WeakProtectionDomainKey)obj).get());
            } else {
                return false;
            }
        }
    }
}
