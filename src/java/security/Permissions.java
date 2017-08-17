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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.io.Serializable;
import java.io.ObjectStreamField;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;




public final class Permissions extends PermissionCollection
implements Serializable
{

    private transient ConcurrentHashMap<Class<?>, PermissionCollection> permsMap;

    // optimization. keep track of whether unresolved permissions need to be
    // checked
    private transient boolean hasUnresolved = false;

    // optimization. keep track of the AllPermission collection
    // - package private for ProtectionDomain optimization
    PermissionCollection allPermission;


    public Permissions() {
        permsMap = new ConcurrentHashMap<>(11);
        allPermission = null;
    }


    @Override
    public void add(Permission permission) {
        if (isReadOnly())
            throw new SecurityException(
              "attempt to add a Permission to a readonly Permissions object");

        PermissionCollection pc = getPermissionCollection(permission, true);
        pc.add(permission);

        // No sync; staleness -> optimizations delayed, which is OK
        if (permission instanceof AllPermission) {
            allPermission = pc;
        }
        if (permission instanceof UnresolvedPermission) {
            hasUnresolved = true;
        }
    }


    @Override
    public boolean implies(Permission permission) {
        // No sync; staleness -> skip optimization, which is OK
        if (allPermission != null) {
            return true; // AllPermission has already been added
        } else {
            PermissionCollection pc = getPermissionCollection(permission,
                false);
            if (pc != null) {
                return pc.implies(permission);
            } else {
                // none found
                return false;
            }
        }
    }


    @Override
    public Enumeration<Permission> elements() {
        // go through each Permissions in the hash table
        // and call their elements() function.

        return new PermissionsEnumerator(permsMap.values().iterator());
    }


    private PermissionCollection getPermissionCollection(Permission p,
                                                         boolean createEmpty) {
        Class<?> c = p.getClass();

        if (!hasUnresolved && !createEmpty) {
            return permsMap.get(c);
        }

        // Create and add permission collection to map if it is absent.
        // NOTE: cannot use lambda for mappingFunction parameter until
        // JDK-8076596 is fixed.
        return permsMap.computeIfAbsent(c,
            new java.util.function.Function<>() {
                @Override
                public PermissionCollection apply(Class<?> k) {
                    // Check for unresolved permissions
                    PermissionCollection pc =
                        (hasUnresolved ? getUnresolvedPermissions(p) : null);

                    // if still null, create a new collection
                    if (pc == null && createEmpty) {

                        pc = p.newPermissionCollection();

                        // still no PermissionCollection?
                        // We'll give them a PermissionsHash.
                        if (pc == null) {
                            pc = new PermissionsHash();
                        }
                    }
                    return pc;
                }
            }
        );
    }


    private PermissionCollection getUnresolvedPermissions(Permission p)
    {
        UnresolvedPermissionCollection uc =
        (UnresolvedPermissionCollection) permsMap.get(UnresolvedPermission.class);

        // we have no unresolved permissions if uc is null
        if (uc == null)
            return null;

        List<UnresolvedPermission> unresolvedPerms =
                                        uc.getUnresolvedPermissions(p);

        // we have no unresolved permissions of this type if unresolvedPerms is null
        if (unresolvedPerms == null)
            return null;

        java.security.cert.Certificate[] certs = null;

        Object[] signers = p.getClass().getSigners();

        int n = 0;
        if (signers != null) {
            for (int j=0; j < signers.length; j++) {
                if (signers[j] instanceof java.security.cert.Certificate) {
                    n++;
                }
            }
            certs = new java.security.cert.Certificate[n];
            n = 0;
            for (int j=0; j < signers.length; j++) {
                if (signers[j] instanceof java.security.cert.Certificate) {
                    certs[n++] = (java.security.cert.Certificate)signers[j];
                }
            }
        }

        PermissionCollection pc = null;
        synchronized (unresolvedPerms) {
            int len = unresolvedPerms.size();
            for (int i = 0; i < len; i++) {
                UnresolvedPermission up = unresolvedPerms.get(i);
                Permission perm = up.resolve(p, certs);
                if (perm != null) {
                    if (pc == null) {
                        pc = p.newPermissionCollection();
                        if (pc == null)
                            pc = new PermissionsHash();
                    }
                    pc.add(perm);
                }
            }
        }
        return pc;
    }

    private static final long serialVersionUID = 4858622370623524688L;

    // Need to maintain serialization interoperability with earlier releases,
    // which had the serializable field:
    // private Hashtable perms;


    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("perms", Hashtable.class),
        new ObjectStreamField("allPermission", PermissionCollection.class),
    };


    /*
     * Writes the contents of the permsMap field out as a Hashtable for
     * serialization compatibility with earlier releases. allPermission
     * unchanged.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Don't call out.defaultWriteObject()

        // Copy perms into a Hashtable
        Hashtable<Class<?>, PermissionCollection> perms =
            new Hashtable<>(permsMap.size()*2); // no sync; estimate
        perms.putAll(permsMap);

        // Write out serializable fields
        ObjectOutputStream.PutField pfields = out.putFields();

        pfields.put("allPermission", allPermission); // no sync; staleness OK
        pfields.put("perms", perms);
        out.writeFields();
    }

    /*
     * Reads in a Hashtable of Class/PermissionCollections and saves them in the
     * permsMap field. Reads in allPermission.
     */
    private void readObject(ObjectInputStream in) throws IOException,
    ClassNotFoundException {
        // Don't call defaultReadObject()

        // Read in serialized fields
        ObjectInputStream.GetField gfields = in.readFields();

        // Get allPermission
        allPermission = (PermissionCollection) gfields.get("allPermission", null);

        // Get permissions
        // writeObject writes a Hashtable<Class<?>, PermissionCollection> for
        // the perms key, so this cast is safe, unless the data is corrupt.
        @SuppressWarnings("unchecked")
        Hashtable<Class<?>, PermissionCollection> perms =
            (Hashtable<Class<?>, PermissionCollection>)gfields.get("perms", null);
        permsMap = new ConcurrentHashMap<>(perms.size()*2);
        permsMap.putAll(perms);

        // Set hasUnresolved
        UnresolvedPermissionCollection uc =
        (UnresolvedPermissionCollection) permsMap.get(UnresolvedPermission.class);
        hasUnresolved = (uc != null && uc.elements().hasMoreElements());
    }
}

final class PermissionsEnumerator implements Enumeration<Permission> {

    // all the perms
    private Iterator<PermissionCollection> perms;
    // the current set
    private Enumeration<Permission> permset;

    PermissionsEnumerator(Iterator<PermissionCollection> e) {
        perms = e;
        permset = getNextEnumWithMore();
    }

    // No need to synchronize; caller should sync on object as required
    public boolean hasMoreElements() {
        // if we enter with permissionimpl null, we know
        // there are no more left.

        if (permset == null)
            return  false;

        // try to see if there are any left in the current one

        if (permset.hasMoreElements())
            return true;

        // get the next one that has something in it...
        permset = getNextEnumWithMore();

        // if it is null, we are done!
        return (permset != null);
    }

    // No need to synchronize; caller should sync on object as required
    public Permission nextElement() {

        // hasMoreElements will update permset to the next permset
        // with something in it...

        if (hasMoreElements()) {
            return permset.nextElement();
        } else {
            throw new NoSuchElementException("PermissionsEnumerator");
        }

    }

    private Enumeration<Permission> getNextEnumWithMore() {
        while (perms.hasNext()) {
            PermissionCollection pc = perms.next();
            Enumeration<Permission> next =pc.elements();
            if (next.hasMoreElements())
                return next;
        }
        return null;

    }
}



final class PermissionsHash extends PermissionCollection
implements Serializable
{

    private transient ConcurrentHashMap<Permission, Permission> permsMap;


    PermissionsHash() {
        permsMap = new ConcurrentHashMap<>(11);
    }


    @Override
    public void add(Permission permission) {
        permsMap.put(permission, permission);
    }


    @Override
    public boolean implies(Permission permission) {
        // attempt a fast lookup and implies. If that fails
        // then enumerate through all the permissions.
        Permission p = permsMap.get(permission);

        // If permission is found, then p.equals(permission)
        if (p == null) {
            for (Permission p_ : permsMap.values()) {
                if (p_.implies(permission))
                    return true;
            }
            return false;
        } else {
            return true;
        }
    }


    @Override
    public Enumeration<Permission> elements() {
        return permsMap.elements();
    }

    private static final long serialVersionUID = -8491988220802933440L;
    // Need to maintain serialization interoperability with earlier releases,
    // which had the serializable field:
    // private Hashtable perms;

    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("perms", Hashtable.class),
    };


    /*
     * Writes the contents of the permsMap field out as a Hashtable for
     * serialization compatibility with earlier releases.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Don't call out.defaultWriteObject()

        // Copy perms into a Hashtable
        Hashtable<Permission, Permission> perms =
                new Hashtable<>(permsMap.size()*2);
        perms.putAll(permsMap);

        // Write out serializable fields
        ObjectOutputStream.PutField pfields = out.putFields();
        pfields.put("perms", perms);
        out.writeFields();
    }

    /*
     * Reads in a Hashtable of Permission/Permission and saves them in the
     * permsMap field.
     */
    private void readObject(ObjectInputStream in) throws IOException,
    ClassNotFoundException {
        // Don't call defaultReadObject()

        // Read in serialized fields
        ObjectInputStream.GetField gfields = in.readFields();

        // Get permissions
        // writeObject writes a Hashtable<Class<?>, PermissionCollection> for
        // the perms key, so this cast is safe, unless the data is corrupt.
        @SuppressWarnings("unchecked")
        Hashtable<Permission, Permission> perms =
                (Hashtable<Permission, Permission>)gfields.get("perms", null);
        permsMap = new ConcurrentHashMap<>(perms.size()*2);
        permsMap.putAll(perms);
    }
}
