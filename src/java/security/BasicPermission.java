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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;



public abstract class BasicPermission extends Permission
    implements java.io.Serializable
{

    private static final long serialVersionUID = 6279438298436773498L;

    // does this permission have a wildcard at the end?
    private transient boolean wildcard;

    // the name without the wildcard on the end
    private transient String path;

    // is this permission the old-style exitVM permission (pre JDK 1.6)?
    private transient boolean exitVM;


    private void init(String name) {
        if (name == null)
            throw new NullPointerException("name can't be null");

        int len = name.length();

        if (len == 0) {
            throw new IllegalArgumentException("name can't be empty");
        }

        char last = name.charAt(len - 1);

        // Is wildcard or ends with ".*"?
        if (last == '*' && (len == 1 || name.charAt(len - 2) == '.')) {
            wildcard = true;
            if (len == 1) {
                path = "";
            } else {
                path = name.substring(0, len - 1);
            }
        } else {
            if (name.equals("exitVM")) {
                wildcard = true;
                path = "exitVM.";
                exitVM = true;
            } else {
                path = name;
            }
        }
    }


    public BasicPermission(String name) {
        super(name);
        init(name);
    }



    public BasicPermission(String name, String actions) {
        super(name);
        init(name);
    }


    @Override
    public boolean implies(Permission p) {
        if ((p == null) || (p.getClass() != getClass()))
            return false;

        BasicPermission that = (BasicPermission) p;

        if (this.wildcard) {
            if (that.wildcard) {
                // one wildcard can imply another
                return that.path.startsWith(path);
            } else {
                // make sure ap.path is longer so a.b.* doesn't imply a.b
                return (that.path.length() > this.path.length()) &&
                    that.path.startsWith(this.path);
            }
        } else {
            if (that.wildcard) {
                // a non-wildcard can't imply a wildcard
                return false;
            }
            else {
                return this.path.equals(that.path);
            }
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if ((obj == null) || (obj.getClass() != getClass()))
            return false;

        BasicPermission bp = (BasicPermission) obj;

        return getName().equals(bp.getName());
    }



    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }


    @Override
    public String getActions() {
        return "";
    }


    @Override
    public PermissionCollection newPermissionCollection() {
        return new BasicPermissionCollection(this.getClass());
    }


    private void readObject(ObjectInputStream s)
         throws IOException, ClassNotFoundException
    {
        s.defaultReadObject();
        // init is called to initialize the rest of the values.
        init(getName());
    }


    final String getCanonicalName() {
        return exitVM ? "exitVM.*" : getName();
    }
}



final class BasicPermissionCollection
    extends PermissionCollection
    implements java.io.Serializable
{

    private static final long serialVersionUID = 739301742472979399L;


    private transient ConcurrentHashMap<String, Permission> perms;


    private boolean all_allowed;


    private Class<?> permClass;


    public BasicPermissionCollection(Class<?> clazz) {
        perms = new ConcurrentHashMap<>(11);
        all_allowed = false;
        permClass = clazz;
    }


    @Override
    public void add(Permission permission) {
        if (! (permission instanceof BasicPermission))
            throw new IllegalArgumentException("invalid permission: "+
                                               permission);
        if (isReadOnly())
            throw new SecurityException("attempt to add a Permission to a readonly PermissionCollection");

        BasicPermission bp = (BasicPermission) permission;

        // make sure we only add new BasicPermissions of the same class
        // Also check null for compatibility with deserialized form from
        // previous versions.
        if (permClass == null) {
            // adding first permission
            permClass = bp.getClass();
        } else {
            if (bp.getClass() != permClass)
                throw new IllegalArgumentException("invalid permission: " +
                                                permission);
        }

        String canonName = bp.getCanonicalName();
        perms.put(canonName, permission);

        // No sync on all_allowed; staleness OK
        if (!all_allowed) {
            if (canonName.equals("*"))
                all_allowed = true;
        }
    }


    @Override
    public boolean implies(Permission permission) {
        if (! (permission instanceof BasicPermission))
            return false;

        BasicPermission bp = (BasicPermission) permission;

        // random subclasses of BasicPermission do not imply each other
        if (bp.getClass() != permClass)
            return false;

        // short circuit if the "*" Permission was added
        if (all_allowed)
            return true;

        // strategy:
        // Check for full match first. Then work our way up the
        // path looking for matches on a.b..*

        String path = bp.getCanonicalName();
        //System.out.println("check "+path);

        Permission x = perms.get(path);

        if (x != null) {
            // we have a direct hit!
            return x.implies(permission);
        }

        // work our way up the tree...
        int last, offset;

        offset = path.length()-1;

        while ((last = path.lastIndexOf('.', offset)) != -1) {

            path = path.substring(0, last+1) + "*";
            //System.out.println("check "+path);

            x = perms.get(path);

            if (x != null) {
                return x.implies(permission);
            }
            offset = last -1;
        }

        // we don't have to check for "*" as it was already checked
        // at the top (all_allowed), so we just return false
        return false;
    }


    @Override
    public Enumeration<Permission> elements() {
        return perms.elements();
    }

    // Need to maintain serialization interoperability with earlier releases,
    // which had the serializable field:
    //
    // @serial the Hashtable is indexed by the BasicPermission name
    //
    // private Hashtable permissions;

    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("permissions", Hashtable.class),
        new ObjectStreamField("all_allowed", Boolean.TYPE),
        new ObjectStreamField("permClass", Class.class),
    };


    /*
     * Writes the contents of the perms field out as a Hashtable for
     * serialization compatibility with earlier releases. all_allowed
     * and permClass unchanged.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Don't call out.defaultWriteObject()

        // Copy perms into a Hashtable
        Hashtable<String, Permission> permissions =
                new Hashtable<>(perms.size()*2);

        permissions.putAll(perms);

        // Write out serializable fields
        ObjectOutputStream.PutField pfields = out.putFields();
        pfields.put("all_allowed", all_allowed);
        pfields.put("permissions", permissions);
        pfields.put("permClass", permClass);
        out.writeFields();
    }


    private void readObject(java.io.ObjectInputStream in)
         throws IOException, ClassNotFoundException
    {
        // Don't call defaultReadObject()

        // Read in serialized fields
        ObjectInputStream.GetField gfields = in.readFields();

        // Get permissions
        // writeObject writes a Hashtable<String, Permission> for the
        // permissions key, so this cast is safe, unless the data is corrupt.
        @SuppressWarnings("unchecked")
        Hashtable<String, Permission> permissions =
                (Hashtable<String, Permission>)gfields.get("permissions", null);
        perms = new ConcurrentHashMap<>(permissions.size()*2);
        perms.putAll(permissions);

        // Get all_allowed
        all_allowed = gfields.get("all_allowed", false);

        // Get permClass
        permClass = (Class<?>) gfields.get("permClass", null);

        if (permClass == null) {
            // set permClass
            Enumeration<Permission> e = permissions.elements();
            if (e.hasMoreElements()) {
                Permission p = e.nextElement();
                permClass = p.getClass();
            }
        }
    }
}
