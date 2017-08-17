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

import java.util.Enumeration;
import java.util.WeakHashMap;
import java.util.Objects;
import sun.security.jca.GetInstance;
import sun.security.util.Debug;
import sun.security.util.SecurityConstants;




public abstract class Policy {


    public static final PermissionCollection UNSUPPORTED_EMPTY_COLLECTION =
                        new UnsupportedEmptyCollection();

    // Information about the system-wide policy.
    private static class PolicyInfo {
        // the system-wide policy
        final Policy policy;
        // a flag indicating if the system-wide policy has been initialized
        final boolean initialized;

        PolicyInfo(Policy policy, boolean initialized) {
            this.policy = policy;
            this.initialized = initialized;
        }
    }

    // PolicyInfo is volatile since we apply DCL during initialization.
    // For correctness, care must be taken to read the field only once and only
    // write to it after any other initialization action has taken place.
    private static volatile PolicyInfo policyInfo = new PolicyInfo(null, false);

    private static final Debug debug = Debug.getInstance("policy");

    // Default policy provider
    private static final String DEFAULT_POLICY =
        "sun.security.provider.PolicyFile";

    // Cache mapping ProtectionDomain.Key to PermissionCollection
    private WeakHashMap<ProtectionDomain.Key, PermissionCollection> pdMapping;


    static boolean isSet() {
        PolicyInfo pi = policyInfo;
        return pi.policy != null && pi.initialized == true;
    }

    private static void checkPermission(String type) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("createPolicy." + type));
        }
    }


    public static Policy getPolicy()
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(SecurityConstants.GET_POLICY_PERMISSION);
        return getPolicyNoCheck();
    }


    static Policy getPolicyNoCheck()
    {
        PolicyInfo pi = policyInfo;
        // Use double-check idiom to avoid locking if system-wide policy is
        // already initialized
        if (pi.initialized == false || pi.policy == null) {
            synchronized (Policy.class) {
                pi = policyInfo;
                if (pi.policy == null) {
                    return loadPolicyProvider();
                }
            }
        }
        return pi.policy;
    }


    private static Policy loadPolicyProvider() {
        String policyProvider =
            AccessController.doPrivileged(new PrivilegedAction<>() {
                @Override
                public String run() {
                    return Security.getProperty("policy.provider");
                }
            });

        /*
         * If policy.provider is not set or is set to the default provider,
         * simply instantiate it and return.
         */
        if (policyProvider == null || policyProvider.isEmpty() ||
            policyProvider.equals(DEFAULT_POLICY))
        {
            Policy polFile = new sun.security.provider.PolicyFile();
            policyInfo = new PolicyInfo(polFile, true);
            return polFile;
        }

        /*
         * Locate, load, and instantiate the policy.provider impl using
         * the system class loader. While doing so, install the bootstrap
         * provider to avoid potential recursion.
         */
        Policy polFile = new sun.security.provider.PolicyFile();
        policyInfo = new PolicyInfo(polFile, false);

        Policy pol = AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public Policy run() {
                try {
                    ClassLoader scl = ClassLoader.getSystemClassLoader();
                    @SuppressWarnings("deprecation")
                    Object o = Class.forName(policyProvider, true, scl).newInstance();
                    return (Policy)o;
                } catch (Exception e) {
                    if (debug != null) {
                        debug.println("policy provider " + policyProvider +
                                      " not available");
                        e.printStackTrace();
                    }
                    return null;
                }
            }
        });

        if (pol == null) {
            // Fallback and use the system default implementation
            if (debug != null) {
                debug.println("using " + DEFAULT_POLICY);
            }
            pol = polFile;
        }
        policyInfo = new PolicyInfo(pol, true);
        return pol;
    }


    public static void setPolicy(Policy p)
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(
                                 new SecurityPermission("setPolicy"));
        if (p != null) {
            initPolicy(p);
        }
        synchronized (Policy.class) {
            policyInfo = new PolicyInfo(p, p != null);
        }
    }


    private static void initPolicy (final Policy p) {
        /*
         * A policy provider not on the bootclasspath could trigger
         * security checks fulfilling a call to either Policy.implies
         * or Policy.getPermissions. If this does occur the provider
         * must be able to answer for it's own ProtectionDomain
         * without triggering additional security checks, otherwise
         * the policy implementation will end up in an infinite
         * recursion.
         *
         * To mitigate this, the provider can collect it's own
         * ProtectionDomain and associate a PermissionCollection while
         * it is being installed. The currently installed policy
         * provider (if there is one) will handle calls to
         * Policy.implies or Policy.getPermissions during this
         * process.
         *
         * This Policy superclass caches away the ProtectionDomain and
         * statically binds permissions so that legacy Policy
         * implementations will continue to function.
         */

        ProtectionDomain policyDomain =
        AccessController.doPrivileged(new PrivilegedAction<>() {
            public ProtectionDomain run() {
                return p.getClass().getProtectionDomain();
            }
        });

        /*
         * Collect the permissions granted to this protection domain
         * so that the provider can be security checked while processing
         * calls to Policy.implies or Policy.getPermissions.
         */
        PermissionCollection policyPerms = null;
        synchronized (p) {
            if (p.pdMapping == null) {
                p.pdMapping = new WeakHashMap<>();
           }
        }

        if (policyDomain.getCodeSource() != null) {
            Policy pol = policyInfo.policy;
            if (pol != null) {
                policyPerms = pol.getPermissions(policyDomain);
            }

            if (policyPerms == null) { // assume it has all
                policyPerms = new Permissions();
                policyPerms.add(SecurityConstants.ALL_PERMISSION);
            }

            synchronized (p.pdMapping) {
                // cache of pd to permissions
                p.pdMapping.put(policyDomain.key, policyPerms);
            }
        }
        return;
    }



    public static Policy getInstance(String type, Policy.Parameters params)
                throws NoSuchAlgorithmException {
        Objects.requireNonNull(type, "null type name");
        checkPermission(type);
        try {
            GetInstance.Instance instance = GetInstance.getInstance("Policy",
                                                        PolicySpi.class,
                                                        type,
                                                        params);
            return new PolicyDelegate((PolicySpi)instance.impl,
                                                        instance.provider,
                                                        type,
                                                        params);
        } catch (NoSuchAlgorithmException nsae) {
            return handleException(nsae);
        }
    }


    public static Policy getInstance(String type,
                                Policy.Parameters params,
                                String provider)
                throws NoSuchProviderException, NoSuchAlgorithmException {

        Objects.requireNonNull(type, "null type name");
        if (provider == null || provider.length() == 0) {
            throw new IllegalArgumentException("missing provider");
        }

        checkPermission(type);
        try {
            GetInstance.Instance instance = GetInstance.getInstance("Policy",
                                                        PolicySpi.class,
                                                        type,
                                                        params,
                                                        provider);
            return new PolicyDelegate((PolicySpi)instance.impl,
                                                        instance.provider,
                                                        type,
                                                        params);
        } catch (NoSuchAlgorithmException nsae) {
            return handleException(nsae);
        }
    }


    public static Policy getInstance(String type,
                                Policy.Parameters params,
                                Provider provider)
                throws NoSuchAlgorithmException {

        Objects.requireNonNull(type, "null type name");
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }

        checkPermission(type);
        try {
            GetInstance.Instance instance = GetInstance.getInstance("Policy",
                                                        PolicySpi.class,
                                                        type,
                                                        params,
                                                        provider);
            return new PolicyDelegate((PolicySpi)instance.impl,
                                                        instance.provider,
                                                        type,
                                                        params);
        } catch (NoSuchAlgorithmException nsae) {
            return handleException(nsae);
        }
    }

    private static Policy handleException(NoSuchAlgorithmException nsae)
                throws NoSuchAlgorithmException {
        Throwable cause = nsae.getCause();
        if (cause instanceof IllegalArgumentException) {
            throw (IllegalArgumentException)cause;
        }
        throw nsae;
    }


    public Provider getProvider() {
        return null;
    }


    public String getType() {
        return null;
    }


    public Policy.Parameters getParameters() {
        return null;
    }


    public PermissionCollection getPermissions(CodeSource codesource) {
        return Policy.UNSUPPORTED_EMPTY_COLLECTION;
    }


    public PermissionCollection getPermissions(ProtectionDomain domain) {
        PermissionCollection pc = null;

        if (domain == null)
            return new Permissions();

        if (pdMapping == null) {
            initPolicy(this);
        }

        synchronized (pdMapping) {
            pc = pdMapping.get(domain.key);
        }

        if (pc != null) {
            Permissions perms = new Permissions();
            synchronized (pc) {
                for (Enumeration<Permission> e = pc.elements() ; e.hasMoreElements() ;) {
                    perms.add(e.nextElement());
                }
            }
            return perms;
        }

        pc = getPermissions(domain.getCodeSource());
        if (pc == null || pc == UNSUPPORTED_EMPTY_COLLECTION) {
            pc = new Permissions();
        }

        addStaticPerms(pc, domain.getPermissions());
        return pc;
    }


    private void addStaticPerms(PermissionCollection perms,
                                PermissionCollection statics) {
        if (statics != null) {
            synchronized (statics) {
                Enumeration<Permission> e = statics.elements();
                while (e.hasMoreElements()) {
                    perms.add(e.nextElement());
                }
            }
        }
    }


    public boolean implies(ProtectionDomain domain, Permission permission) {
        PermissionCollection pc;

        if (pdMapping == null) {
            initPolicy(this);
        }

        synchronized (pdMapping) {
            pc = pdMapping.get(domain.key);
        }

        if (pc != null) {
            return pc.implies(permission);
        }

        pc = getPermissions(domain);
        if (pc == null) {
            return false;
        }

        synchronized (pdMapping) {
            // cache it
            pdMapping.put(domain.key, pc);
        }

        return pc.implies(permission);
    }


    public void refresh() { }


    private static class PolicyDelegate extends Policy {

        private PolicySpi spi;
        private Provider p;
        private String type;
        private Policy.Parameters params;

        private PolicyDelegate(PolicySpi spi, Provider p,
                        String type, Policy.Parameters params) {
            this.spi = spi;
            this.p = p;
            this.type = type;
            this.params = params;
        }

        @Override public String getType() { return type; }

        @Override public Policy.Parameters getParameters() { return params; }

        @Override public Provider getProvider() { return p; }

        @Override
        public PermissionCollection getPermissions(CodeSource codesource) {
            return spi.engineGetPermissions(codesource);
        }
        @Override
        public PermissionCollection getPermissions(ProtectionDomain domain) {
            return spi.engineGetPermissions(domain);
        }
        @Override
        public boolean implies(ProtectionDomain domain, Permission perm) {
            return spi.engineImplies(domain, perm);
        }
        @Override
        public void refresh() {
            spi.engineRefresh();
        }
    }


    public static interface Parameters { }


    private static class UnsupportedEmptyCollection
        extends PermissionCollection {

        private static final long serialVersionUID = -8492269157353014774L;

        private Permissions perms;


        public UnsupportedEmptyCollection() {
            this.perms = new Permissions();
            perms.setReadOnly();
        }


        @Override public void add(Permission permission) {
            perms.add(permission);
        }


        @Override public boolean implies(Permission permission) {
            return perms.implies(permission);
        }


        @Override public Enumeration<Permission> elements() {
            return perms.elements();
        }
    }
}
