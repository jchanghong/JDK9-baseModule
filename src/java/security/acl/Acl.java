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

package java.security.acl;

import java.util.Enumeration;
import java.security.Principal;



@Deprecated(since="9")
public interface Acl extends Owner {


    public void setName(Principal caller, String name)
      throws NotOwnerException;


    public String getName();


    public boolean addEntry(Principal caller, AclEntry entry)
      throws NotOwnerException;


    public boolean removeEntry(Principal caller, AclEntry entry)
          throws NotOwnerException;


    public Enumeration<Permission> getPermissions(Principal user);


    public Enumeration<AclEntry> entries();


    public boolean checkPermission(Principal principal, Permission permission);


    public String toString();
}
