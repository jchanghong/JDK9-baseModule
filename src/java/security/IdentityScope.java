/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Properties;


@Deprecated(since="1.2")
public abstract
class IdentityScope extends Identity {

    private static final long serialVersionUID = -2337346281189773310L;

    /* The system's scope */
    private static IdentityScope scope;

    // initialize the system scope
    private static void initializeSystemScope() {

        String classname = AccessController.doPrivileged(
                                new PrivilegedAction<>() {
            public String run() {
                return Security.getProperty("system.scope");
            }
        });

        if (classname == null) {
            return;

        } else {

            try {
                Class.forName(classname);
            } catch (ClassNotFoundException e) {
                //Security.error("unable to establish a system scope from " +
                //             classname);
                e.printStackTrace();
            }
        }
    }


    protected IdentityScope() {
        this("restoring...");
    }


    public IdentityScope(String name) {
        super(name);
    }


    public IdentityScope(String name, IdentityScope scope)
    throws KeyManagementException {
        super(name, scope);
    }


    public static IdentityScope getSystemScope() {
        if (scope == null) {
            initializeSystemScope();
        }
        return scope;
    }



    protected static void setSystemScope(IdentityScope scope) {
        check("setSystemScope");
        IdentityScope.scope = scope;
    }


    public abstract int size();


    public abstract Identity getIdentity(String name);


    public Identity getIdentity(Principal principal) {
        return getIdentity(principal.getName());
    }


    public abstract Identity getIdentity(PublicKey key);


    public abstract void addIdentity(Identity identity)
    throws KeyManagementException;


    public abstract void removeIdentity(Identity identity)
    throws KeyManagementException;


    public abstract Enumeration<Identity> identities();


    public String toString() {
        return super.toString() + "[" + size() + "]";
    }

    private static void check(String directive) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkSecurityAccess(directive);
        }
    }

}
